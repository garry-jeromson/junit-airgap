# Claude Code Context for junit-no-network

This document provides essential context for working on the junit-no-network project.

## Critical Requirements

### Always Use Java 21 for Gradle Commands

**IMPORTANT**: All Gradle commands MUST use Java 21. Set `JAVA_HOME` before every Gradle invocation:

```bash
JAVA_HOME=/Users/garry.jeromson/Library/Java/JavaVirtualMachines/temurin-21.0.4/Contents/Home ./gradlew <task>
```

This is required because:
- The project targets Java 21
- JVMTI agent requires Java 21+ for native agent support
- Android and KMP configurations require specific JVM features

### Test-First Development

Always follow test-first principles:
1. Write or update tests first
2. Run tests to see them fail
3. Implement the feature
4. Run tests to see them pass
5. Refactor if needed

## Project Structure

This is a Kotlin Multiplatform (KMP) project with three main components:

1. **`junit-no-network/`** - Core library (JVM + Android targets)
2. **`gradle-plugin/`** - Gradle plugin for zero-configuration setup
3. **`plugin-integration-tests/`** - Integration tests for different project configurations

### Integration Test Projects

Located in `plugin-integration-tests/`:
- `test-contracts` - Shared KMP module with generic test assertions (used by all projects below)
- `jvm-junit4` - Pure JVM with JUnit 4
- `jvm-junit5` - Pure JVM with JUnit 5
- `android-robolectric` - Android library with JUnit 4 + Robolectric
- `kmp-junit4` - KMP (JVM + Android) with JUnit 4
- `kmp-junit5` - KMP (JVM + Android) with JUnit 5
- `kmp-kotlintest` - KMP (JVM + Android) with kotlin.test + JUnit 4 runtime
- `kmp-kotlintest-junit5` - KMP (JVM + Android) with kotlin.test + JUnit 5 runtime

All integration test projects use the `test-contracts` module for shared test assertions, eliminating code duplication.

## Running Tests

### Quick Commands (via Makefile)

```bash
# Run all tests
make test

# Run JVM-only tests (fastest)
make test-jvm

# Run Android tests
make test-android

# Run integration tests
make test-integration

# Run plugin integration tests
make test-plugin-integration

# Format code with ktlint
make format

# Lint code
make lint
```

### Direct Gradle Commands

Always prefix with `JAVA_HOME`:

```bash
# Run JVM tests
JAVA_HOME=/Users/garry.jeromson/Library/Java/JavaVirtualMachines/temurin-21.0.4/Contents/Home ./gradlew :junit-no-network:jvmTest

# Run Android tests
JAVA_HOME=/Users/garry.jeromson/Library/Java/JavaVirtualMachines/temurin-21.0.4/Contents/Home ./gradlew :junit-no-network:testDebugUnitTest

# Run specific integration test
JAVA_HOME=/Users/garry.jeromson/Library/Java/JavaVirtualMachines/temurin-21.0.4/Contents/Home ./gradlew :plugin-integration-tests:jvm-junit4:test

# Run Android Robolectric tests
JAVA_HOME=/Users/garry.jeromson/Library/Java/JavaVirtualMachines/temurin-21.0.4/Contents/Home ./gradlew :plugin-integration-tests:android-robolectric:testDebugUnitTest
```

## Plugin Development Workflow

The Gradle plugin is tested using integration tests that consume it from Maven Local.

### Publishing Plugin Changes

After modifying the plugin, you MUST publish it to Maven Local before integration tests can see the changes:

```bash
JAVA_HOME=/Users/garry.jeromson/Library/Java/JavaVirtualMachines/temurin-21.0.4/Contents/Home ./gradlew :gradle-plugin:publishToMavenLocal
```

Then run integration tests:

```bash
make test-plugin-integration
```

### Plugin Architecture

The plugin provides two integration paths:

1. **JUnit 5**: Automatic extension discovery via `junit-platform.properties`
2. **JUnit 4**: Bytecode injection via `JUnit4RuleInjectionTask` using ByteBuddy

