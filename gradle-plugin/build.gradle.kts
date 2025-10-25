plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    signing
}

group = "io.github.garryjeromson"
version = "0.1.0-SNAPSHOT"

gradlePlugin {
    plugins {
        create("junitNoNetworkPlugin") {
            id = "io.github.garryjeromson.junit-no-network"
            implementationClass = "io.github.garryjeromson.junit.nonetwork.gradle.JunitNoNetworkPlugin"
            displayName = "JUnit No-Network Plugin"
            description = "Automatically configure JUnit tests to block network requests"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))

    // ByteBuddy for JUnit 4 rule injection via bytecode enhancement
    implementation("net.bytebuddy:byte-buddy:1.15.11")

    // JUnit 4 API for detecting test annotations
    implementation("junit:junit:4.13.2")

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = "io.github.garryjeromson"
            artifactId = "junit-no-network-gradle-plugin"
            version = "0.1.0-SNAPSHOT"

            // POM metadata for Maven Central
            pom {
                name.set("JUnit No-Network Gradle Plugin")
                description.set("Gradle plugin for automatically configuring JUnit tests to block network requests")
                url.set("https://github.com/garry-jeromson/junit-request-blocker")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("garry-jeromson")
                        name.set("Garry Jeromson")
                        email.set("garry.jeromson@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/garry-jeromson/junit-request-blocker.git")
                    developerConnection.set("scm:git:ssh://github.com:garry-jeromson/junit-request-blocker.git")
                    url.set("https://github.com/garry-jeromson/junit-request-blocker")
                }
            }
        }
    }
}

// Signing configuration for Maven Central
signing {
    // Only require signing if publishing to Maven Central (not for local builds)
    setRequired { gradle.taskGraph.allTasks.any { it.name.contains("publish") } }

    // Use in-memory key from environment variables or gradle.properties
    val signingKeyId: String? = findProperty("signingKeyId") as String? ?: System.getenv("ORG_GRADLE_PROJECT_signingKeyId")
    val signingKey: String? = findProperty("signingKey") as String? ?: System.getenv("ORG_GRADLE_PROJECT_signingKey")
    val signingPassword: String? = findProperty("signingPassword") as String? ?: System.getenv("ORG_GRADLE_PROJECT_signingPassword")

    if (signingKeyId != null && signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    }
}
