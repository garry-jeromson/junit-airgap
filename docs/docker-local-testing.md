# Docker Local Multi-Platform Testing

This guide explains how to use Docker to test Linux builds locally before pushing to CI, enabling faster feedback loops and catching platform-specific issues early.

## Prerequisites

1. **Docker Desktop** (macOS/Windows) or **Docker Engine** (Linux)
   - macOS: [Download Docker Desktop](https://www.docker.com/products/docker-desktop)
   - Linux: [Install Docker Engine](https://docs.docker.com/engine/install/)
   - Minimum version: Docker 20.10+ with Compose V2

2. **Disk Space**
   - ~5-10GB for Docker images and build caches
   - Additional space for test results and artifacts

## Quick Start

### 1. Build the Docker Image (One-Time Setup)

```bash
make docker-build-linux
```

This builds the Linux x86-64 Docker image with:
- Ubuntu 24.04
- Java 21 (OpenJDK)
- CMake and C++ build tools
- Gradle wrapper

**First build takes ~5-10 minutes** to download base images and dependencies.

### 2. Run Tests in Docker

```bash
make docker-test-linux
```

This runs the complete test suite inside the Linux container:
- Builds JVMTI native agent for Linux
- Compiles Kotlin code
- Runs all tests (unit + integration)

**Subsequent runs are faster (~30 seconds)** thanks to incremental builds and caching.

## Available Commands

### Build Commands

```bash
# Build Linux x86-64 image
make docker-build-linux

# Build all platform images (future: will include ARM64, Windows)
make docker-build-all
```

### Test Commands

```bash
# Run tests on Linux x86-64
make docker-test-linux

# Run tests on all platforms (future: multiple platforms)
make docker-test-all
```

### Interactive Debugging

```bash
# Open shell in Linux container for debugging
make docker-shell-linux

# Inside the container, you can:
./gradlew test                    # Run tests
./gradlew :junit-airgap:jvmTest   # Run specific test suite
make build-native                  # Build native agent
ls -la native/build/              # Check native artifacts
```

### Cleanup Commands

```bash
# Remove containers and volumes (keeps images)
make docker-clean

# Remove everything (containers, volumes, images)
make docker-clean-all
```

## How It Works

### Volume Mounting Strategy

The Docker setup uses a hybrid volume mounting approach for optimal performance:

**Read-Only Source Mount:**
```
./:/workspace:ro
```
- Entire source tree mounted read-only
- Prevents container from accidentally modifying host files
- Changes on host immediately visible in container

**Read-Write Build Mounts:**
```
./build:/workspace/build
./native/build:/workspace/native/build
./junit-airgap/build:/workspace/junit-airgap/build
...
```
- Build outputs mounted read-write
- Enables incremental builds across container runs
- First build is slow, subsequent builds are fast

**Persistent Cache Volumes:**
```
gradle-cache-linux-x86:/root/.gradle
maven-local-linux-x86:/root/.m2
```
- Named Docker volumes for Gradle cache and Maven Local
- Persists across container runs
- Separate from host to avoid native library conflicts

### Build Process

When you run `make docker-test-linux`, Docker:

1. **Starts container** from pre-built image
2. **Mounts volumes** (source, builds, caches)
3. **Runs command**: `./gradlew clean test --no-daemon`
   - Builds native agent for Linux (.so file)
   - Compiles Kotlin code
   - Publishes plugin to Maven Local (in container)
   - Runs all tests
4. **Exits** and removes container (keeps volumes)

## Developer Workflows

### Workflow 1: Pre-Push Validation

Test your changes on Linux before pushing:

```bash
# Make code changes on macOS
vim junit-airgap/src/jvmMain/kotlin/...

# Test locally on macOS
make test

# Test on Linux (in Docker)
make docker-test-linux

# If tests pass, push
git push
```

### Workflow 2: Debugging Linux-Specific Issues

When tests fail only on Linux CI:

```bash
# Open shell in Linux container
make docker-shell-linux

# Inside container, run failing test
./gradlew :junit-airgap:jvmTest --tests "SpecificTest" --info

# Check native agent
ls -la native/build/
file native/build/libjunit-airgap-agent.so

# Enable debug logging
./gradlew test -Djunit.airgap.debug=true

# Exit when done
exit
```

### Workflow 3: Quick Iteration

For fast iteration on Linux-specific code:

```bash
# Edit code on host
vim native/src/agent.cpp

# Test immediately (uses incremental build)
make docker-test-linux

# Repeat as needed (each run ~30 seconds)
```

### Workflow 4: Clean Rebuild

When you need to start fresh:

```bash
# Remove all caches and rebuild from scratch
make docker-clean-all
make docker-build-linux
make docker-test-linux
```

## Performance Characteristics

| Operation | First Time | Subsequent Runs |
|-----------|-----------|-----------------|
| Build Docker image | 5-10 min | Instant (cached) |
| Run all tests | 3-5 min | 30-60 sec |
| Incremental test | N/A | 10-30 sec |

**Why subsequent runs are faster:**
- Gradle incremental compilation
- Persistent build outputs
- Cached dependencies
- Native agent only rebuilds when CMake files or C++ code changes

## Troubleshooting

### Docker Daemon Not Running

**Error:**
```
Cannot connect to the Docker daemon at unix:///var/run/docker.sock
```

**Solution:**
```bash
# macOS: Start Docker Desktop application
open -a Docker

# Linux: Start Docker service
sudo systemctl start docker
```

### Out of Disk Space

**Error:**
```
Error response from daemon: no space left on device
```

**Solution:**
```bash
# Clean up unused Docker resources
make docker-clean-all

# Or use Docker's prune command
docker system prune -a --volumes
```

### Tests Fail in Docker But Pass Locally

**Common causes:**
1. **Platform-specific code** - Check native agent behavior
2. **File path differences** - Verify absolute paths aren't hardcoded
3. **Permission issues** - Ensure test files aren't created as root

**Debug steps:**
```bash
# Open shell and investigate
make docker-shell-linux

# Run tests with debug output
./gradlew test --info -Djunit.airgap.debug=true

# Check file permissions
ls -la native/build/
```

### Gradle Daemon Issues

The Docker setup disables the Gradle daemon (`--no-daemon`) to avoid:
- Stale daemon processes in containers
- Memory overhead for long-running containers
- Configuration cache conflicts

This is normal and expected in containerized environments.

### Slow First Build

**Expected behavior:** First build takes 5-10 minutes to:
- Download Ubuntu base image (~200MB)
- Install Java 21 and build tools
- Download Gradle dependencies
- Build native agent
- Run all tests

**Subsequent builds are much faster** (~30 seconds) thanks to Docker layer caching and incremental builds.

## Advanced Configuration

### Custom Docker Compose Overrides

Create `docker-compose.override.yml` for local customizations:

```yaml
version: '3.8'

services:
  linux-x86-64:
    environment:
      # Enable debug logging
      - GRADLE_OPTS=-Dorg.gradle.daemon=false -Djunit.airgap.debug=true
    command: bash -c "
      chmod +x ./gradlew &&
      ./gradlew :junit-airgap:jvmTest --info
    "
```

### Running Specific Tests

```bash
# Run specific test class
docker compose run --rm linux-x86-64 bash -c "
  chmod +x ./gradlew &&
  ./gradlew :junit-airgap:jvmTest --tests 'DebugLoggerTest'
"

# Run with additional flags
docker compose run --rm linux-x86-64 bash -c "
  chmod +x ./gradlew &&
  ./gradlew test --info --stacktrace
"
```

### Extracting Test Reports

```bash
# Run tests
make docker-test-linux

# Reports are written to host volumes (mounted)
open junit-airgap/build/reports/tests/jvmTest/index.html
```

## Comparison: Docker vs CI

| Aspect | Local Docker | GitHub Actions CI |
|--------|-------------|-------------------|
| **Feedback Speed** | 30 sec - 5 min | 5-10 min (queue + run) |
| **Iteration** | Instant reruns | Requires git push |
| **Debugging** | Interactive shell | Logs only |
| **Cost** | Free (local CPU) | Free (GitHub quota) |
| **Platform Coverage** | Linux only (macOS host) | Linux + Windows + macOS |
| **Java Versions** | Single version | Matrix (21, 25) |

**Recommended approach:**
- Use **Docker** for rapid iteration and debugging
- Use **CI** for comprehensive testing (multiple platforms, Java versions)

## Future Enhancements

Planned additions to Docker setup:

- **Linux ARM64** - Test on ARM64 via QEMU emulation
- **Windows** - Native Windows containers (Windows host only) or Wine cross-compilation
- **Helper scripts** - Automated result extraction and reporting
- **Multi-platform testing** - Run all platforms in parallel

## See Also

- [Compatibility Matrix](compatibility-matrix.md) - Platform support details
- [CLAUDE.md](../CLAUDE.md) - Project development guidelines
- [Makefile](../Makefile) - All available make commands
- [GitHub Actions CI](.github/workflows/ci.yml) - CI configuration
