package io.github.garryjeromson.junit.airgap.gradle

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.Test

/**
 * Detects JUnit version and determines whether JUnit 4 @Rule injection should be enabled.
 *
 * Uses a hybrid detection strategy:
 * 1. Checks if test tasks use useJUnitPlatform() (indicates JUnit 5)
 * 2. Checks for junit:junit dependency (indicates JUnit 4)
 * 3. Checks for junit-jupiter dependency (indicates JUnit 5)
 *
 * Decision matrix:
 * - Pure JUnit 5 (platform + JUnit 5 only) → Skip injection
 * - Pure JUnit 4 (no platform + JUnit 4) → Enable injection
 * - Mixed (platform + both) → Enable injection for JUnit Vintage support
 * - Unknown → Skip injection (safe default)
 */
class JUnitVersionDetector(
    private val project: Project,
    private val logger: Logger,
    private val debugEnabled: Boolean = false,
) {
    /**
     * Determines whether JUnit 4 @Rule injection should be enabled.
     *
     * @return true if injection should be enabled, false otherwise
     */
    fun shouldInjectJUnit4Rule(): Boolean {
        val usesJUnitPlatform = detectsJUnitPlatform()
        val hasJUnit4 = hasJUnit4Dependency()
        val hasJUnit5 = hasJUnit5Dependency()

        return when {
            // Pure JUnit 5 (with JUnit Platform) - no injection needed
            usesJUnitPlatform && hasJUnit5 && !hasJUnit4 -> {
                if (debugEnabled) {
                    logger.debug("Auto-detected pure JUnit 5 project - skipping @Rule injection")
                }
                false
            }

            // Pure JUnit 4 (no JUnit Platform) - needs injection
            !usesJUnitPlatform && hasJUnit4 -> {
                logger.info("Auto-detected JUnit 4 project - enabling @Rule injection")
                true
            }

            // Mixed (JUnit Vintage) - inject for JUnit 4 tests
            usesJUnitPlatform && hasJUnit4 -> {
                logger.info(
                    "Auto-detected mixed JUnit 4 + JUnit 5 project - enabling @Rule injection for JUnit 4 tests",
                )
                true
            }

            // Unknown or no JUnit - don't inject (safe default)
            else -> {
                if (debugEnabled) {
                    logger.debug(
                        "Could not auto-detect JUnit version " +
                            "(usesJUnitPlatform=$usesJUnitPlatform, hasJUnit4=$hasJUnit4, hasJUnit5=$hasJUnit5) " +
                            "- skipping @Rule injection",
                    )
                }
                false
            }
        }
    }

    /**
     * Checks if any Test task uses JUnit Platform.
     * JUnit Platform is used when test tasks call useJUnitPlatform().
     *
     * @return true if any test task uses JUnit Platform
     */
    private fun detectsJUnitPlatform(): Boolean =
        project.tasks.withType(Test::class.java).any { testTask ->
            try {
                // JUnit Platform is used if the test task has JUnitPlatformOptions
                testTask.options is org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Checks if the project has a JUnit 4 dependency.
     * Looks for org.junit:junit:4.x in test dependencies (without resolving configurations).
     *
     * This uses allDependencies instead of resolving the configuration to avoid
     * configuration-time resolution issues.
     *
     * @return true if JUnit 4 dependency is present
     */
    private fun hasJUnit4Dependency(): Boolean {
        val configNames =
            listOf(
                "testImplementation", // Standard JVM
                "testRuntimeClasspath", // Standard JVM (fallback)
                "jvmTestImplementation", // KMP JVM target
                "testDebugImplementation", // Android
            )

        return configNames.any { configName ->
            try {
                val config = project.configurations.findByName(configName)
                config?.allDependencies?.any { dep ->
                    dep.group == "junit" && dep.name == "junit"
                } ?: false
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Checks if the project has a JUnit 5 (Jupiter) dependency.
     * Looks for org.junit.jupiter:junit-jupiter* in test dependencies (without resolving configurations).
     *
     * This uses allDependencies instead of resolving the configuration to avoid
     * configuration-time resolution issues.
     *
     * @return true if JUnit 5 dependency is present
     */
    private fun hasJUnit5Dependency(): Boolean {
        val configNames =
            listOf(
                "testImplementation", // Standard JVM
                "testRuntimeClasspath", // Standard JVM (fallback)
                "jvmTestImplementation", // KMP JVM target
                "testDebugImplementation", // Android
            )

        return configNames.any { configName ->
            try {
                val config = project.configurations.findByName(configName)
                config?.allDependencies?.any { dep ->
                    dep.group == "org.junit.jupiter" && dep.name?.startsWith("junit-jupiter") == true
                } ?: false
            } catch (e: Exception) {
                false
            }
        }
    }
}
