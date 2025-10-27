package io.github.garryjeromson.junit.airgap.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task that generates junit-platform.properties file for JUnit 5 configuration.
 *
 * This task creates the junit-platform.properties file at execution time (not configuration time)
 * to support Gradle configuration cache. The generated file enables JUnit Jupiter extension
 * auto-detection and configures the junit-no-network extension behavior.
 *
 * Configuration cache compatible: All inputs use Property/ListProperty APIs.
 */
abstract class GenerateJunitPlatformPropertiesTask : DefaultTask() {
    /**
     * Output location for the junit-platform.properties file
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * Whether to apply network blocking to all tests (even without @BlockNetworkRequests)
     */
    @get:Input
    abstract val applyToAllTests: Property<Boolean>

    /**
     * List of allowed host patterns (glob patterns like *.example.com)
     */
    @get:Input
    @get:Optional
    abstract val allowedHosts: ListProperty<String>

    /**
     * List of blocked host patterns (glob patterns like *.ads.com)
     */
    @get:Input
    @get:Optional
    abstract val blockedHosts: ListProperty<String>

    /**
     * Whether debug logging is enabled
     */
    @get:Input
    abstract val debug: Property<Boolean>

    init {
        group = "verification"
        description = "Generate junit-platform.properties for JUnit No-Network extension configuration"
    }

    @TaskAction
    fun generate() {
        val props = buildString {
            // Enable JUnit Jupiter extension auto-detection
            appendLine("junit.jupiter.extensions.autodetection.enabled=true")

            // Configure junit-no-network extension
            appendLine("junit.airgap.applyToAllTests=${applyToAllTests.get()}")

            // Add allowed hosts if configured
            if (allowedHosts.isPresent && allowedHosts.get().isNotEmpty()) {
                val hosts = allowedHosts.get().joinToString(",")
                appendLine("junit.airgap.allowedHosts=$hosts")
            }

            // Add blocked hosts if configured
            if (blockedHosts.isPresent && blockedHosts.get().isNotEmpty()) {
                val hosts = blockedHosts.get().joinToString(",")
                appendLine("junit.airgap.blockedHosts=$hosts")
            }
        }

        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(props)

        if (debug.get()) {
            logger.lifecycle("Generated junit-platform.properties at: ${file.absolutePath}")
            logger.lifecycle("Contents:\n$props")
        }
    }
}