#### JUnit 4 Bytecode Injection

- Uses ByteBuddy to inject `@Rule NoNetworkRule` field into test classes
- Scans compiled test classes and adds the rule automatically
- Resolves Test task's classpath at execution time (not configuration time)
- **Known Issue**: Android/KMP projects currently have task wiring challenges

## Code Quality

### Formatting with ktlint

The project uses ktlint for Kotlin code formatting. Always format before committing:

```bash
make format
```

Or:

```bash
JAVA_HOME=/Users/garry.jeromson/Library/Java/JavaVirtualMachines/temurin-21.0.4/Contents/Home ./gradlew ktlintFormat
```

### Running ktlint Check

```bash
make lint
```

## Network Blocking Implementation

The library uses JVMTI (JVM Tool Interface) for network blocking on all JVM platforms.

### JVMTI Implementation (JVM and Android)

**Single unified implementation** using C++ JVMTI agent:
- Intercepts socket and DNS operations at the native level
- Works on Java 21+ (no SecurityManager dependency)
- Agent automatically packaged with library and extracted at runtime
- Supports both hostname and IP address blocking
- Includes DNS interception for more reliable blocking
- Platform-agnostic: same implementation for JVM and Android (Robolectric)

**How it works**:
1. JVMTI agent (`libjunit-no-network-agent.dylib` / `.so` / `.dll`) packaged in JAR
2. At test time, agent extracted to temporary directory
3. Agent loaded via JVM attach API or agent command-line option
4. Agent hooks native socket connection and DNS resolution functions
5. Connections checked against allowed/blocked host lists
6. Unauthorized connections throw `NetworkRequestAttemptedException`

**Native code location**: `native/src/`
- `agent.cpp` - JVMTI agent initialization and JNI interface
- `socket_interceptor.cpp` - Socket interception logic
- `dns_interceptor.cpp` - DNS resolution interception

### iOS Implementation

**Status**: API structure only, no active blocking
- iOS uses Kotlin/Native which doesn't support JVMTI
- Provides API compatibility for KMP projects
- No actual network blocking occurs on iOS

## Test Contracts Module

The `test-contracts` module provides generic, client-agnostic test assertions used by all integration test projects.

### Architecture

**Location**: `plugin-integration-tests/test-contracts/`

**Structure**:
- KMP module with JVM and Android targets
- Uses expect/actual pattern for platform-specific implementations
- Provides two main assertion helpers

### Generic Assertions

```kotlin
// Assert that network is blocked
assertRequestBlocked {
    // Any network operation here
    Socket("example.com", 80).use { }
    // or
    ktorClient.get("https://example.com")
    // or
    retrofit.getData().execute()
}

// Assert that network is allowed
assertRequestAllowed {
    // Any network operation here
    Socket("example.com", 80).use { }
}
```

### Benefits

1. **Client-Agnostic**: Works with any HTTP client (Socket, Ktor, Retrofit, OkHttp, ReactorNetty, etc.)
2. **Platform-Specific Handling**: Automatically handles exception wrapping differences between JVM and Android
3. **Code Reduction**: Eliminated ~500+ lines of duplicated exception handling logic
4. **Maintainability**: Changes to assertion logic only need to be made once
5. **Consistency**: All integration tests use the same patterns

### Platform Implementations

**JVM** (`jvmMain/Assertions.jvm.kt`):
- Checks for `NetworkRequestAttemptedException` directly or in cause chain
- Handles wrapped exceptions from frameworks like Reactor

**Android** (`androidMain/Assertions.android.kt`):
- Checks exception by class name (Robolectric limitation)
- Handles exceptions wrapped in `IOException`
- Searches entire cause chain for network exceptions

### Usage in Integration Tests

All 7 integration test projects use test-contracts:

```kotlin
// Common pattern across all projects
@Test
@BlockNetworkRequests
fun testNetworkBlocked() {
    assertRequestBlocked {
        makeHttpRequest() // Any HTTP client
    }
}
```

