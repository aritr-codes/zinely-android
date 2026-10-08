package com.aritr.zinely.feature.library

import java.util.Locale

/**
 * A stand-in for the app's folder-name rules, for tests of this module, which cannot see `:core:data`:
 * trimmed, compared ignoring case, forty characters. It is the frozen page's own rule (`fold`). The real
 * rules are `FolderNames`', passed in by the app as `folderNameVerdict` and tested there.
 */
internal fun plainFolderNameVerdict(typed: String): FolderNameVerdict {
    val whole = typed.trim()
    // The key is of the name that is kept, so a long name and its own first forty characters are one folder.
    val name = whole.take(40).trim()
    val key = name.lowercase(Locale.ROOT)
    return when {
        name.isEmpty() -> FolderNameVerdict.Blank
        key == "my shelf" -> FolderNameVerdict.MyShelf
        else -> FolderNameVerdict.Name(name, key, cut = whole.length > 40)
    }
}
