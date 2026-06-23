package com.aritr.zinely.data.android

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Instrumentation runner that swaps in [HiltTestApplication] so `@HiltAndroidTest` graph tests have a
 * Hilt-enabled Application (PR-A Step 7, design §13.3). The existing non-Hilt instrumented tests
 * (e.g. the real-`Os` durability checks) are unaffected — they don't touch the graph.
 */
public class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
