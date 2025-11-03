# Linux x86-64 build environment for junit-airgap with Android SDK
FROM ubuntu:24.04

# Prevent interactive prompts during package installation
ENV DEBIAN_FRONTEND=noninteractive

# Install build dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    # C++ toolchain for JVMTI agent
    build-essential \
    cmake \
    g++ \
    # Java 21 (OpenJDK) - required for Kotlin and JVMTI (full JDK for headers)
    openjdk-21-jdk \
    # Utilities
    curl \
    git \
    unzip \
    wget \
    ca-certificates \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*

# Set Java environment
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Install Android SDK (required for Robolectric tests)
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=${ANDROID_SDK_ROOT}
ENV PATH="${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools"

# Download and install Android command line tools
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    cd ${ANDROID_SDK_ROOT}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && \
    unzip commandlinetools-linux-11076708_latest.zip && \
    rm commandlinetools-linux-11076708_latest.zip && \
    mv cmdline-tools latest

# Accept licenses and install required SDK components
# Robolectric needs platform-tools and at least one platform
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

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
