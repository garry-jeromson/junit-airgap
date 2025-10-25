package io.github.garryjeromson.junit.nonetwork.test

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Robolectric tests for KMP Android target demonstrating Android API usage.
 * These tests run only in the Android unit test source set of the KMP project.
 */
@RunWith(RobolectricTestRunner::class)
class RobolectricKmpTest {
    @Test
    @BlockNetworkRequests
    fun `KMP Android target can use Robolectric with network blocking`() {
        // Verify Robolectric provides Android framework in KMP context
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context, "Android context should be available in KMP Android target")
        assertNotNull(context.packageManager, "PackageManager should be available")
    }

    @Test
    @BlockNetworkRequests
    fun kmpAndroidTargetCanUseSharedPreferencesWithNetworkBlocking() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs: SharedPreferences = context.getSharedPreferences("kmp_test_prefs", Context.MODE_PRIVATE)

        // Write data
        prefs.edit().putString("kmp_key", "kmp_value").apply()

        // Read data back
        val value = prefs.getString("kmp_key", null)
        assertEquals("kmp_value", value, "SharedPreferences should work in KMP Android target")
    }

    @Test
    @AllowNetworkRequests
    fun kmpAndroidTargetCanUseAndroidApisWithAllowNetwork() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Test various Android APIs work with @AllowNetworkRequests in KMP
        assertNotNull(context.resources, "Resources should be available in KMP Android target")
        assertNotNull(context.contentResolver, "ContentResolver should be available in KMP Android target")
        assertNotNull(context.applicationInfo, "ApplicationInfo should be available in KMP Android target")
    }
}
