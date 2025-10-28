# Gradle Plugin Configuration Guide

This guide provides comprehensive documentation for the JUnit Airgap Gradle plugin.

## Plugin Overview

The Gradle plugin simplifies setup by automatically:
- Adding the library dependency to test classpaths
- Creating `junit-platform.properties` for JUnit 5 auto-detection
- Configuring test tasks with appropriate system properties
- Optionally injecting `@Rule` fields for JUnit 4 (bytecode enhancement)

## Basic Setup

```kotlin
plugins {
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-SNAPSHOT"
}

junitAirgap {
    enabled = true
}
```

## Complete Configuration Reference

```kotlin
junitAirgap {
    // Whether the plugin is enabled (default: true)
    enabled = true

    // Apply network blocking to all tests by default (default: false)
    // When true: tests block network unless annotated with @AllowNetworkRequests
    // When false: tests only block when annotated with @BlockNetworkRequests
    applyToAllTests = false

    // Library version to use (default: matches plugin version)
    libraryVersion = "0.1.0-SNAPSHOT"

    // List of allowed host patterns (optional)
    // Supports wildcards: "*.example.com", "*.test.local"
    allowedHosts = listOf("localhost", "127.0.0.1")

    // List of blocked host patterns (optional)
    // Blocked hosts take precedence over allowed hosts
    blockedHosts = listOf("evil.com", "*.tracking.com")

    // Enable debug logging (default: false)
    debug = false

    // Enable automatic @Rule injection for JUnit 4 (default: false)
    // Uses ByteBuddy to inject AirgapRule into test classes
    injectJUnit4Rule = false
}
```

## Configuration Properties

### enabled

Controls whether the plugin functionality is active.

```kotlin
junitAirgap {
    enabled = true // Plugin active
}
```

**Use cases**:
- Disable for specific modules: `enabled = false`
- Conditional activation: `enabled = project.hasProperty("enableNoNetwork")`

### applyToAllTests

Changes the default behavior from opt-in to opt-out.

```kotlin
junitAirgap {
    applyToAllTests = true // Block by default
}
```

**When false (default)**:
```kotlin
@Test
@BlockNetworkRequests // Explicit opt-in
fun test() { }
```

**When true**:
```kotlin
@Test // Network blocked by default
fun test1() { }

@Test
@AllowNetworkRequests // Explicit opt-out
fun test2() { }
```

### libraryVersion

Specifies which version of the library to use.

```kotlin
junitAirgap {
    libraryVersion = "0.1.0-SNAPSHOT"
}
```

**Use cases**:
- Pin to specific version: `libraryVersion = "1.2.3"`
- Use different version than plugin: `libraryVersion = "0.2.0"`

### allowedHosts

Globally configure allowed hosts for all tests.

```kotlin
junitAirgap {
    allowedHosts = listOf(
        "localhost",
        "127.0.0.1",
        "*.test.local",
        "*.staging.mycompany.com"
    )
}
```

**Features**:
- Wildcard support: `*.example.com` matches all subdomains
- IPv4 and IPv6: `127.0.0.1`, `::1`
- Hostname and domain patterns

**Note**: Individual tests can override with `@AllowRequestsToHosts` annotation.

### blockedHosts

Globally configure blocked hosts for all tests.

```kotlin
junitAirgap {
    allowedHosts = listOf("*") // Allow all
    blockedHosts = listOf(
        "evil.com",
        "*.tracking.com",
        "analytics.example.com"
    )
}
```

**Priority**: Blocked hosts **always** take precedence over allowed hosts.

### debug

Enables debug logging for troubleshooting.

```kotlin
junitAirgap {
    debug = true
}
```

**Output examples**:
```
NetworkBlocker: Using SECURITY_MANAGER implementation
NetworkBlocker: Installing network blocker
NetworkBlocker: Configuration: allowedHosts=[localhost], blockedHosts=[]
```

### injectJUnit4Rule (Experimental)

Automatically injects `@Rule val noNetworkRule = AirgapRule()` into JUnit 4 test classes using ByteBuddy.

```kotlin
junitAirgap {
    injectJUnit4Rule = true
}
```

**Benefits**:
- No manual `@Rule` declarations needed
- Works with `@BlockNetworkRequests` annotation directly
- Cleaner test code

**Limitations**:
- Experimental feature
- May not work with all build configurations
- Fallback: Use manual `@Rule` configuration

**Example**:

Without injection:
```kotlin
class MyTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

    @Test
    @BlockNetworkRequests
    fun test() { }
}
```

With injection:
```kotlin
class MyTest {
    // No @Rule needed!

    @Test
    @BlockNetworkRequests
    fun test() { }
}
```

## Project Type-Specific Configuration

### Pure JVM Project

