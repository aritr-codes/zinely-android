package com.aritr.zinely.editor

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * **Share-in — the images another app handed us** (SUPPLIES-SPEC §6 / §10 S1, re-sequenced to the front of
 * the queue by [ADR-105](../../../../../../docs/DECISIONS.md#adr-105)).
 *
 * This is the whole of the share-in *transport*. It holds nothing but `content://` URIs the sender granted
 * us read access to, for as long as it takes the editor to pick them up. Deliberately **not** a second
 * import pipeline: everything downstream of "here is a Uri" — decode, EXIF normalisation, the
 * content-addressed [AssetStore][com.aritr.zinely.core.data.asset.AssetStore] write, placement, the
 * reducer's `CommitAddImage`, undo — is the shipped ADR-031 §5 path, reached through
 * [AndroidImagePickDecodePipeline.decodeAndStore]. The only thing share-in replaces is the *pick*.
 *
 * ### Where a shared photo lands, and why
 *
 * **In the zine the maker opens next — or, if a zine is already open, in that one.** The alternatives were
 * rejected on the product principle that every screen answers the question the user is holding:
 *
 *  - *A new zine per share* silently multiplies the shelf and answers "which zine?" on the user's behalf.
 *  - *The most-recent zine* guesses, and a wrong guess writes into work the maker did not name.
 *  - *A zine chooser* is a new designed surface, and the [HTML-first workflow](../../../../../../CLAUDE.md#html-first-ui-workflow-mandatory)
 *    forbids inventing one in Compose. It is also unnecessary: the Library **is** the chooser, it is the
 *    start destination, and its whole job is already "which zine do I want?".
 *
 * So share-in adds no navigation and no screen. The received URIs wait here; whichever editor is (or
 * becomes) alive drains them. See **D-081** in
 * [V2-SPEC-DEFECTS.md](../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-081) for the questions this
 * behaviour defers to the owner (multi-image placement, a designed pending-photos affordance).
 *
 * ### Two invariants worth stating where they live
 *
 *  - **Nothing leaves the device.** A received Uri is read, decoded and written to app-private storage.
 *    Zinely never re-shares, uploads or resolves it outward, and this class holds no other data.
 *  - **A Uri outlives neither the task nor the grant.** The sender's `FLAG_GRANT_READ_URI_PERMISSION` is
 *    scoped to this task, so the inbox is deliberately in-memory only: persisting URIs across process
 *    death would persist handles that no longer open. An import attempted after the grant is gone fails
 *    the same way any unreadable source does — [ImportMasterDecoder] returns `null` and the maker is told.
 */
@Singleton
public class ShareInbox @Inject constructor() {

    private val _pending = MutableStateFlow<List<Uri>>(emptyList())

    /** URIs waiting for an editor. Emits `emptyList()` again the moment [takeAll] drains it. */
    public val pending: StateFlow<List<Uri>> = _pending.asStateFlow()

    /**
     * `true` when an editor is currently collecting [pending] — i.e. a zine is open and a share will land
     * in it immediately, visibly, with no further choice to make.
     *
     * This is the one fact that decides whether the receiving Activity owes the maker a sentence, and the
     * inbox is the only object that knows it. When it is `false` the app is showing the shelf (or is cold
     * starting into it) and the photos would otherwise arrive with no explanation at all.
     */
    public val hasOpenZine: Boolean get() = _pending.subscriptionCount.value > 0

    /** Queue [uris] for the next (or current) editor. Appends — a second share never drops the first. */
    public fun offer(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _pending.update { it + uris }
    }

    /** Take everything queued, leaving the inbox empty. Returns `emptyList()` when there is nothing. */
    public fun takeAll(): List<Uri> {
        var taken: List<Uri> = emptyList()
        _pending.update { current ->
            taken = current
            emptyList()
        }
        return taken
    }
}

/**
 * What this app accepts out of a received intent — **pure**, so the rule is unit-testable without an
 * `android.content.Intent`. Generic over the URI type for the same reason [PhotoPicker] is.
 *
 * Three things must hold, and each maps to a failure mode the manifest alone does not close:
 *
 *  1. **The action is a share.** `ACTION_SEND` / `ACTION_SEND_MULTIPLE` only — the launcher's `ACTION_MAIN`
 *     re-enters this code on every cold start and must yield nothing.
 *  2. **The declared type is an image, or the sender declined to say.** The manifest filter says images
 *     only, but a filter is a routing hint, not a guarantee: an explicit intent from another app reaches an
 *     exported Activity regardless, so `application/pdf` is refused here rather than handed to a decoder
 *     that would simply fail. `*&#47;*` is **accepted** — several gallery apps send multi-image shares under
 *     it, it matches the manifest filter, and refusing it would reject the legitimate share it usually is.
 *     A wildcard costs nothing: [ImportMasterDecoder] is the real arbiter and reports what it cannot read.
 *  3. **There is at least one URI.** `EXTRA_STREAM` is optional; a text-only `ACTION_SEND` carries none.
 *
 * A `null` [mimeType] is refused: an intent that names no type and matched no filter is not something we
 * were meaningfully offered.
 */
internal fun <T : Any> acceptedShareIn(action: String?, mimeType: String?, uris: List<T?>): List<T> {
    val isShare = action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE
    if (!isShare) return emptyList()
    if (mimeType == null) return emptyList()
    if (!mimeType.startsWith("image/") && mimeType != WILDCARD_MIME) return emptyList()
    return uris.filterNotNull()
}

/** The "I'm not telling you" MIME type. Accepted; see [acceptedShareIn] rule 2. */
private const val WILDCARD_MIME = "*/*"

/**
 * **Which URI schemes Zinely will read from a share** — the trust boundary, and a rule with teeth.
 *
 * Only `content://`. A share is an arbitrary `Uri` chosen by another app, and Zinely reads it with *its own*
 * uid: a `file:///data/user/0/com.aritr.zinely/...` handed back to us would let a sender copy Zinely's own
 * private files into a zine, from which the shipped export path ([ADR-039](../../../../../../docs/DECISIONS.md#adr-039))
 * can send them anywhere. `content://` is the only scheme that routes through a provider and therefore
 * through a grant the sender actually holds — which is the whole reason share-in needs no storage
 * permission. Nothing about this is theoretical enough to be left as a comment.
 */
internal fun isReadableShareScheme(scheme: String?): Boolean = scheme == "content"

/**
 * The thin Android seam over [acceptedShareIn]: read `EXTRA_STREAM` in both its shapes (a single
 * `Parcelable` for `ACTION_SEND`, a list for `ACTION_SEND_MULTIPLE`), keep only what is genuinely a
 * readable `content://` [Uri], and apply the pure rule.
 *
 * ⚠ **`filterIsInstance` is a guard, not tidiness.** `EXTRA_STREAM` is typed `Parcelable`; below API 33 the
 * only available accessors are the erased generic ones, so a sender that puts *any* other `Parcelable`
 * there produces a `List<Uri>` the compiler believes and the runtime does not. Without this filter the
 * `ClassCastException` surfaces later, inside the import coroutine, as a crash on a hostile or merely buggy
 * share. Validated here, at the boundary, where the untrusted value arrives.
 *
 * The deprecated `getParcelableExtra` overloads are used below API 33 because the typed replacements do
 * not exist there; the branch is the platform's, not a preference.
 */
@Suppress("DEPRECATION")
internal fun Intent.sharedImageUris(): List<Uri> {
    val uris: List<Parcelable?> = when (action) {
        Intent.ACTION_SEND_MULTIPLE ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableArrayListExtra(Intent.EXTRA_STREAM, Parcelable::class.java).orEmpty()
            } else {
                getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM).orEmpty()
            }
        else -> listOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(Intent.EXTRA_STREAM, Parcelable::class.java)
            } else {
                getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
            },
        )
    }
    val readable = uris.filterIsInstance<Uri>().filter { isReadableShareScheme(it.scheme) }
    return acceptedShareIn(action, type, readable)
}

/**
 * `true` when this intent is a share Zinely was offered but cannot use — a share action carrying a
 * non-image type. It is separated from "not a share at all" because the two owe the maker different
 * things: silence for a launcher start, an honest refusal for a PDF someone tried to put in a zine.
 */
internal fun Intent.isUnsupportedShare(): Boolean {
    val isShare = action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE
    return isShare && sharedImageUris().isEmpty()
}
