.PHONY: help build clean test test-java21 test-java25 benchmark format lint check fix install publish jar sources-jar all verify setup-native build-native test-native clean-native

# Default Java version for the project
JAVA_VERSION ?= 21

# Detect Java home for the required version
JAVA_HOME := $(shell /usr/libexec/java_home -v $(JAVA_VERSION) 2>/dev/null || echo "")

# Detect available Java versions for multi-version testing
JAVA_21_HOME := $(shell /usr/libexec/java_home -v 21 2>/dev/null || echo "")
JAVA_25_HOME := $(shell /usr/libexec/java_home -v 25 2>/dev/null || echo "")

# Gradle wrapper
GRADLEW := ./gradlew

# Default target
.DEFAULT_GOAL := help

## help: Display this help message
help:
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  JUnit Airgap Extension - Development Commands"
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
	@echo "  bootstrap      Bootstrap plugin to Maven Local (auto-runs on first test)"
	@echo "  test           Run all tests (default Java 21)"
	@echo "  test-java21    Run all tests with Java 21"
	@echo "  test-java25    Run all tests with Java 25"
	@echo "  verify         Run all tests and checks"
	@echo ""
	@echo "Native Agent Commands (JVMTI Implementation):"
	@echo "  setup-native            Install native build dependencies (CMake)"
	@echo "  build-native            Build JVMTI native agent (.dylib/.so/.dll)"
	@echo "  test-native             Run native agent tests (AgentLoadTest, SocketInterceptTest)"
	@echo "  clean-native            Clean native build artifacts"
	@echo ""
	@echo "Performance Benchmark Commands:"
	@echo "  benchmark      Run all performance benchmarks"
	@echo ""
	@echo "Code Quality Commands:"
	@echo "  format             Auto-format code with ktlint"
	@echo "  lint               Check code style with ktlint"
	@echo "  check              Run all checks (lint + tests)"
	@echo "  fix                Auto-fix formatting and lint issues"
	@echo ""
	@echo "Code Coverage Commands:"
	@echo "  coverage           Generate code coverage reports"
	@echo "  coverage-report    Open HTML coverage report in browser"
	@echo ""
	@echo "Publishing Commands:"
	@echo "  install            Install to local Maven repository (~/.m2/repository)"
	@echo "  publish            Publish to Maven Central (requires OSSRH credentials)"
	@echo "  publish-plugin     Publish Gradle plugin to Plugin Portal (requires credentials)"
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

## bootstrap: Bootstrap plugin to Maven Local (required before first test run)
bootstrap:
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  Bootstrapping plugin to Maven Local..."
	@echo "═══════════════════════════════════════════════════════════════"
	@JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :gradle-plugin:publishToMavenLocal --quiet || \
		JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :gradle-plugin:publishToMavenLocal
	@echo "✅ Plugin bootstrap complete"
	@echo ""

## test: Run all tests (Core library + Gradle plugin + Integration tests + Plugin integration tests)
test:
	@# Check if plugin exists in Maven Local (only check gradle-plugin marker)
	@if [ ! -d "$$HOME/.m2/repository/io/github/garryjeromson/junit-airgap-gradle-plugin" ]; then \
		echo "═══════════════════════════════════════════════════════════════"; \
		echo "  First-time setup: Publishing plugin to Maven Local..."; \
		echo "═══════════════════════════════════════════════════════════════"; \
		JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :gradle-plugin:publishToMavenLocal --quiet || \
			JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :gradle-plugin:publishToMavenLocal; \
		echo "✅ Plugin published successfully"; \
		echo ""; \
	fi
	@echo "Running all tests..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) test

## test-java21: Run all tests with Java 21
test-java21:
	@if [ -z "$(JAVA_21_HOME)" ]; then \
		echo "❌ Error: Java 21 not found."; \
		echo "Please install Java 21 or check your installation."; \
		exit 1; \
	fi
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  Running all tests with Java 21"
	@echo "  Java Home: $(JAVA_21_HOME)"
	@echo "═══════════════════════════════════════════════════════════════"
	@$(JAVA_21_HOME)/bin/java -version
	@echo ""
	JAVA_HOME=$(JAVA_21_HOME) $(GRADLEW) test

## test-java25: Run all tests with Java 25
test-java25:
	@if [ -z "$(JAVA_25_HOME)" ]; then \
		echo "❌ Error: Java 25 not found."; \
		echo "Please install Java 25 or check your installation."; \
		exit 1; \
	fi
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  Running all tests with Java 25"
	@echo "  Java Home: $(JAVA_25_HOME)"
	@echo "═══════════════════════════════════════════════════════════════"
	@$(JAVA_25_HOME)/bin/java -version
	@echo ""
	JAVA_HOME=$(JAVA_25_HOME) $(GRADLEW) test

## benchmark: Run performance benchmarks and compare control vs treatment
benchmark:
	@echo "Running performance benchmarks with comparison..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) compareBenchmarks

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

## coverage: Generate code coverage reports
coverage:
	@echo "Generating code coverage reports..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) koverHtmlReport koverXmlReport

## coverage-report: Open HTML coverage report in browser
coverage-report: coverage
	@echo "Opening coverage report..."
	@open junit-airgap/build/reports/kover/html/index.html || \
	 open gradle-plugin/build/reports/kover/html/index.html || \
	 echo "Coverage reports generated. Check:"
	@echo "  - junit-airgap/build/reports/kover/html/index.html"
	@echo "  - gradle-plugin/build/reports/kover/html/index.html"

## verify: Run all tests and checks
verify: check test
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

## publish: Publish to Maven Central
publish:
	@echo "Publishing artifacts to Maven Central..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) publishToSonatype closeAndReleaseSonatypeStagingRepository

## publish-plugin: Publish Gradle plugin to Plugin Portal
publish-plugin:
	@echo "Publishing Gradle plugin to Plugin Portal..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :gradle-plugin:publishPlugins

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
	@echo "✅ Native agent built: native/build/libjunit-airgap-agent.dylib"

## test-native: Run native agent tests
test-native: build-native
	@echo "Running native agent tests..."
	@echo ""
	@echo "Test 1: AgentLoadTest (verify agent loads)"
	@echo "─────────────────────────────────────────────"
	@cd native/test && \
		$(JAVA_HOME)/bin/javac AgentLoadTest.java && \
		$(JAVA_HOME)/bin/java -agentpath:../build/libjunit-airgap-agent.dylib AgentLoadTest
	@echo ""
	@echo "Test 2: SocketInterceptTest (verify Socket interception)"
	@echo "─────────────────────────────────────────────────────────────"
	@cd native/test && \
		$(JAVA_HOME)/bin/javac SocketInterceptTest.java && \
		$(JAVA_HOME)/bin/java -agentpath:../build/libjunit-airgap-agent.dylib SocketInterceptTest
	@echo ""
	@echo "✅ All native tests passed!"


## clean-native: Clean native build artifacts
clean-native:
	@echo "Cleaning native build artifacts..."
	@rm -rf native/build
	@rm -f native/test/*.class
	@echo "✅ Native build cleaned"
