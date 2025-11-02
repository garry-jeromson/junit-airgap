package org.robolectric.internal.dependency

import io.github.garryjeromson.junit.airgap.NetworkConfiguration
import io.github.garryjeromson.junit.airgap.bytebuddy.NetworkBlockerContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for Robolectric MavenArtifactFetcher detection.
 *
 * This test is placed in the org.robolectric.internal.dependency package
 * to simulate the real Robolectric class hierarchy in the call stack.
 */
class MavenArtifactFetcherTest {
    @AfterEach
    fun cleanup() {
        NetworkBlockerContext.clearConfiguration()
    }

    @Test
    fun `checkConnection allows connections from MavenArtifactFetcher`() {
        // Set up configuration that blocks ALL hosts
        val configuration =
            NetworkConfiguration(
                allowedHosts = emptySet(),
                blockedHosts = emptySet(),
            )
        NetworkBlockerContext.setConfiguration(configuration)

        // Create a mock fetcher class (simulates Robolectric's real class)
        val fetcher = MockMavenArtifactFetcher()

        // This should succeed without throwing because we detect Robolectric in the stack
        fetcher.downloadArtifact()
    }

    @Test
    fun `isRobolectricArtifactDownload detects MavenArtifactFetcher in stack`() {
        // Call from a class with "MavenArtifactFetcher" in the name
        val result = MockMavenArtifactFetcher.checkFromStaticContext()

        // Should detect Robolectric in the stack
        assertTrue(result) {
            "Should detect MavenArtifactFetcher in call stack"
        }
    }

    /**
     * Mock class that simulates Robolectric's MavenArtifactFetcher.
     * The package and class name match Robolectric's actual structure.
     */
    private class MockMavenArtifactFetcher {
        fun downloadArtifact() {
            // Simulate Maven artifact download
            // This should NOT throw NetworkRequestAttemptedException
            NetworkBlockerContext.checkConnection("repo1.maven.org", 443, "test")
        }

        companion object {
            fun checkFromStaticContext(): Boolean = NetworkBlockerContext.isRobolectricArtifactDownload()
        }
    }
}
