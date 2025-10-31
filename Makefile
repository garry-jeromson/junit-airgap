.PHONY: help build clean test test-java21 test-java25 benchmark format lint check fix install publish publish-local jar sources-jar all verify setup-native build-native test-native clean-native gpg-generate gpg-list gpg-export-private gpg-export-public gpg-publish gpg-key-id

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
	@echo "  publish-local      Publish to Maven Central from local machine (uses .env file)"
	@echo "  publish            Publish to Maven Central via Central Portal API (CI only)"
	@echo "  publish-plugin     Publish Gradle plugin to Plugin Portal"
	@echo ""
	@echo "GPG Key Management Commands:"
	@echo "  gpg-generate       Generate a new GPG key pair for signing"
	@echo "  gpg-list           List all GPG keys"
	@echo "  gpg-key-id         Show GPG key ID for secrets"
	@echo "  gpg-export-private Export private key (ASCII-armored or base64)"
	@echo "  gpg-export-public  Export public key for publishing to keyservers"
	@echo "  gpg-publish        Publish public key to keyservers"
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
	@if [ ! -d "$$HOME/.m2/repository/io/github/garry-jeromson/junit-airgap-gradle-plugin" ]; then \
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

## publish-local: Publish to Maven Central from local machine (uses .env file)
publish-local:
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  Publishing to Maven Central from Local Machine"
	@echo "═══════════════════════════════════════════════════════════════"
	@echo ""
	@if [ ! -f .env ]; then \
		echo "❌ Error: .env file not found"; \
		echo ""; \
		echo "Create a .env file with your publishing credentials:"; \
		echo ""; \
		echo "  # Maven Central Portal Token (from https://central.sonatype.com/account)"; \
		echo "  export ORG_GRADLE_PROJECT_mavenCentralUsername=YourTokenUsername"; \
		echo "  export ORG_GRADLE_PROJECT_mavenCentralPassword=YourTokenPassword"; \
		echo ""; \
		echo "  # GPG Signing Credentials (ASCII-armored format)"; \
		echo "  export ORG_GRADLE_PROJECT_signingInMemoryKeyId=YourKeyId"; \
		echo "  export ORG_GRADLE_PROJECT_signingInMemoryKey='-----BEGIN PGP PRIVATE KEY BLOCK-----"; \
		echo "  ..."; \
		echo "  -----END PGP PRIVATE KEY BLOCK-----'"; \
		echo "  export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=YourGPGPassphrase"; \
		echo ""; \
		echo "  # Gradle Plugin Portal Credentials"; \
		echo "  export GRADLE_PUBLISH_KEY=YourPluginPortalKey"; \
		echo "  export GRADLE_PUBLISH_SECRET=YourPluginPortalSecret"; \
		echo ""; \
		echo "Tip: Get your GPG key with: make gpg-export-private"; \
		echo ""; \
		exit 1; \
	fi
	@echo "Loading credentials from .env file..."
	@echo ""
	@echo "⚠️  WARNING: You are about to publish to Maven Central and Gradle Plugin Portal!"
	@echo "This will make the artifacts publicly available and cannot be undone."
	@echo ""
	@echo -n "Are you sure you want to continue? [y/N] " && read ans && [ $${ans:-N} = y ]
	@echo ""
	@echo "Publishing junit-airgap library..."
	@bash -c 'source .env && JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :junit-airgap:publishAndReleaseToMavenCentral --no-daemon --stacktrace'
	@echo ""
	@echo "Publishing gradle-plugin to Maven Central..."
	@bash -c 'source .env && JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :gradle-plugin:publishAndReleaseToMavenCentral --no-daemon --stacktrace'
	@echo ""
	@echo "Publishing gradle-plugin to Gradle Plugin Portal..."
	@bash -c 'source .env && JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :gradle-plugin:publishPlugins --no-daemon --stacktrace'
	@echo ""
	@echo "✅ Published successfully!"
	@echo ""
	@echo "Verify at:"
	@echo "  - Maven Central: https://central.sonatype.com/publishing"
	@echo "  - Plugin Portal: https://plugins.gradle.org/plugin/io.github.garry-jeromson.junit-airgap"

