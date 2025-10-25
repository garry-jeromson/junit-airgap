plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.nexus.publish)
}

allprojects {
    group = "io.github.garryjeromson"
    version = "0.1.0-SNAPSHOT"

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.3.1")
        android.set(true)
        verbose.set(true)
        outputToConsole.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(false)

        filter {
            exclude("**/build/**")
        }
    }
}

// Configure Nexus publishing for Maven Central
nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(findProperty("ossrhUsername") as String? ?: System.getenv("ORG_GRADLE_PROJECT_ossrhUsername"))
            password.set(findProperty("ossrhPassword") as String? ?: System.getenv("ORG_GRADLE_PROJECT_ossrhPassword"))
        }
    }
}
