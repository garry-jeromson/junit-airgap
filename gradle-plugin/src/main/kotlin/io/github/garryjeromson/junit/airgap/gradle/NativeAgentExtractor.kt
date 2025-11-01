package io.github.garryjeromson.junit.airgap.gradle

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
                "darwin" -> "libjunit-airgap-agent.dylib"
                "linux" -> "libjunit-airgap-agent.so"
                "windows" -> "junit-airgap-agent.dll"
                else -> return null
            }

        return Platform(os, arch, agentFileName)
    }

    /**
     * Check if the extracted agent is up-to-date by comparing file sizes.
     *
     * This prevents stale cached agents from being used after plugin updates.
     * We compare the size of the extracted file with the resource in the JAR.
     *
     * @param extractedAgent The extracted agent file
     * @param resourceStream Stream to the agent resource in the JAR
     * @param logger Logger for diagnostic messages
     * @return true if the agent is up-to-date, false if it needs re-extraction
     */
    private fun isAgentUpToDate(
        extractedAgent: File,
        resourceStream: java.io.InputStream,
        logger: Logger,
    ): Boolean {
        if (!extractedAgent.exists()) {
            return false
        }

        try {
            // Get size of extracted file
            val extractedSize = extractedAgent.length()

            // Get size of resource by reading it
            val resourceSize = resourceStream.available().toLong()

            // If available() returns 0, we need to count bytes manually
            val actualResourceSize =
                if (resourceSize == 0L) {
                    var count = 0L
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (resourceStream.read(buffer).also { bytesRead = it } != -1) {
                        count += bytesRead
                    }
                    count
                } else {
                    resourceSize
                }

            if (extractedSize != actualResourceSize) {
                logger.debug(
                    "Native agent size mismatch (extracted=$extractedSize, resource=$actualResourceSize) - will re-extract",
                )
                return false
            }

            return true
        } catch (e: Exception) {
            logger.debug("[junit-airgap:plugin] Failed to check agent version: ${e.message} - will re-extract")
            return false
        }
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
                "JUnit Airgap: Unsupported platform (os=${System.getProperty("os.name")}, " +
                    "arch=${System.getProperty("os.arch")}). JVMTI agent will not be loaded.",
            )
            return null
        }

        if (debug) {
            logger.debug("[junit-airgap:plugin] Detected platform: ${platform.os}-${platform.arch}")
        }

        // Extract to build/junit-airgap/native/
        val extractDir = File(buildDir, "junit-airgap/native")
        val extractedAgent = File(extractDir, platform.agentFileName)

        // Load resource from plugin JAR
        val resourcePath = platform.resourcePath
        val resourceStream = NativeAgentExtractor::class.java.classLoader.getResourceAsStream(resourcePath)

        if (resourceStream == null) {
            logger.warn(
                "JUnit Airgap: Native agent not found in plugin resources at '$resourcePath'. " +
                    "This may indicate the agent was not packaged correctly for ${platform.os}-${platform.arch}. " +
                    "Tests will run without network blocking.",
            )
            return null
        }

        // Check if we need to re-extract by comparing resource size with extracted file size
        val needsExtraction = !extractedAgent.exists() || !isAgentUpToDate(extractedAgent, resourceStream, logger)

        // Close the stream and reopen if we don't need to extract
        // (we needed to open it to check size, but if not extracting we need to close it)
        if (!needsExtraction) {
            resourceStream.close()
            if (debug) {
                logger.debug("[junit-airgap:plugin] Native agent already up-to-date: ${extractedAgent.absolutePath}")
            }
            return extractedAgent
        }

        // Create extraction directory
        extractDir.mkdirs()

        // Re-open the resource stream for extraction (we consumed it during size check)
        val extractionStream =
            NativeAgentExtractor::class.java.classLoader.getResourceAsStream(resourcePath)
                ?: run {
                    logger.error("Failed to re-open resource stream for extraction")
                    return null
                }

        // Extract agent to build directory
        try {
            extractionStream.use { input ->
                FileOutputStream(extractedAgent).use { output ->
                    input.copyTo(output)
                }
            }

            // Make executable on Unix-like systems
            if (platform.os != "windows") {
                extractedAgent.setExecutable(true, false)
            }

            if (debug) {
                logger.debug("[junit-airgap:plugin] Extracted JVMTI agent to: ${extractedAgent.absolutePath}")
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
    ): String? = extractAgent(buildDir, logger, debug)?.absolutePath

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
    ): String? =
        extractAgent(
            project.layout.buildDirectory
                .get()
                .asFile,
            logger,
            debug,
        )?.absolutePath
}
