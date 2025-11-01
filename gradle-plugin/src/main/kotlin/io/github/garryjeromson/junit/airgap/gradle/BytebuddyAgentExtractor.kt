package io.github.garryjeromson.junit.airgap.gradle

import org.gradle.api.logging.Logger
import java.io.File
import java.io.FileOutputStream

/**
 * Extracts the ByteBuddy Java agent from plugin resources to the build directory.
 *
 * The ByteBuddy agent is packaged inside the Gradle plugin JAR as a resource.
 * Unlike the JVMTI native agent, this is a pure Java agent that works on all platforms.
 *
 * ## Why ByteBuddy Agent?
 *
 * The ByteBuddy agent provides DNS interception at the Java API layer (InetAddress.getAllByName)
 * as a fallback when JVMTI native interception fails. This happens when:
 * - DNS classes are loaded before the JVMTI agent completes initialization
 * - Multiple JVM instances spawn (e.g., Android Studio test runners)
 * - Native methods are bound before NativeMethodBindCallback fires
 *
 * @see io.github.garryjeromson.junit.airgap.bytebuddy.InetAddressBytebuddyAgent
 */
object BytebuddyAgentExtractor {
    private const val AGENT_RESOURCE_PATH = "bytebuddy-agent/junit-airgap-bytebuddy-agent.jar"
    private const val AGENT_FILE_NAME = "junit-airgap-bytebuddy-agent.jar"

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
                    "ByteBuddy agent size mismatch (extracted=$extractedSize, resource=$actualResourceSize) - will re-extract",
                )
                return false
            }

            return true
        } catch (e: Exception) {
            logger.debug("[junit-airgap:plugin] Failed to check ByteBuddy agent version: ${e.message} - will re-extract")
            return false
        }
    }

    /**
     * Extract the ByteBuddy agent to the build directory.
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
        // Extract to build/junit-airgap/bytebuddy/
        val extractDir = File(buildDir, "junit-airgap/bytebuddy")
        val extractedAgent = File(extractDir, AGENT_FILE_NAME)

        // Load resource from plugin JAR
        val resourceStream =
            BytebuddyAgentExtractor::class.java.classLoader.getResourceAsStream(AGENT_RESOURCE_PATH)

        if (resourceStream == null) {
            logger.warn(
                "JUnit Airgap: ByteBuddy agent not found in plugin resources at '$AGENT_RESOURCE_PATH'. " +
                    "This may indicate the agent was not packaged correctly. " +
                    "DNS interception will rely solely on JVMTI native interception.",
            )
            return null
        }

        // Check if we need to re-extract by comparing resource size with extracted file size
        val needsExtraction = !extractedAgent.exists() || !isAgentUpToDate(extractedAgent, resourceStream, logger)

        // Close the stream and reopen if we don't need to extract
        if (!needsExtraction) {
            resourceStream.close()
            if (debug) {
                logger.debug("[junit-airgap:plugin] ByteBuddy agent already up-to-date: ${extractedAgent.absolutePath}")
            }
            return extractedAgent
        }

        // Create extraction directory
        extractDir.mkdirs()

        // Re-open the resource stream for extraction (we consumed it during size check)
        val extractionStream =
            BytebuddyAgentExtractor::class.java.classLoader.getResourceAsStream(AGENT_RESOURCE_PATH)
                ?: run {
                    logger.error("Failed to re-open ByteBuddy agent resource stream for extraction")
                    return null
                }

        // Extract agent to build directory
        try {
            extractionStream.use { input ->
                FileOutputStream(extractedAgent).use { output ->
                    input.copyTo(output)
                }
            }

            if (debug) {
                logger.debug("[junit-airgap:plugin] Extracted ByteBuddy agent to: ${extractedAgent.absolutePath}")
            }
            return extractedAgent
        } catch (e: Exception) {
            logger.error("Failed to extract ByteBuddy agent: ${e.message}", e)
            return null
        }
    }

    /**
     * Get the path to the ByteBuddy agent, extracting it if necessary.
     *
     * Convenience method that combines detection and extraction.
     *
     * @param buildDir Build directory (for configuration cache compatibility)
     * @param logger Logger for diagnostic messages
     * @param debug Whether debug logging is enabled
     * @return Absolute path to the agent JAR, or null if unavailable
     */
    fun getAgentPath(
        buildDir: File,
        logger: Logger,
        debug: Boolean = false,
    ): String? = extractAgent(buildDir, logger, debug)?.absolutePath
}
