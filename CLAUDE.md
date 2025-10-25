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
- SecurityManager requires `-Djava.security.manager=allow` on Java 21+
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
- `jvm-junit4` - Pure JVM with JUnit 4
- `jvm-junit5` - Pure JVM with JUnit 5
- `android-robolectric` - Android library with JUnit 4 + Robolectric
- `kmp-junit4` - KMP (JVM + Android) with JUnit 4
- `kmp-junit5` - KMP (JVM + Android) with JUnit 5
- `kmp-kotlintest` - KMP (JVM + Android) with kotlin.test + JUnit 4 runtime
- `kmp-kotlintest-junit5` - KMP (JVM + Android) with kotlin.test + JUnit 5 runtime

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

The library uses different strategies for blocking network access:

### JVM Implementation

Three implementations (tried in order):
1. **SECURITY_POLICY** (Java 21+) - Custom SecurityManager + Policy
2. **SECURITY_MANAGER** (Java 17-20) - Custom SecurityManager only
3. **SOCKET_WRAPPER** (fallback) - Socket factory replacement

**Java 21+ Requirement**: SecurityManager needs JVM arg `-Djava.security.manager=allow`

### Android Implementation

- Uses socket factory replacement
- No SecurityManager available on Android

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

1. **ByteBuddy for JUnit 4**: Enables zero-configuration by injecting `@Rule` fields at compile time
2. **Execution-time classpath resolution**: Injection task resolves Test task classpath when it runs, not during Gradle configuration
3. **SecurityManager vs Socket Wrapper**: Multiple strategies ensure compatibility across Java versions
4. **Gradle Plugin separate from library**: Allows users to use the library directly without the plugin

## Testing Philosophy

- **Unit tests** for core logic (fast feedback)
- **Integration tests** for library behavior in real projects
- **Plugin integration tests** for different project types
- **Benchmark tests** for performance tracking (informational only)

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