See `plugin-integration-tests/test-contracts/README.md` for complete documentation.

## Common Issues and Solutions

### Issue: Gradle configuration phase errors with KMP projects

**Symptom**: `DefaultTaskContainer#NamedDomainObjectProvider.configure(Action) on task set cannot be executed in the current context`

**Status**: Known issue with automatic task wiring for Android/KMP projects

**Workaround**: KMP projects currently don't have automatic task dependency wiring. The injection tasks are registered but need manual execution if needed.

### Issue: Integration tests fail after plugin changes

**Solution**: Always publish to Maven Local after modifying the plugin:

```bash
JAVA_HOME=/Users/garry.jeromson/Library/Java/JavaVirtualMachines/temurin-21.0.4/Contents/Home ./gradlew :gradle-plugin:publishToMavenLocal
```

### Issue: "Unsupported class file major version 65" or similar

**Solution**: Ensure you're using Java 21 for all Gradle commands. Check with:

```bash
echo $JAVA_HOME
```

Should output: `/Users/garry.jeromson/Library/Java/JavaVirtualMachines/temurin-21.0.4/Contents/Home`

### Issue: JVMTI agent not loading or native library errors

**Solution**: The JVMTI agent should be automatically extracted and loaded. If you see errors:
1. Check that Java 21+ is being used
2. Verify the native library is being extracted to the temp directory
3. Check file permissions on the extracted agent library
4. On macOS, ensure the dylib is not being blocked by Gatekeeper

## Project Configuration Files

- `gradle.properties` - Gradle JVM settings, Kotlin version
- `settings.gradle.kts` - Multi-module project structure
- `libs.versions.toml` - Dependency version catalog
- `Makefile` - Convenient test and build commands

## Git Workflow

### Committing Changes

Create separate commits for each logical change:
- Formatting changes separate from functional changes
- Each feature/fix in its own commit
- Clear, descriptive commit messages

### Example Commit Messages

```
Add unit tests for JUnit 4 rule injection logic

Refactor JUnit4RuleInjectionTask to resolve test classpath at execution time

Apply ktlint formatting to benchmark module
```

## Key Architecture Decisions

1. **JVMTI for network blocking**: Native agent intercepts socket/DNS at the lowest level, works on Java 21+
2. **ByteBuddy for JUnit 4**: Enables zero-configuration by injecting `@Rule` fields at compile time
3. **Execution-time classpath resolution**: Injection task resolves Test task classpath when it runs, not during Gradle configuration
4. **Single implementation for JVM/Android**: JVMTI agent works identically on both platforms
5. **Gradle Plugin separate from library**: Allows users to use the library directly without the plugin

## Testing Philosophy

- **Unit tests** for core logic (fast feedback)
- **Integration tests** for library behavior in real projects
- **Plugin integration tests** for different project types
- **Benchmark tests** for performance tracking (informational only)

### Test Naming Conventions

Tests should use **declarative naming** that describes what the test IS testing, not what it "should" do.

**Good (declarative):**
```kotlin
@Test
fun `blocks HTTP requests to external hosts`()

@Test
fun `allows requests to localhost`()

@Test
fun `supports wildcard patterns in allowed hosts`()
```

**Bad (prescriptive):**
```kotlin
@Test
fun `should block HTTP requests to external hosts`()  // ❌ Don't use "should"

@Test
fun `should allow requests to localhost`()  // ❌ Don't use "should"

@Test
fun `should support wildcard patterns in allowed hosts`()  // ❌ Don't use "should"
```

**Rationale**: Declarative names are more concise and read naturally in test reports. They describe the behavior directly rather than expressing an expectation.

## Performance Notes

- JVM tests: ~10-30 seconds
- Android tests: ~1-2 minutes (includes Robolectric)
- Plugin integration tests: ~2-3 minutes per project
- Bytecode injection: <1 second for typical projects

## Documentation

- `README.md` - User-facing documentation
- `CLAUDE.md` - This file (development context)
- JavaDoc/KDoc - In-code documentation
- Test examples - Best documentation for usage patterns
