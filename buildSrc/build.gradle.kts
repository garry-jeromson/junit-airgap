plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

// Suppress experimental language version warning (Gradle 9.1.0 uses Kotlin 2.2.0)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}
