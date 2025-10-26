plugins {
    id("junit-extensions.kotlin-multiplatform")
}

kotlin {
    // Use Java 17 to match junit-no-network compatibility
    jvmToolchain(17)

    // JVM target
    jvm()

    sourceSets {
        // Common source sets
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }

        // JVM source sets
        val jvmMain by getting {
            dependencies {
                // JUnit 5 for AfterAllCallback
                implementation(libs.junit.jupiter.api)
            }
        }
    }
}

// Enable strict compilation - treat all warnings as errors
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}
