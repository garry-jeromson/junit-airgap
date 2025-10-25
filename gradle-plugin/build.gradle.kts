plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
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
        }
    }
}
