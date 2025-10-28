# Gradle Plugin Reference

Complete configuration reference for the JUnit Airgap Gradle plugin.

## Plugin Overview

The Gradle plugin simplifies setup by automatically:
- Adding the library dependency to test classpaths
- Creating `junit-platform.properties` for JUnit 5 auto-detection
- Configuring test tasks with appropriate system properties
- Optionally injecting `@Rule` fields for JUnit 4 (bytecode enhancement)

## Basic Setup

```kotlin
plugins {
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-beta.1"
}
```

That's it! The plugin auto-configures with sensible defaults.

## Configuration

All configuration properties with defaults:

```kotlin
junitAirgap {
    // Enable/disable plugin (default: true)
    enabled = true

    // Block all tests by default (default: false)
    // false: opt-in with @BlockNetworkRequests
    // true: opt-out with @AllowNetworkRequests
    applyToAllTests = false

    // Library version (default: matches plugin version)
    libraryVersion = "0.1.0-beta.1"

    // Global allowed hosts (default: empty)
    allowedHosts = listOf("localhost", "127.0.0.1")

    // Global blocked hosts (default: empty)
    // Blocked hosts take precedence
    blockedHosts = listOf("*.tracking.com")

    // Debug logging (default: false)
    debug = false

    // Auto-inject @Rule for JUnit 4 (default: auto-detected)
    injectJUnit4Rule = null // null = auto-detect, true/false = force
}
```

## Configuration Properties

### enabled

Disable plugin for specific modules:

```kotlin
junitAirgap {
    enabled = false // Skip this module
}
```

Or conditionally:

```kotlin
junitAirgap {
    enabled = project.hasProperty("enableNoNetwork")
}
```

### applyToAllTests

**Default behavior (false)**: Opt-in with `@BlockNetworkRequests`

```kotlin
@Test
@BlockNetworkRequests
fun test() { }
```

**Opt-out behavior (true)**: Block all by default

```kotlin
junitAirgap {
    applyToAllTests = true
}
```

```kotlin
@Test // Blocked by default
fun test1() { }

@Test
@AllowNetworkRequests // Opt-out
fun test2() { }
```

### allowedHosts / blockedHosts

Global host filtering:

```kotlin
junitAirgap {
    allowedHosts = listOf(
        "localhost",
        "127.0.0.1",
        "*.staging.example.com" // Wildcards supported
    )

    blockedHosts = listOf(
        "*.tracking.com",
        "evil.com"
    )
}
```

**Important**: Blocked hosts always take precedence over allowed hosts.

### debug

Enable detailed logging:

```kotlin
junitAirgap {
    debug = true
}
```

Or via command line:

```bash
./gradlew test -Djunit.airgap.debug=true
```

### injectJUnit4Rule

**Auto-detection (default)**: Plugin detects JUnit 4 projects automatically

```kotlin
// No configuration needed - auto-detects
```

**Manual override**:

```kotlin
junitAirgap {
    injectJUnit4Rule = true // Force enable
}
```

## Project Types

### Pure JVM

```kotlin
plugins {
    kotlin("jvm")
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-beta.1"
}
```

### Android

```kotlin
plugins {
    id("com.android.library")
    kotlin("android")
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-beta.1"
}

android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true // For Robolectric
        }
    }
}
```

### Kotlin Multiplatform

```kotlin
plugins {
    kotlin("multiplatform")
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-beta.1"
}

kotlin {
    jvm()
    androidTarget()
    iosSimulatorArm64()
}
```

## Common Patterns

### Development vs CI

```kotlin
junitAirgap {
    val isCi = System.getenv("CI") == "true"

    applyToAllTests = isCi // Strict on CI
    debug = !isCi // Debug locally

    allowedHosts = if (isCi) {
        listOf("localhost")
    } else {
        listOf("localhost", "*.staging.example.com")
    }
}
```

### Per-Module Configuration

**Root build.gradle.kts:**

```kotlin
allprojects {
    pluginManager.withPlugin("io.github.garryjeromson.junit-airgap") {
        configure<JunitAirgapExtension> {
            enabled = true
            allowedHosts = listOf("localhost")
        }
    }
}
```

**Specific module:**

```kotlin
junitAirgap {
    enabled = false // Disable for this module
}
```

### Environment-Specific Hosts

```kotlin
junitAirgap {
    allowedHosts = when (System.getenv("ENV")) {
        "staging" -> listOf("localhost", "*.staging.example.com")
        "production" -> listOf("localhost")
        else -> listOf("localhost", "*.dev.example.com")
    }
}
```

## Plugin Tasks

The plugin automatically configures test tasks. No manual task configuration needed.

**What the plugin does:**
- Adds library to `testImplementation`
- Configures test tasks with system properties
- Creates `junit-platform.properties` for JUnit 5
- Injects `@Rule` fields for JUnit 4 (if enabled)

## Troubleshooting

### Plugin Not Applied

Check plugin block:

```kotlin
plugins {
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-beta.1"
}
```

### Configuration Not Working

1. Is `junitAirgap { }` block present?
2. Check property values with debug mode
3. Verify plugin version matches library version

### JUnit 4 Auto-Injection Not Working

1. Is `junit:junit` in `testImplementation`?
2. Check build output for "Auto-detected JUnit 4" message
3. Try manual override: `injectJUnit4Rule = true`
4. Enable debug logging

## See Also

- [JVM + JUnit 5 Setup](jvm-junit5.md)
- [JVM + JUnit 4 Setup](jvm-junit4.md)
- [Android Setup](android-junit4.md)
- [KMP Setup](kmp-junit5.md)
- [Advanced Configuration](../advanced-configuration.md)
