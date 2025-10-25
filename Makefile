.PHONY: help build clean test test-jvm test-android test-integration test-socketimpl test-jvm-socketimpl test-integration-socketimpl test-integration-jvmti test-plugin-integration benchmark benchmark-jvm benchmark-android format lint check fix install publish jar sources-jar all verify setup-native build-native test-native clean-native

# Default Java version for the project (uses Java 17 toolchain internally)
JAVA_VERSION ?= 21

# Detect Java home for the required version
JAVA_HOME := $(shell /usr/libexec/java_home -v $(JAVA_VERSION) 2>/dev/null || echo "")

# Gradle wrapper
GRADLEW := ./gradlew

# Default target
.DEFAULT_GOAL := help

## help: Display this help message
help:
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  JUnit No-Network Extension - Development Commands"
	@echo "═══════════════════════════════════════════════════════════════"
	@echo ""
	@echo "Usage: make <target>"
	@echo ""
	@echo "Build Commands:"
	@echo "  build              Build the entire project (compile + test)"
	@echo "  clean              Clean build artifacts"
	@echo "  jar                Build JVM JAR file"
	@echo "  sources-jar        Build sources JAR files"
	@echo "  assemble           Assemble all outputs without running tests"
	@echo ""
	@echo "Test Commands:"
	@echo "  test                       Run all tests (JVM + Android + Integration)"
	@echo "  test-jvm                   Run JVM unit tests only"
	@echo "  test-android               Run Android unit tests (Robolectric)"
	@echo "  test-integration           Run JVM integration tests (junit-no-network module)"
	@echo "  test-plugin-integration    Run all plugin integration tests (KMP/Android/JVM × JUnit4/5)"
	@echo "  verify                     Run all tests and checks (test + lint + plugin tests)"
	@echo ""
	@echo "Test Commands (SocketImplFactory Implementation):"
	@echo "  test-socketimpl            Run all tests using SocketImplFactory (Java 24+ compatible)"
	@echo "  test-jvm-socketimpl        Run JVM tests using SocketImplFactory"
	@echo "  test-integration-socketimpl Run integration tests using SocketImplFactory"
	@echo ""
	@echo "Native Agent Commands (JVMTI Implementation):"
	@echo "  setup-native            Install native build dependencies (CMake)"
	@echo "  build-native            Build JVMTI native agent (.dylib/.so/.dll)"
	@echo "  test-native             Run native agent tests (AgentLoadTest, SocketInterceptTest)"
	@echo "  test-integration-jvmti  Run integration tests using ONLY JVMTI (no SecurityManager)"
	@echo "  clean-native            Clean native build artifacts"
	@echo ""
	@echo "Performance Benchmark Commands:"
	@echo "  benchmark          Run all performance benchmarks (JVM + Android)"
	@echo "  benchmark-jvm      Run JVM performance benchmarks only"
	@echo "  benchmark-android  Run Android performance benchmarks only"
	@echo ""
	@echo "Code Quality Commands:"
	@echo "  format             Auto-format code with ktlint"
	@echo "  lint               Check code style with ktlint"
	@echo "  check              Run all checks (lint + tests)"
	@echo "  fix                Auto-fix formatting and lint issues"
	@echo ""
	@echo "Publishing Commands:"
	@echo "  install            Install to local Maven repository"
	@echo "  publish            Publish to configured repository"
	@echo ""
	@echo "Utility Commands:"
	@echo "  tasks              List all available Gradle tasks"
	@echo "  deps               Show dependency tree"
	@echo "  wrapper            Update Gradle wrapper"
	@echo "  all                Clean, build, format, and verify everything"
	@echo ""
	@echo "Environment:"
	@echo "  JAVA_HOME          $(JAVA_HOME)"
	@echo "  Java Version       $(JAVA_VERSION)"
	@echo ""

## build: Build the entire project (compile and run tests)
build:
	@echo "Building project..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) build

## clean: Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) clean

## test: Run all tests (JVM + Android + Integration + Plugin Integration)
test:
	@echo "Running all tests..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) test integrationTest

## test-jvm: Run JVM unit tests only
test-jvm:
	@echo "Running JVM tests..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) jvmTest

## test-android: Run Android unit tests (Robolectric)
test-android:
	@echo "Running Android tests..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) testDebugUnitTest testReleaseUnitTest

## test-integration: Run JVM integration tests (junit-no-network module)
test-integration:
	@echo "Running integration tests..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) integrationTest

## test-socketimpl: Run all tests using SocketImplFactory implementation (Java 24+ compatible)
test-socketimpl:
	@echo "Running all tests with SocketImplFactory implementation..."
	@echo "(Debug output enabled to verify implementation selection)"
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) test integrationTest \
		-Djunit.nonetwork.implementation=socketimplfactory \
		-Djunit.nonetwork.debug=true

## test-jvm-socketimpl: Run JVM tests using SocketImplFactory implementation
test-jvm-socketimpl:
	@echo "Running JVM tests with SocketImplFactory implementation..."
	@echo "(Debug output enabled to verify implementation selection)"
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) jvmTest \
		-Djunit.nonetwork.implementation=socketimplfactory \
		-Djunit.nonetwork.debug=true

## test-integration-socketimpl: Run integration tests using SocketImplFactory implementation
test-integration-socketimpl:
	@echo "Running integration tests with SocketImplFactory implementation..."
	@echo "(Debug output enabled to verify implementation selection)"
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) integrationTest \
		-Djunit.nonetwork.implementation=socketimplfactory \
		-Djunit.nonetwork.debug=true

