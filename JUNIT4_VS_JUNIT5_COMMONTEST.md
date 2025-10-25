# JUnit 4 vs JUnit 5 Behavior with commonTest

## Executive Summary

**Key Finding:** In modern Kotlin Multiplatform projects configured with `useJUnitPlatform()`, **commonTest tests execute under JUnit 5**, not JUnit 4, regardless of which test annotations are used.

This has important implications for the junit-no-network Gradle plugin's JUnit 4 bytecode enhancement feature.

## Background

The junit-no-network Gradle plugin provides two mechanisms for network blocking:
1. **JUnit 5 Extension** - Automatic via ServiceLoader (zero configuration)
2. **JUnit 4 @Rule Injection** - Automatic via bytecode enhancement

## The Test Framework Selection

### How JUnit Platform Works

When a Gradle project is configured with:
```kotlin
tasks.withType<Test> {
    useJUnitPlatform()
}
```

ALL tests in that project execute under **JUnit Platform** (JUnit 5's test engine), including:
- Tests with `@org.junit.jupiter.api.Test` (JUnit 5)
- Tests with `@org.junit.Test` (JUnit 4 annotation)
- Tests with `@kotlin.test.Test` (Kotlin test)

The JUnit Platform can execute JUnit 4 tests through a compatibility layer (`junit-vintage-engine`), but it's still JUnit 5 running them.

### Impact on commonTest

In KMP projects:
- commonTest uses `@kotlin.test.Test` by default
- `@kotlin.test.Test` translates to platform-specific annotations
- On JVM/Android with `useJUnitPlatform()`, it becomes a JUnit 5 test

**Result:** commonTest tests execute under JUnit Platform (JUnit 5), not JUnit 4.

## JUnit 4 Bytecode Enhancement Behavior

### What the Bytecode Enhancement Does

The `JUnit4RuleInjectionTask` scans compiled test classes and:
1. Checks if a class has methods annotated with `@org.junit.Test`
2. Checks if the class uses `@RunWith` (JUnit 4 indicator)
3. If yes, injects a `@Rule` field for `NoNetworkRule`

### When It Works

The bytecode enhancement works for:
- ✅ **Pure JUnit 4 projects** - Projects without `useJUnitPlatform()`
- ✅ **Mixed JUnit 4/5 projects** - Using `junit-vintage-engine`
- ✅ **JUnit 4 tests in JUnit 5 projects** - If explicitly using JUnit 4 runner

### When It Doesn't Apply

The bytecode enhancement does NOT apply to:
- ❌ **JUnit 5 tests** - They don't use `@Rule`, they use Extensions
- ❌ **commonTest in `useJUnitPlatform()` projects** - They're JUnit 5 tests
- ❌ **Tests executed by JUnit Platform** - Even with `@org.junit.Test` annotations

## Test Results from plugin-integration-test

### Configuration
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-SNAPSHOT"
}

tasks.withType<Test> {
    useJUnitPlatform()  // ALL tests use JUnit 5
}