## publish: Publish to Maven Central via Central Portal API (CI only)
publish:
	@echo "Publishing artifacts to Maven Central via Central Portal API..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :junit-airgap:publishAndReleaseToMavenCentral :gradle-plugin:publishAndReleaseToMavenCentral

## publish-plugin: Publish Gradle plugin to Plugin Portal
publish-plugin:
	@echo "Publishing Gradle plugin to Plugin Portal..."
	JAVA_HOME=$(JAVA_HOME) $(GRADLEW) :gradle-plugin:publishPlugins

## GPG Key Management Targets

# gpg-generate: Generate a new GPG key pair for artifact signing
gpg-generate:
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  Generating GPG Key Pair for Maven Central Signing"
	@echo "═══════════════════════════════════════════════════════════════"
	@echo ""
	@if ! command -v gpg >/dev/null 2>&1; then \
		echo "❌ Error: GPG not found."; \
		echo ""; \
		echo "Install GPG:"; \
		echo "  macOS:   brew install gnupg"; \
		echo "  Linux:   apt-get install gnupg  (Debian/Ubuntu)"; \
		echo "           yum install gnupg      (RHEL/CentOS)"; \
		exit 1; \
	fi
	@echo "Follow the prompts to create your GPG key:"
	@echo "  - Key type: RSA and RSA (default)"
	@echo "  - Key size: 4096 bits (recommended)"
	@echo "  - Expiration: 0 (does not expire) or set as preferred"
	@echo "  - Real name: Your name"
	@echo "  - Email: Your email (should match Git commits)"
	@echo "  - Passphrase: Choose a strong passphrase and save it securely"
	@echo ""
	@gpg --full-generate-key
	@echo ""
	@echo "✅ GPG key generated successfully!"
	@echo ""
	@echo "Next steps:"
	@echo "  1. Get your key ID:         make gpg-key-id"
	@echo "  2. Export private key:      make gpg-export-private"
	@echo "  3. Publish public key:      make gpg-publish"

# gpg-list: List all GPG keys
gpg-list:
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  GPG Keys"
	@echo "═══════════════════════════════════════════════════════════════"
	@echo ""
	@if ! command -v gpg >/dev/null 2>&1; then \
		echo "❌ Error: GPG not found. Install with: brew install gnupg"; \
		exit 1; \
	fi
	@gpg --list-secret-keys --keyid-format LONG

# gpg-key-id: Show GPG key ID for GitHub secrets
gpg-key-id:
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  GPG Key ID (for SIGNING_KEY_ID secret)"
	@echo "═══════════════════════════════════════════════════════════════"
	@echo ""
	@if ! command -v gpg >/dev/null 2>&1; then \
		echo "❌ Error: GPG not found. Install with: brew install gnupg"; \
		exit 1; \
	fi
	@gpg --list-secret-keys --keyid-format LONG | grep -A 1 "^sec" | head -2 || \
		(echo "❌ No GPG keys found. Generate one with: make gpg-generate" && exit 1)
	@echo ""
	@echo "The key ID is the part after 'rsa4096/' (e.g., ABCD1234EFGH5678)"
	@echo "Use this value for the SIGNING_KEY_ID GitHub secret"

