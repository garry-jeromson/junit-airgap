plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(17)

    // JVM target
    jvm()

    sourceSets {
        // Common source sets
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
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