## test-plugin-integration: Run all plugin integration tests across different configurations
test-plugin-integration:
	@echo "Publishing artifacts to mavenLocal..."
	@JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :junit-no-network:publishToMavenLocal :gradle-plugin:publishToMavenLocal
	@echo ""
	@echo "Running plugin integration tests..."
	@JAVA_HOME=$(JAVA_HOME) $(GRADLEW) \
		:plugin-integration-tests:kmp-junit5:test \
		:plugin-integration-tests:kmp-junit4:test \
		:plugin-integration-tests:kmp-kotlintest:test \
		:plugin-integration-tests:kmp-kotlintest-junit5:test \
		:plugin-integration-tests:android-robolectric:testDebugUnitTest \
		:plugin-integration-tests:jvm-junit5:test \
		:plugin-integration-tests:jvm-junit4:test

## benchmark: Run all performance benchmarks (JVM + Android)
benchmark:
	@echo "Running all performance benchmarks..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :benchmark:benchmark

## benchmark-jvm: Run JVM performance benchmarks only
benchmark-jvm:
	@echo "Running JVM performance benchmarks..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :benchmark:jvmTest

## benchmark-android: Run Android performance benchmarks only
benchmark-android:
	@echo "Running Android performance benchmarks..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :benchmark:testDebugUnitTest

## format: Auto-format code with ktlint
format:
	@echo "Formatting code with ktlint..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) ktlintFormat

## lint: Check code style with ktlint
lint:
	@echo "Checking code style with ktlint..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) ktlintCheck

## check: Run all checks (tests + linting)
check:
	@echo "Running all checks..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) check

## fix: Auto-fix formatting and lint issues
fix: format
	@echo "Code formatting applied!"

## verify: Run all tests and checks
verify: check test-integration test-plugin-integration
	@echo "All verification passed!"

## jar: Build JVM JAR file
jar:
	@echo "Building JVM JAR..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) jvmJar

## sources-jar: Build sources JAR files
sources-jar:
	@echo "Building sources JARs..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) jvmSourcesJar androidDebugSourcesJar androidReleaseSourcesJar

## assemble: Assemble all outputs without running tests
assemble:
	@echo "Assembling project..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) assemble

## install: Install to local Maven repository (~/.m2/repository)
install:
	@echo "Installing to local Maven repository..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) publishToMavenLocal

## publish: Publish to configured repository
publish:
	@echo "Publishing artifacts..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) publish

## tasks: List all available Gradle tasks
tasks:
	@echo "Available Gradle tasks:"
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) tasks

## deps: Show dependency tree
deps:
	@echo "Showing dependencies..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) dependencies

## wrapper: Update Gradle wrapper
wrapper:
	@echo "Updating Gradle wrapper..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) wrapper

## all: Clean, build, format, and verify everything
all: clean format build verify
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  ✅ All tasks completed successfully!"
	@echo "═══════════════════════════════════════════════════════════════"

# Utility target to check Java installation
check-java:
	@if [ -z "$(JAVA_HOME)" ]; then \
		echo "❌ Error: Java $(JAVA_VERSION) not found."; \
		echo "Please install Java $(JAVA_VERSION) or set JAVA_VERSION variable."; \
		exit 1; \
	else \
		echo "✅ Using Java from: $(JAVA_HOME)"; \
		$(JAVA_HOME)/bin/java -version; \
	fi

## Native Agent Targets (JVMTI Implementation)

# setup-native: Install native build dependencies (CMake)
setup-native:
	@echo "Checking for CMake installation..."
	@if ! command -v cmake >/dev/null 2>&1; then \
		echo "CMake not found. Installing via Homebrew..."; \
		if command -v brew >/dev/null 2>&1; then \
			brew install cmake; \
		else \
			echo "❌ Error: Homebrew not found. Please install Homebrew first:"; \
			echo "  /bin/bash -c \"\$$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""; \
			exit 1; \
		fi; \
	else \
		echo "✅ CMake already installed: $$(cmake --version | head -1)"; \
	fi

## build-native: Build JVMTI native agent
build-native: setup-native
	@echo "Building JVMTI native agent..."
	@cd native && mkdir -p build && cd build && cmake .. && $(MAKE)
	@echo "✅ Native agent built: native/build/libjunit-no-network-agent.dylib"

## test-native: Run native agent tests
test-native: build-native
	@echo "Running native agent tests..."
	@echo ""
	@echo "Test 1: AgentLoadTest (verify agent loads)"
	@echo "─────────────────────────────────────────────"
	@cd native/test && \
		$(JAVA_HOME)/bin/javac AgentLoadTest.java && \
		$(JAVA_HOME)/bin/java -agentpath:../build/libjunit-no-network-agent.dylib AgentLoadTest
	@echo ""
	@echo "Test 2: SocketInterceptTest (verify Socket interception)"
	@echo "─────────────────────────────────────────────────────────────"
	@cd native/test && \
		$(JAVA_HOME)/bin/javac SocketInterceptTest.java && \
		$(JAVA_HOME)/bin/java -agentpath:../build/libjunit-no-network-agent.dylib SocketInterceptTest
	@echo ""
	@echo "✅ All native tests passed!"

## test-integration-jvmti: Run integration tests using ONLY JVMTI (disables SecurityManager)
test-integration-jvmti: build-native
	@echo "Running integration tests with JVMTI agent (SecurityManager disabled)..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :junit-no-network:integrationTestJvmti

## clean-native: Clean native build artifacts
clean-native:
	@echo "Cleaning native build artifacts..."
	@rm -rf native/build
	@rm -f native/test/*.class
	@echo "✅ Native build cleaned"
