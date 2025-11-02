# macOS CI Test Failures Investigation

## Summary

macOS CI tests were failing with `NoRouteToHostException` caused by the JVMTI agent blocking Gradle's Maven artifact fetching during test execution.

## Root Cause

The JVMTI agent intercepts ALL socket connections via `sun.nio.ch.Net.connect0()` at the JVM-native boundary. While this design is intentional for comprehensive network blocking, it has an unintended side effect:

**The agent blocks Gradle's own HTTP connections when fetching Maven dependencies during test execution.**

### Why Tests Pass Locally

On local development machines, all test dependencies are already cached in:
- `~/.gradle/caches/` - Gradle's dependency cache
- `~/.m2/repository/` - Maven Local repository

When tests run locally, no network fetching occurs, so the JVMTI agent never interferes with Gradle's HTTP client.

### Why Tests Fail on CI

GitHub Actions runners start with empty caches. When tests execute:

1. Test task starts with `-agentpath` loading the JVMTI agent
2. JVMTI agent intercepts all `Net.connect0()` calls
3. Test runs with `@BlockNetworkRequests`, setting `NetworkBlockerContext` configuration
4. Gradle needs to fetch a Maven artifact (lazy dependency resolution)
5. Gradle's HTTP client calls `Net.connect0()`
6. JVMTI agent intercepts and checks `NetworkBlockerContext.getConfiguration()`
7. **Returns active configuration due to `globalConfiguration` volatile field** (designed for test worker threads)
8. Agent blocks the connection → returns `-2` error code
9. JVM interprets `-2` as network error → throws `NoRouteToHostException`
10. Test fails with: `java.lang.AssertionError at MavenArtifactFetcher.java:129`

## Error Evidence

```
KtorClientTest > ktorClientIsBlockedWithNoNetworkTest FAILED
    java.lang.AssertionError at MavenArtifactFetcher.java:129
        Caused by: java.util.concurrent.ExecutionException at AbstractFuture.java:588
            Caused by: java.net.NoRouteToHostException at Net.java:-2
```

The `-2` in `Net.java:-2` is the error code returned by our wrapped `connect0()` function, confirming the JVMTI agent is the source of the block.

## Architectural Issue: `globalConfiguration` Scope

The root architectural problem is in `NetworkBlockerContext.kt`:

```kotlin
object NetworkBlockerContext {
    @Volatile
    private var globalConfiguration: NetworkConfiguration? = null

    private val configurationThreadLocal = InheritableThreadLocal<NetworkConfiguration?>()

    fun setConfiguration(configuration: NetworkConfiguration) {
        configuration.generation = currentGeneration
        globalConfiguration = configuration  // ← PROBLEM: Visible to ALL threads
        configurationThreadLocal.set(configuration)
    }

    fun getConfiguration(): NetworkConfiguration? {
        val config = configurationThreadLocal.get()

        if (config != null && config.generation == currentGeneration) {
            return config  // Thread-local (correct for test threads)
        }

        // Fall back to global (catches Gradle worker threads!)
        return globalConfiguration
    }
}
```

**The `globalConfiguration` field was designed to handle HTTP client worker threads spawned BY tests** (e.g., OkHttp connection pools, Ktor coroutines). However, it inadvertently catches **Gradle's own worker threads** too, since they also don't have a thread-local configuration.

## Solution Implemented (Current)

**Detect and exclude Gradle worker threads from network blocking.**

Implementation in `NetworkBlockerContext.kt`:

```kotlin
fun getConfiguration(): NetworkConfiguration? {
    val config = configurationThreadLocal.get()
    val threadName = Thread.currentThread().name

    // If we have a config and it matches current generation, use it
    if (config != null && config.generation == currentGeneration) {
        return config
    }

    // Don't use global configuration for Gradle worker threads
    if (isGradleWorkerThread(threadName)) {
        return null
    }

    // Otherwise, use the global configuration (for HTTP client worker threads)
    return globalConfiguration
}

private fun isGradleWorkerThread(threadName: String): Boolean {
    return threadName.startsWith("Execution worker") ||
        threadName.startsWith("daemon worker") ||
        threadName.contains("Gradle") ||
        threadName.contains("worker-")
}
```

This approach detects Gradle's worker threads by their naming patterns and excludes them from inheriting the global configuration, while still allowing HTTP client worker threads (spawned by tests) to inherit the blocking configuration.

### Benefits of This Approach

- ✅ Simple, minimal code change (~15 lines)
- ✅ No breaking changes
- ✅ Works on both CI and local environments
- ✅ No performance overhead
- ✅ Fixes the root cause for Gradle threads specifically

### Limitations

- ⚠️ Relies on Gradle thread naming conventions (may break if Gradle changes naming)
- ⚠️ Doesn't fully fix the underlying architectural issue (globalConfiguration still exists)
- ⚠️ May need adjustment if other build tools have similar issues

## Proper Long-Term Fix

**Remove the `globalConfiguration` field entirely** and rely solely on `InheritableThreadLocal`.

### Proposed Architecture

```kotlin
object NetworkBlockerContext {
    private val configurationThreadLocal = InheritableThreadLocal<TimestampedConfig>()

    @Volatile
    private var currentGeneration = 0L

    fun setConfiguration(configuration: NetworkConfiguration) {
        val timestamped = TimestampedConfig(configuration, currentGeneration)
        configurationThreadLocal.set(timestamped)
    }

    fun getConfiguration(): NetworkConfiguration? {
        val timestamped = configurationThreadLocal.get() ?: return null

        // Only return if generation matches (prevents stale inheritance)
        if (timestamped.generation == currentGeneration) {
            return timestamped.config
        }

        return null  // DON'T fall back to global
    }
}
```

### Impact of This Change

**Breaking changes:**
- HTTP client worker threads spawned BEFORE test starts will NOT be blocked
- Tests relying on pre-spawned connection pools may need updates
- Users may need to ensure HTTP clients are initialized AFTER `@BlockNetworkRequests` takes effect

**Benefits:**
- ✅ Architecturally correct: Only blocks test thread and its children
- ✅ No false positives (Gradle threads won't inherit configuration)
- ✅ Simpler mental model (pure thread-local scoping)
- ✅ Eliminates entire class of bugs related to global state

### Migration Strategy

1. Document the behavior change clearly
2. Add `@AllowWorkerThreads` annotation for backward compatibility (if needed)
3. Provide migration guide with examples
4. Consider feature flag for gradual rollout: `junit.airgap.strictThreadScoping=true`

## Timeline

- **v0.x.x** (Current): Temporary workaround (pre-resolve dependencies)
- **v0.y.0** (Next minor): Proper architectural fix (remove `globalConfiguration`)
- **v1.0.0**: Stabilize with architectural fix as default behavior

## References

- GitHub Actions macOS runners: https://docs.github.com/en/actions/using-github-hosted-runners/about-github-hosted-runners
- JVMTI specification: https://docs.oracle.com/en/java/javase/21/docs/specs/jvmti.html
- InheritableThreadLocal: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/InheritableThreadLocal.html

## Related Issues

- CI failures on macOS: [Issue reference TBD]
- Ubuntu JNI header issue: Resolved in commit 8dc0031