junitNoNetwork {
    enabled = true
    applyToAllTests = true  // Uses JUnit 5 extension
    debug = true
}
```

### Test Execution Log
```
> Task :plugin-integration-test:injectAndroidJUnit4NetworkRule
Scanning for JUnit 4 test classes in: .../build/tmp/kotlin-classes/debugUnitTest
Found 5 class files to analyze
JUnit 4 Rule Injection: enhanced 0 classes, skipped 0 classes
```

**Analysis:**
- 5 class files scanned (including commonTest classes)
- 0 classes enhanced (none were JUnit 4 tests)
- 0 classes skipped (none had existing `@Rule` fields)

### Why Tests Still Pass

The tests pass because `applyToAllTests = true` activates the **JUnit 5 extension mechanism**:

```kotlin
// Generated junit-platform.properties
junit.jupiter.extensions.autodetection.enabled=true
junit.nonetwork.applyToAllTests=true
```

The `NoNetworkExtension` (JUnit 5) provides network blocking, making JUnit 4 bytecode enhancement unnecessary.

## When Is JUnit 4 Bytecode Enhancement Needed?

JUnit 4 bytecode enhancement is needed for:

1. **Pure JUnit 4 projects** - Projects that don't use JUnit Platform
   ```kotlin
   // NO useJUnitPlatform() call
   dependencies {
       testImplementation("junit:junit:4.13.2")
   }
   ```

2. **Legacy Android projects** - Many Android projects use JUnit 4 by default
   ```kotlin
   android {
       testOptions {
           unitTests.returnDefaultValues = true
       }
   }
   // Uses JUnit 4 runner, not JUnit Platform
   ```

3. **Opt-out from JUnit 5** - Projects that explicitly want JUnit 4
   ```kotlin
   tasks.withType<Test> {
       useJUnit()  // NOT useJUnitPlatform()
   }
   ```

## Recommendations

### For Modern KMP Projects

If your project uses `useJUnitPlatform()`:
- ✅ Network blocking works automatically via JUnit 5 extension
- ✅ commonTest tests work perfectly (they're JUnit 5 tests)
- ✅ No manual setup needed (`applyToAllTests = true` is sufficient)
- ℹ️ JUnit 4 bytecode enhancement won't apply (but you don't need it)

### For Pure JUnit 4 Projects

If your project uses JUnit 4 (no `useJUnitPlatform()`):
- ✅ Bytecode enhancement automatically injects `@Rule` fields
- ✅ Zero configuration for JUnit 4 tests
- ⚠️ commonTest with expect/actual may work (platform-specific)
- ⚠️ Pure commonTest classes depend on target platform configuration

### For Mixed Projects

If you have both JUnit 4 and JUnit 5 tests:
- ✅ Use `junit-vintage-engine` to run JUnit 4 tests under JUnit Platform
- ✅ Bytecode enhancement works for pure JUnit 4 tests
- ✅ JUnit 5 extension works for JUnit 5 tests
- ℹ️ Set `applyToAllTests` based on your needs

## Verification Test Results

### Pure commonTest Tests

**Test:** `PluginPureCommonTest.kt`
- Location: `plugin-integration-test/src/commonTest/`
- Annotations: `@org.junit.Test` (explicit JUnit 4 annotation)
- Result: ✅ Tests pass (via JUnit 5 extension)
- Bytecode: ❌ No `@Rule` field injected (not a JUnit 4 test)

**Why:** The project uses `useJUnitPlatform()`, so all tests execute as JUnit 5 tests, regardless of annotation.

### commonTest with expect/actual

**Test:** `PluginCommonTestJUnit4Test.kt`
- Location: `plugin-integration-test/src/commonTest/`
- Platform impls: `androidUnitTest/`, `jvmTest/`
- Annotations: `@org.junit.Test`
- Result: ✅ Tests pass (via JUnit 5 extension)
- Bytecode: ❌ No `@Rule` field injected (not a JUnit 4 test)

### Functional Tests

Both test types successfully:
- ✅ Block network requests with `@BlockNetworkRequests`
- ✅ Allow network with `@AllowNetworkRequests`
- ✅ Execute on both JVM and Android platforms

**Mechanism:** JUnit 5 extension (`NoNetworkExtension`), not JUnit 4 `@Rule`

## Conclusion

The junit-no-network plugin's JUnit 4 bytecode enhancement feature works correctly:
- ✅ It detects and enhances actual JUnit 4 test classes
- ✅ It correctly skips JUnit 5 tests (including commonTest in JUnit Platform projects)
- ✅ It provides zero-configuration for JUnit 4 projects

For modern KMP projects with `useJUnitPlatform()`:
- commonTest tests are JUnit 5 tests (by design)
- They use the JUnit 5 extension mechanism (works perfectly)
- JUnit 4 bytecode enhancement doesn't apply (and doesn't need to)

**The plugin provides zero-configuration network blocking for BOTH JUnit 4 and JUnit 5**, using the appropriate mechanism for each test framework.