# gpg-export-private: Export private key for publishing
gpg-export-private:
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  Export GPG Private Key"
	@echo "═══════════════════════════════════════════════════════════════"
	@echo ""
	@if ! command -v gpg >/dev/null 2>&1; then \
		echo "❌ Error: GPG not found. Install with: brew install gnupg"; \
		exit 1; \
	fi
	@echo "Enter the key ID (e.g., ABCD1234EFGH5678) from 'make gpg-key-id':"
	@read -p "Key ID: " KEY_ID; \
	if [ -z "$$KEY_ID" ]; then \
		echo "❌ Error: Key ID cannot be empty"; \
		exit 1; \
	fi; \
	echo ""; \
	echo "Choose export format:"; \
	echo "  1) ASCII-armored (for .env file / local publishing)"; \
	echo "  2) Base64 (for GitHub secrets)"; \
	read -p "Choice [1/2]: " CHOICE; \
	echo ""; \
	if [ "$$CHOICE" = "1" ]; then \
		echo "Exporting ASCII-armored private key for key ID: $$KEY_ID"; \
		echo "⚠️  WARNING: This will display your PRIVATE key. Keep it secure!"; \
		echo ""; \
		echo "Copy the entire output below (including BEGIN/END lines) to your .env file:"; \
		echo ""; \
		gpg --armor --export-secret-keys $$KEY_ID; \
		echo ""; \
		echo "✅ Private key exported (ASCII-armored)"; \
		echo ""; \
		echo "Add to .env as:"; \
		echo "  export ORG_GRADLE_PROJECT_signingInMemoryKeyId=$$KEY_ID"; \
		echo "  export ORG_GRADLE_PROJECT_signingInMemoryKey='-----BEGIN PGP..."; \
		echo "  ..."; \
		echo "  -----END PGP PRIVATE KEY BLOCK-----'"; \
	elif [ "$$CHOICE" = "2" ]; then \
		echo "Exporting base64-encoded private key for key ID: $$KEY_ID"; \
		echo "⚠️  WARNING: This will display your PRIVATE key. Keep it secure!"; \
		echo ""; \
		gpg --armor --export-secret-keys $$KEY_ID | base64 | tr -d '\n' && echo ""; \
		echo ""; \
		echo "✅ Private key exported (base64 encoded)"; \
		echo ""; \
		echo "Copy the output above and use it for the SIGNING_KEY GitHub secret"; \
	else \
		echo "❌ Invalid choice"; \
		exit 1; \
	fi; \
	echo ""; \
	echo "⚠️  SECURITY REMINDER:"; \
	echo "  - Never commit this key to version control"; \
	echo "  - Store it securely (password manager recommended)"; \
	echo "  - Clear your terminal history if needed"

# gpg-export-public: Export public key
gpg-export-public:
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  Export GPG Public Key"
	@echo "═══════════════════════════════════════════════════════════════"
	@echo ""
	@if ! command -v gpg >/dev/null 2>&1; then \
		echo "❌ Error: GPG not found. Install with: brew install gnupg"; \
		exit 1; \
	fi
	@echo "Enter the key ID (e.g., ABCD1234EFGH5678) from 'make gpg-key-id':"
	@read -p "Key ID: " KEY_ID; \
	if [ -z "$$KEY_ID" ]; then \
		echo "❌ Error: Key ID cannot be empty"; \
		exit 1; \
	fi; \
	echo ""; \
	echo "Exporting public key for key ID: $$KEY_ID"; \
	echo ""; \
	gpg --armor --export $$KEY_ID; \
	echo ""; \
	echo "✅ Public key exported"

# gpg-publish: Publish public key to keyservers
gpg-publish:
	@echo "═══════════════════════════════════════════════════════════════"
	@echo "  Publish GPG Public Key to Keyservers"
	@echo "═══════════════════════════════════════════════════════════════"
	@echo ""
	@if ! command -v gpg >/dev/null 2>&1; then \
		echo "❌ Error: GPG not found. Install with: brew install gnupg"; \
		exit 1; \
	fi
	@echo "Enter the key ID (e.g., ABCD1234EFGH5678) from 'make gpg-key-id':"
	@read -p "Key ID: " KEY_ID; \
	if [ -z "$$KEY_ID" ]; then \
		echo "❌ Error: Key ID cannot be empty"; \
		exit 1; \
	fi; \
	echo ""; \
	echo "Publishing public key to multiple keyservers..."; \
	echo ""; \
	echo "→ keyserver.ubuntu.com"; \
	gpg --keyserver keyserver.ubuntu.com --send-keys $$KEY_ID || echo "⚠️  Failed to publish to keyserver.ubuntu.com"; \
	echo ""; \
	echo "→ keys.openpgp.org"; \
	gpg --keyserver keys.openpgp.org --send-keys $$KEY_ID || echo "⚠️  Failed to publish to keys.openpgp.org"; \
	echo ""; \
	echo "→ pgp.mit.edu"; \
	gpg --keyserver pgp.mit.edu --send-keys $$KEY_ID || echo "⚠️  Failed to publish to pgp.mit.edu"; \
	echo ""; \
	echo "✅ Public key published to keyservers"; \
	echo ""; \
	echo "Note: It may take a few minutes for the key to propagate"; \
	echo "Verify publication with: gpg --keyserver keyserver.ubuntu.com --search-keys <your-email>"

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
