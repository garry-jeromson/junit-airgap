# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0-beta.1] - 2025-10-28

### Added
- Initial beta release of junit-no-network extension and Gradle plugin
- JUnit 5 support via `@BlockNetworkRequests` and `@AllowNetworkRequests` annotations
- JUnit 4 support via `AirgapRule` and bytecode-injected `@Rule` fields
- Gradle plugin with zero-configuration setup
- JVMTI-based network blocking for JVM and Android (Robolectric) platforms
- Configuration options:
  - `enabled`: Enable/disable network blocking
  - `applyToAllTests`: Block network by default for all tests
  - `allowedHosts`: Whitelist specific hosts (supports wildcards)
  - `blockedHosts`: Blacklist specific hosts (supports wildcards)
  - `injectJUnit4Rule`: Automatic `@Rule` injection for JUnit 4
  - `debug`: Enable debug logging
- Kotlin Multiplatform support (JVM, Android, iOS API compatibility)
- Comprehensive HTTP client support:
  - Java standard library (`Socket`, `HttpURLConnection`, `HttpClient`)
  - OkHttp
  - Retrofit
  - Ktor Client (CIO, OkHttp, Java engines)
  - Apache HttpClient 5
  - Async HTTP Client
  - Reactor Netty (Spring WebFlux)
  - Spring WebClient
  - OpenFeign
  - Fuel
  - Android Volley

### Known Issues
- KMP projects: Automatic task wiring for JUnit 4 bytecode injection not yet working for Android/KMP configurations
- iOS: API compatibility only, no active network blocking (Kotlin/Native limitation)
- macOS only: Native JVMTI agent currently built for macOS only (Linux and Windows support planned)

### Technical Details
- Requires Java 21+ for JVMTI agent support
- Uses native agent (`.dylib`/`.so`/`.dll`) for low-level socket and DNS interception
- Test-first development approach with extensive integration test coverage
- Zero external dependencies for core library (JUnit only)

## [Unreleased]

### Planned
- Linux and Windows native agent builds
- Additional HTTP client support as requested
- Performance optimizations
- Enhanced error messages and diagnostics
