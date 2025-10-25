package io.github.garryjeromson.junit.nonetwork.test

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Robolectric tests demonstrating Android API usage with network blocking.
 * Tests the bytecode enhancement path for @Rule injection with Android framework APIs.
 */
@RunWith(RobolectricTestRunner::class)
class RobolectricNetworkTest {
    @Test
    @NoNetworkTest
    fun canUseAndroidContextWithNetworkBlocking() {
        // Verify Robolectric provides Android framework
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context, "Android context should be available")
        assertNotNull(context.packageManager, "PackageManager should be available")
        assertNotNull(context.packageName, "Package name should be available")
    }

    @Test
    @NoNetworkTest
    fun canUseSharedPreferencesWithNetworkBlocking() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs: SharedPreferences = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)

        // Write data
        prefs.edit().putString("test_key", "test_value").apply()

        // Read data back
        val value = prefs.getString("test_key", null)
        assertEquals("test_value", value, "SharedPreferences should work with network blocking")
    }

    @Test
    @AllowNetwork
    fun canUseAndroidApisWithAllowNetworkAnnotation() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Test various Android APIs work with @AllowNetwork
        assertNotNull(context.resources, "Resources should be available")
        assertNotNull(context.contentResolver, "ContentResolver should be available")
        assertNotNull(context.applicationInfo, "ApplicationInfo should be available")
    }

    @Test
    @NoNetworkTest
    fun `test names with spaces should work with bytecode injection`() {
        // Verify that Kotlin backtick syntax (spaces in method names) works with ByteBuddy injection
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context, "Context should be available even with spaces in test name")
    }
}
