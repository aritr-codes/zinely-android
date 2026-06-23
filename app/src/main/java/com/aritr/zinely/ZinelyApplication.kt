package com.aritr.zinely

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt application root (PR-A Step 7). Triggers generation of the `SingletonComponent` that wires the
 * autosave stack contributed by `:data-android` (`di/`). No behavior beyond hosting the graph.
 */
@HiltAndroidApp
class ZinelyApplication : Application()
