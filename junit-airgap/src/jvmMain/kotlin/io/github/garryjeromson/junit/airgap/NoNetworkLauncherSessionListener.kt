package io.github.garryjeromson.junit.airgap

import io.github.garryjeromson.junit.airgap.bytebuddy.NetworkBlockerContext
import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener

/**
 * JUnit Platform LauncherSessionListener that initializes NetworkBlockerContext early.
 *
 * This listener is discovered via ServiceLoader and runs at the very start of the test session,
 * before any test framework infrastructure is set up. This ensures that NetworkBlockerContext
 * registers with the JVMTI agent (if loaded) before any network connections are made.
 *
 * ## Why This is Needed
 *
 * The JVMTI agent intercepts network calls at the native level, but it needs NetworkBlockerContext
 * to be initialized to check test configuration. Without this listener:
 *
 * 1. JVM starts, JVMTI agent loads
 * 2. Test framework makes connections (localhost test server)
 * 3. JVMTI agent intercepts but NetworkBlockerContext not registered yet → allows connection
 * 4. THEN tests run and NetworkBlockerContext initializes
 *
 * With this listener:
 *
 * 1. JVM starts, JVMTI agent loads
 * 2. LauncherSessionListener runs → NetworkBlockerContext initializes → registers with agent
 * 3. Test framework makes connections → JVMTI agent has NetworkBlockerContext registered
 * 4. Tests run with network blocking active
 *
 * ## Service Loader Registration
 *
 * This class is registered in:
 * `resources/META-INF/services/org.junit.platform.launcher.LauncherSessionListener`
 *
 * JUnit Platform automatically discovers and invokes it.
 */
class AirgapLauncherSessionListener : LauncherSessionListener {
    private val logger = DebugLogger.instance

    override fun launcherSessionOpened(session: LauncherSession) {
        logger.debug { "LauncherSessionListener.launcherSessionOpened() called" }

        // Check if the plugin is disabled via system property
        val enabled = System.getProperty(ENABLED_PROPERTY, "true").toBoolean()
        if (!enabled) {
            logger.debug { "JUnit Airgap is disabled (${ENABLED_PROPERTY}=false), skipping initialization" }
            return
        }

        // Force NetworkBlockerContext to initialize by accessing it.
        // This triggers its init block, which calls registerWithAgent() (if JVMTI agent is loaded).
        // We call getConfiguration() because it's a simple getter with no side effects.
        try {
            NetworkBlockerContext.getConfiguration()
            logger.debug { "NetworkBlockerContext initialized successfully" }
        } catch (e: Throwable) {
            // Ignore errors - NetworkBlockerContext will initialize when first used
            // This is expected if the JVMTI agent is not loaded
            logger.debug { "NetworkBlockerContext initialization failed: ${e.message}" }
        }
    }
}
