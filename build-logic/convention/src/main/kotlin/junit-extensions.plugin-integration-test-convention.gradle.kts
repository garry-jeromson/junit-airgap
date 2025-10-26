/**
 * Convention plugin for plugin integration test projects.
 *
 * Automatically adds publishToMavenLocal task dependency to ensure
 * the gradle-plugin is published to Maven Local before tests run.
 *
 * This ensures that:
 * - Individual test project runs always use the current plugin code
 * - Full test suite runs publish the plugin once before all tests
 * - No manual publishing step is needed after plugin changes
 */

// Configure all test tasks to depend on publishing the gradle plugin
tasks.withType<Test>().configureEach {
    // Find the included build and add task dependency
    gradle.includedBuilds.find { it.name == "junit-extensions-build" }?.let { includedBuild ->
        dependsOn(includedBuild.task(":gradle-plugin:publishToMavenLocal"))
    }
}
