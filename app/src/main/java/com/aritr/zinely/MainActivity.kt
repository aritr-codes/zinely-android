package com.aritr.zinely

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.editor.ShareInbox
import com.aritr.zinely.editor.ZinelyNavHost
import com.aritr.zinely.editor.isUnsupportedShare
import com.aritr.zinely.editor.sharedImageUris
import com.aritr.zinely.ui.theme.ZinelyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The single Activity (ADR-030 §1). Hosts the navigation-compose graph; all screens are composable
 * destinations. `@AndroidEntryPoint` lets `hiltViewModel()` resolve the editor's `@HiltViewModel`
 * against the `:data-android` SingletonComponent graph.
 *
 * It is also the app's **one** external entry point, and since ADR-105's re-sequencing that means the
 * share-in receiver: photos handed to Zinely by any other app arrive here as `ACTION_SEND` /
 * `ACTION_SEND_MULTIPLE` and are queued in the [ShareInbox] for whichever editor is (or becomes) alive.
 * This Activity performs no import itself — it only reads the intent and says what happened.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var shareInbox: ShareInbox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a genuine cold/warm start, never on a configuration change: `savedInstanceState != null`
        // means this is the same logical Activity being rebuilt, and its intent is the SAME intent whose
        // photos were already queued (and very likely already imported). Re-reading it would add the
        // maker's photo a second time on every rotation.
        if (savedInstanceState == null) receiveShare(intent)
        setContent {
            ZinelyApp()
        }
    }

    /**
     * A share delivered while Zinely is already running (`singleTask`, see the manifest — `singleTop` was
     * measurably not enough, because the share sheet launches with FLAG_ACTIVITY_NEW_TASK). `setIntent` keeps
     * `getIntent()` honest for anything that reads it later; the queued photos land in the open zine if
     * there is one, and wait for the next one if there is not.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveShare(intent)
    }

    /**
     * Queue whatever the sender gave us, then say what is about to happen to it.
     *
     * **Every accepted share is acknowledged here, not only the ones that land on the shelf.** An earlier
     * draft stayed silent when a zine was open, on the reasoning that the photos would visibly appear and a
     * toast over a visible result is noise. Review falsified it: the editor's spoken count can be dropped
     * (the announcement channel is replay-free, so an emission with no collector — the Proof screen is
     * showing over the live editor, or the editor is still booting — is discarded), which leaves the whole
     * share reporting *nothing*. Two harmless sentences beat one silent import.
     *
     * The three outcomes:
     *  - **photos + an open zine** — [Copy.ShareIn.ADDING_TO_OPEN_ZINE].
     *  - **photos + no open zine** — [Copy.ShareIn.CHOOSE_ZINE]; the shelf is about to appear and would
     *    otherwise say nothing about the photo the maker just sent.
     *  - **a share we cannot use** — [Copy.ShareIn.ONLY_PHOTOS]. An honest refusal beats a launch that
     *    looks like the share worked.
     */
    private fun receiveShare(intent: Intent) {
        val uris = intent.sharedImageUris()
        if (uris.isNotEmpty()) {
            // Read before offering: `offer` does not itself create a collector, but reading first keeps
            // the sentence keyed to the state the maker is actually in when the share arrives.
            val openZine = shareInbox.hasOpenZine
            shareInbox.offer(uris)
            toast(if (openZine) Copy.ShareIn.ADDING_TO_OPEN_ZINE else Copy.ShareIn.CHOOSE_ZINE)
        } else if (intent.isUnsupportedShare()) {
            toast(Copy.ShareIn.ONLY_PHOTOS)
        }
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
}

/**
 * ⚠ **`consumeWindowInsets` is load-bearing, and its absence was visible only on hardware.**
 *
 * `Modifier.padding(innerPadding)` pads but does not *consume*: it moves the content up off the
 * navigation bar while leaving every descendant still believing the navigation bar is unhandled. A
 * descendant that then asks for the IME inset — [BenchStyleRow]'s `imePadding()`, [ZSheet]'s
 * `navigationBars.union(ime)` — gets an inset measured from the **screen's** bottom edge, and applies it
 * inside a container whose bottom edge is already 48dp above it. The navigation bar is counted twice.
 *
 * On the device that shipped as a style bar floating a clear 48dp above the keyboard with the page
 * showing through the gap, which reads as a toolbar that failed to dock. Nothing could see it here: every
 * Robolectric host in the suite composes the screen directly, with no Scaffold above it and no IME at
 * all, so the double count has nothing to double. It was found by looking at a phone.
 *
 * `consumeWindowInsets(innerPadding)` tells the subtree what has already been paid, so `imePadding()`
 * pads by the remainder and the bar lands on the keyboard.
 */
@Composable
private fun ZinelyApp() {
    ZinelyTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            ZinelyNavHost(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            )
        }
    }
}
