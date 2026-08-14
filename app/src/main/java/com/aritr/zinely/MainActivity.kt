package com.aritr.zinely

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritr.zinely.editor.ZinelyNavHost
import com.aritr.zinely.ui.theme.ZinelyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity (ADR-030 §1). Hosts the navigation-compose graph; all screens are composable
 * destinations. `@AndroidEntryPoint` lets `hiltViewModel()` resolve the editor's `@HiltViewModel`
 * against the `:data-android` SingletonComponent graph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZinelyApp()
        }
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