```kotlin
plugins {
    kotlin("jvm")
    id("io.github.garryjeromson.junit-airgap")
}

junitAirgap {
    enabled = true
}

tasks.withType<Test> {
    useJUnitPlatform() // For JUnit 5
}
```

### Android Library/App

```kotlin
plugins {
    id("com.android.library")
    kotlin("android")
    id("io.github.garryjeromson.junit-airgap")
}

junitAirgap {
    enabled = true
    injectJUnit4Rule = true // Recommended for Android
}

android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
```

### Kotlin Multiplatform

```kotlin
plugins {
    kotlin("multiplatform")
    id("io.github.garryjeromson.junit-airgap")
}

junitAirgap {
    enabled = true
}

kotlin {
    jvm()
    androidTarget()
    iosSimulatorArm64()

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation("io.github.garryjeromson:junit-airgap:0.1.0-SNAPSHOT")
            }
        }
    }
}
```

## Configuration Patterns

### Development vs Production

```kotlin
junitAirgap {
    enabled = !project.hasProperty("skipNoNetwork")
    debug = project.hasProperty("debugNoNetwork")
}
```

Run with: `./gradlew test -PdebugNoNetwork`

### Per-Module Configuration

```kotlin
// Root build.gradle.kts
allprojects {
    pluginManager.withPlugin("io.github.garryjeromson.junit-airgap") {
        configure<JunitAirgapExtension> {
            enabled = true
            allowedHosts = listOf("localhost", "*.test.local")
        }
    }
}

// Specific module
junitAirgap {
    enabled = false // Disable for this module
}
```

### Environment-Specific Hosts

```kotlin
junitAirgap {
    allowedHosts = when {
        project.hasProperty("ci") -> listOf("localhost") // CI: strict
        else -> listOf("localhost", "*.staging.mycompany.com") // Local: relaxed
    }
}
```

## Plugin Tasks

The plugin doesn't create user-facing tasks. It hooks into existing Gradle test tasks.

### JUnit 5 Auto-Detection

For JUnit 5 projects, the plugin creates `src/test/resources/junit-platform.properties`:

```properties
junit.jupiter.extensions.autodetection.enabled=true
```

This enables automatic discovery of `AirgapExtension`.

### JUnit 4 Bytecode Injection

When `injectJUnit4Rule = true`, the plugin:
1. Waits for test compilation to complete
2. Uses ByteBuddy to scan compiled test classes
3. Injects `@Rule val noNetworkRule = AirgapRule()` into classes with `@BlockNetworkRequests`

## Troubleshooting

### Issue: Plugin not found

**Error**: `Plugin [id: 'io.github.garryjeromson.junit-airgap'] was not found`

**Solution**: Ensure plugin is published to Maven Local or plugin portal:

```bash
./gradlew :gradle-plugin:publishToMavenLocal
```

### Issue: Configuration not applied

**Checklist**:
1. Is plugin applied? `plugins { id("io.github.garryjeromson.junit-airgap") }`
2. Is plugin block present? `junitAirgap { enabled = true }`
3. Check debug output: `debug = true`

### Issue: JUnit 4 injection not working

**Solution**:
1. Verify `injectJUnit4Rule = true`
2. Check if test classes are compiled before injection
3. Fallback to manual `@Rule` configuration
4. Enable debug: `debug = true`

### Issue: Tests still make network requests

**Checklist**:
1. Is `@BlockNetworkRequests` annotation present?
2. For JUnit 4 without injection: Is `@Rule` declared?
3. For JUnit 5: Is `junit-platform.properties` present?
4. Check with system property: `-Djunit.airgap.debug=true`


## Plugin Architecture

The plugin:

1. **Applies to Test Source Sets**: Adds library dependency to test compile and runtime classpaths
2. **JUnit 5**: Creates `junit-platform.properties` for auto-detection
3. **JUnit 4**: Optionally injects `@Rule` fields using ByteBuddy
4. **System Properties**: Configures test tasks with appropriate properties

## Disabling the Plugin

### Temporarily Disable

```kotlin
junitAirgap {
    enabled = false
}
```

### Remove Plugin

```kotlin
plugins {
    // Remove or comment out:
    // id("io.github.garryjeromson.junit-airgap")
}
```

Then remove manual configurations:
- Delete `junit-platform.properties` (JUnit 5)
- Remove `@Rule` fields (JUnit 4)
- Remove test annotations

## See Also

- [JVM + JUnit 5 Setup Guide](jvm-junit5.md)
- [JVM + JUnit 4 Setup Guide](jvm-junit4.md)
- [Android + JUnit 4 Setup Guide](android-junit4.md)
- [KMP Setup Guides](kmp-junit5.md)
- [Compatibility Matrix](../compatibility-matrix.md)
