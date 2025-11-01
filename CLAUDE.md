# Claude Code Context for junit-airgap

## Critical Requirements

### Always Use Java 21 for Gradle Commands

**IMPORTANT**: All Gradle commands MUST use Java 21:

```bash
JAVA_HOME=/Users/garry.jeromson/Library/Java/JavaVirtualMachines/temurin-21.0.4/Contents/Home ./gradlew <task>
```

Required because Kotlin Gradle Plugin requires Java 21+ for build. JVMTI agent works on any Java version at runtime.

### Test-First Development

1. Write/update tests first → 2. See them fail → 3. Implement → 4. See them pass → 5. Refactor

## Project Structure

This is a Kotlin Multiplatform (KMP) project targeting JVM and Android with three main components:

1. **`junit-airgap/`** - Core library (JVM + Android targets)
2. **`gradle-plugin/`** - Gradle plugin for zero-configuration setup
3. **`plugin-integration-tests/`** - Integration tests for different project configurations

### Integration Test Projects

Located in `plugin-integration-tests/`: `test-contracts` (shared assertions), `jvm-junit4/5`, `android-robolectric`, `kmp-junit4/5`, `kmp-kotlintest`, `kmp-kotlintest-junit5`.

## Running Tests

### Quick Commands (via Makefile)

```bash
make test                      # Run all tests
make test-jvm                  # JVM-only (fastest)
make test-android              # Android tests
make test-integration          # Integration tests
make test-plugin-integration   # Plugin integration tests
make format                    # Format with ktlint
make lint                      # Lint code
make coverage                  # Generate coverage reports
```

### Docker Multi-Platform Testing

Test Linux builds locally before pushing to CI (requires Docker):

```bash
make docker-build-linux        # Build Linux x86-64 Docker image (one-time)
make docker-test-linux         # Run tests in Linux container
make docker-shell-linux        # Open shell for debugging
```

See [Docker Local Testing Guide](docs/docker-local-testing.md) for complete documentation.

### Direct Gradle Commands

Always prefix with `JAVA_HOME=/Users/garry.jeromson/Library/Java/JavaVirtualMachines/temurin-21.0.4/Contents/Home`:

```bash
./gradlew :junit-airgap:jvmTest                    # JVM tests
./gradlew :junit-airgap:testDebugUnitTest          # Android tests
./gradlew :plugin-integration-tests:jvm-junit4:test  # Specific integration test
```

### Bootstrap Behavior (Fresh Clones)

On fresh clones, `make test` automatically publishes the plugin to Maven Local (required for integration tests). Manual bootstrap: `make bootstrap` or `./gradlew :gradle-plugin:publishToMavenLocal`.

## Plugin Development Workflow

After modifying the plugin, publish to Maven Local before integration tests: `./gradlew :gradle-plugin:publishToMavenLocal`, then `make test-plugin-integration`.

### Plugin Architecture

- **JUnit 5**: Extension discovery via `junit-platform.properties`
- **JUnit 4**: ByteBuddy injects `@Rule AirgapRule` into test classes at execution time
  - **Known Issue**: Android/KMP task wiring challenges

## Code Coverage

Uses Kover for `:junit-airgap` and `:gradle-plugin` modules. Commands: `make coverage` or `make coverage-report` (opens in browser). Reports: `{module}/build/reports/kover/html/index.html` and `report.xml`.

## Code Quality

Format before committing: `make format` (or `./gradlew ktlintFormat`). Check: `make lint`.

## Network Blocking Implementation

### JVMTI Agent (JVM and Android)

C++ JVMTI agent intercepts socket/DNS at native level. Works on any Java version (JVMTI is version-independent). Agent packaged in JAR, extracted at runtime, loaded via JVM attach API. Throws `NetworkRequestAttemptedException` on unauthorized connections.

**Native code**: `native/src/` - `agent.cpp`, `socket_interceptor.cpp`, `dns_interceptor.cpp`

## Debug Logging

Enable: `./gradlew test -Djunit.airgap.debug=true`

**Location**: `junit-airgap/src/jvmMain/kotlin/.../DebugLogger.kt` - Zero overhead when disabled (lazy evaluation), testable via `TestDebugLogger`. See `DebugLoggerTest.kt` for examples.

## Test Contracts Module

Provides `assertRequestBlocked {}` and `assertRequestAllowed {}` helpers used by all integration test projects. Client-agnostic (Socket, Ktor, Retrofit, etc.), handles platform-specific exception wrapping. See `plugin-integration-tests/test-contracts/README.md`.

## Common Issues

- **KMP task wiring**: Known issue - injection tasks registered but not auto-wired for Android/KMP
- **Plugin changes not visible**: Publish to Maven Local: `./gradlew :gradle-plugin:publishToMavenLocal`
- **"Unsupported class file major version"**: Ensure `JAVA_HOME` points to Java 21
- **JVMTI agent errors**: Check temp dir extraction, file permissions, macOS Gatekeeper


## Key Architecture Decisions

1. **JVMTI for network blocking**: Native agent intercepts socket/DNS at the lowest level, works on any Java version (version-independent)
2. **ByteBuddy for JUnit 4**: Enables zero-configuration by injecting `@Rule` fields at compile time
3. **Execution-time classpath resolution**: Injection task resolves Test task classpath when it runs, not during Gradle configuration
4. **Single implementation for JVM/Android**: JVMTI agent works identically on both platforms
5. **Gradle Plugin separate from library**: Allows users to use the library directly without the plugin

## Testing Philosophy

Unit tests for core logic → Integration tests for library behavior → Plugin integration tests for different project types. Use **declarative** test names (e.g., `` `blocks HTTP requests` ``, not `` `should block HTTP requests` ``).
