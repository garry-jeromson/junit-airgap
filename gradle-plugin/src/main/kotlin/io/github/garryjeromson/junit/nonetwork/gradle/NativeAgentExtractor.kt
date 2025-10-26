package io.github.garryjeromson.junit.nonetwork.gradle

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File
import java.io.FileOutputStream

/**
 * Extracts the JVMTI native agent from plugin resources to the build directory.
 *
 * The native agent is packaged inside the Gradle plugin JAR as a resource.
 * This class detects the current platform, extracts the appropriate agent binary,
 * and returns its path for use in JVM arguments.
 */
object NativeAgentExtractor {
    /**
     * Platform information for agent selection.
     */
    data class Platform(
        val os: String,
        val arch: String,
        val agentFileName: String,
    ) {
        val resourcePath: String
            get() = "native/$os-$arch/$agentFileName"
    }

    /**
     * Detect the current platform and return the appropriate agent metadata.
     *
     * @return Platform information, or null if unsupported
     */
    fun detectPlatform(): Platform? {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()

        // Normalize OS name
        val os =
            when {
                osName.contains("mac") || osName.contains("darwin") -> "darwin"
                osName.contains("linux") -> "linux"
                osName.contains("windows") -> "windows"
                else -> return null
            }

        // Normalize architecture
        val arch =
            when {
                osArch.contains("aarch64") || osArch.contains("arm64") -> "aarch64"
                osArch.contains("x86_64") || osArch.contains("amd64") -> "x86-64"
                else -> return null
            }

        // Determine agent file name
        val agentFileName =
            when (os) {
                "darwin" -> "libjunit-no-network-agent.dylib"
                "linux" -> "libjunit-no-network-agent.so"
                "windows" -> "junit-no-network-agent.dll"
                else -> return null
            }

        return Platform(os, arch, agentFileName)
    }

    /**
     * Extract the native agent for the current platform to the build directory.
     *
     * @param buildDir Build directory (for configuration cache compatibility)
     * @param logger Logger for diagnostic messages
     * @param debug Whether debug logging is enabled
     * @return Path to the extracted agent, or null if extraction failed
     */
    fun extractAgent(
        buildDir: File,
        logger: Logger,
        debug: Boolean = false,
    ): File? {
        val platform = detectPlatform()
        if (platform == null) {
            logger.warn(
                "JUnit No-Network: Unsupported platform (os=${System.getProperty("os.name")}, " +
                    "arch=${System.getProperty("os.arch")}). JVMTI agent will not be loaded.",
            )
            return null
        }

        if (debug) {
            logger.debug("Detected platform: ${platform.os}-${platform.arch}")
        }

        // Extract to build/junit-no-network/native/
        val extractDir = File(buildDir, "junit-no-network/native")
        val extractedAgent = File(extractDir, platform.agentFileName)

        // Skip extraction if agent already exists and is up-to-date
        if (extractedAgent.exists()) {
            logger.debug("Native agent already extracted: ${extractedAgent.absolutePath}")
            return extractedAgent
        }

        // Create extraction directory
        extractDir.mkdirs()

        // Load resource from plugin JAR
        val resourcePath = platform.resourcePath
        val resourceStream = NativeAgentExtractor::class.java.classLoader.getResourceAsStream(resourcePath)

        if (resourceStream == null) {
            logger.warn(
                "JUnit No-Network: Native agent not found in plugin resources at '$resourcePath'. " +
                    "This may indicate the agent was not packaged correctly for ${platform.os}-${platform.arch}. " +
                    "Tests will run without network blocking.",
            )
            return null
        }

        // Extract agent to build directory
        try {
            resourceStream.use { input ->
                FileOutputStream(extractedAgent).use { output ->
                    input.copyTo(output)
                }
            }

            // Make executable on Unix-like systems
            if (platform.os != "windows") {
                extractedAgent.setExecutable(true, false)
            }

            if (debug) {
                logger.debug("Extracted JVMTI agent to: ${extractedAgent.absolutePath}")
            }
            return extractedAgent
        } catch (e: Exception) {
            logger.error("Failed to extract JVMTI agent: ${e.message}", e)
            return null
        }
    }

    /**
     * Get the path to the native agent, extracting it if necessary.
     *
     * Convenience method that combines detection and extraction.
     *
     * @param buildDir Build directory (for configuration cache compatibility)
     * @param logger Logger for diagnostic messages
     * @param debug Whether debug logging is enabled
     * @return Absolute path to the agent, or null if unavailable
     */
    fun getAgentPath(
        buildDir: File,
        logger: Logger,
        debug: Boolean = false,
    ): String? {
        return extractAgent(buildDir, logger, debug)?.absolutePath
    }

    /**
     * Get the path to the native agent, extracting it if necessary (Project overload).
     *
     * @param project Gradle project
     * @param logger Logger for diagnostic messages
     * @param debug Whether debug logging is enabled
     * @return Absolute path to the agent, or null if unavailable
     * @deprecated Use getAgentPath(File, Logger, Boolean) instead for configuration cache compatibility
     */
    @Deprecated("Use getAgentPath(File, Logger, Boolean) instead")
    fun getAgentPath(
        project: Project,
        logger: Logger,
        debug: Boolean = false,
    ): String? {
        return extractAgent(project.layout.buildDirectory.get().asFile, logger, debug)?.absolutePath
    }
}
