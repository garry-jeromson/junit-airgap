plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Add ktlint plugin to classpath so convention plugins can apply it
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.1")
}

// Suppress experimental language version warning (Gradle 9.1.0 uses Kotlin 2.2.0)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}
