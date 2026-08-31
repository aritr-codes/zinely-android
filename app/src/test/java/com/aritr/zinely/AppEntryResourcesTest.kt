package com.aritr.zinely

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Pins ADR-111's manifest/theme boundary; resource linking protects the icon layers themselves. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppEntryResourcesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun launcherActivityUsesStartingThemeWhileApplicationKeepsPostSplashTheme() {
        val component = ComponentName(context, MainActivity::class.java)
        val activityInfo = context.packageManager.getActivityInfo(component, 0)
        val applicationInfo = context.applicationInfo

        assertEquals(R.style.Theme_Zinely_Starting, activityInfo.themeResource)
        assertEquals(R.style.Theme_Zinely, applicationInfo.theme)
        assertNotEquals(activityInfo.themeResource, applicationInfo.theme)
    }
}
