package com.aritr.zinely

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Debug-only host for the Robolectric host-level tests (ADR-046 §Testing): `hiltViewModel()` inside
 * [ZinelyNavHost][com.aritr.zinely.editor.ZinelyNavHost]'s destinations resolves its factory through
 * the hosting activity, which must therefore be a Hilt activity — the plain `ComponentActivity` the
 * compose-test manifest ships is not. Registered in the debug manifest overlay; never in a release
 * build. (The standard Hilt testing pattern; it holds no logic.)
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
