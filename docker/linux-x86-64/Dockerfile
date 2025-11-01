# Linux x86-64 build environment for junit-airgap
FROM ubuntu:24.04

# Prevent interactive prompts during package installation
ENV DEBIAN_FRONTEND=noninteractive

# Install build dependencies
RUN apt-get update && apt-get install -y \
    # C++ toolchain for JVMTI agent
    build-essential \
    cmake \
    g++ \
    # Java 21 (OpenJDK) - required for Kotlin and JVMTI
    openjdk-21-jdk \
    # Utilities
    curl \
    git \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Set Java environment
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Create working directory
WORKDIR /workspace

# Copy gradle wrapper files for dependency caching layer
COPY gradle/ gradle/
COPY gradlew gradlew.bat gradle.properties settings.gradle.kts ./

# Make gradlew executable
RUN chmod +x ./gradlew

# Download Gradle dependencies (creates a cache layer)
# This layer only rebuilds when Gradle wrapper or dependencies change
RUN ./gradlew --version || true

# Default command: run all tests
CMD ["./gradlew", "test", "--no-daemon"]
