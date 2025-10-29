# Publishing Guide

This document describes how to publish the JUnit Airgap Extension to Maven Central and Gradle Plugin Portal.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [One-Time Setup](#one-time-setup)
  - [1. Create Sonatype OSSRH Account](#1-create-sonatype-ossrh-account)
  - [2. Generate GPG Keys](#2-generate-gpg-keys)
  - [3. Publish GPG Public Key](#3-publish-gpg-public-key)
  - [4. Create Gradle Plugin Portal Account](#4-create-gradle-plugin-portal-account)
  - [5. Configure GitHub Secrets](#5-configure-github-secrets)
- [Publishing a Release](#publishing-a-release)
- [Troubleshooting](#troubleshooting)

---

## Overview

The JUnit Airgap Extension is published to two repositories:

1. **Maven Central** - For the core library artifacts (via Central Portal API)
   - `io.github.garryjeromson:junit-airgap` (JVM + Android)
   - `io.github.garryjeromson:junit-airgap-gradle-plugin` (Gradle plugin)

2. **Gradle Plugin Portal** - For the Gradle plugin
   - `io.github.garryjeromson.junit-airgap` plugin

The GitHub Actions workflow automates publishing to both repositories simultaneously using the modern Central Portal Publisher API.

## Prerequisites

Before you can publish, you need:

1. **Sonatype OSSRH Account** - For publishing to Maven Central
2. **GPG Key Pair** - For signing artifacts (required by Maven Central)
3. **Gradle Plugin Portal Account** - For publishing the Gradle plugin
4. **GitHub Repository Access** - Admin access to configure secrets

---

## One-Time Setup

### 1. Create Maven Central Account via GitHub

Maven Central now uses the **Central Portal** (https://central.sonatype.com) - a modern, self-service platform that has replaced the old Jira-based OSSRH system.

#### Sign Up with GitHub (Recommended)

1. **Go to** [https://central.sonatype.com](https://central.sonatype.com)
2. **Click "Sign Up"**
3. **Select "Sign up with GitHub"**
4. **Authorize the application** when prompted by GitHub
5. **Verify your email address** - check your inbox for the verification link

#### Register Your Namespace

After signing up, you'll automatically receive a verified namespace for your GitHub account:

- **Your namespace**: `io.github.garryjeromson`
- **Status**: ✅ Automatically verified (no manual verification needed!)

This namespace allows you to publish artifacts with group IDs like:
- `io.github.garryjeromson:junit-airgap`
- `io.github.garryjeromson:junit-airgap-gradle-plugin`

**Important Notes:**
- ⚠️ **Usernames cannot be changed** - your Central Portal username is tied to your GitHub account
- GitHub-based signup provides instant namespace verification
- No Jira tickets or waiting periods required

### 2. Generate GPG Keys

GPG (GNU Privacy Guard) is used to sign artifacts to verify their authenticity.

#### Generate Key Pair

Use the Makefile target to generate a new GPG key:

```bash
make gpg-generate
```

This will:
- Check if GPG is installed (and prompt to install via Homebrew if not)
- Guide you through key generation with recommended settings
- Provide next steps after generation

**Follow the prompts:**
- **Key type**: RSA and RSA (default)
- **Key size**: 4096 bits (recommended)
- **Expiration**: 0 (does not expire) or set as preferred
- **Real name**: Your name
- **Email**: Your email (should match Git commits)
- **Passphrase**: Choose a strong passphrase and **save it securely**

#### Get Key ID

After generating your key, get the key ID for GitHub secrets:

```bash
make gpg-key-id
```

This displays your key ID (e.g., `ABCD1234EFGH5678`) which you'll need for the `SIGNING_KEY_ID` secret.

#### Export Private Key

Export your private key in base64 format for GitHub secrets:

```bash
make gpg-export-private
```

Enter your key ID when prompted. The output will be base64-encoded and ready to paste into the `SIGNING_KEY` GitHub secret.

**⚠️ Security Warning:**
- This displays your PRIVATE key - keep it secure!
- Never commit it to version control
- Store it securely (password manager recommended)
- Clear your terminal history if needed

### 3. Publish GPG Public Key

Your public key must be published to key servers so Maven Central can verify signatures.

Publish to multiple keyservers using the Makefile target:

```bash
make gpg-publish
```

Enter your key ID when prompted. This will publish your public key to:
- keyserver.ubuntu.com
- keys.openpgp.org
- pgp.mit.edu

**Verify publication:**

Wait a few minutes for propagation, then verify:
```bash
gpg --keyserver keyserver.ubuntu.com --search-keys your.email@example.com
```

### 4. Generate Maven Central Portal Token

The Central Portal uses **Portal Tokens** for publishing authentication (replacing the old username/password approach).

#### Generate Your Token

1. **Log in** to https://central.sonatype.com (using your GitHub account)
2. **Navigate to** https://central.sonatype.com/account (or click your profile icon → "Account")
3. **Click on** "Generate User Token" button
4. **Enter details:**
   - **Display Name**: "GitHub Actions Publishing" (or any descriptive name)
   - **Expiration**: Set an appropriate expiration date (e.g., 1 year)
5. **Click "Generate"**
6. **Save the credentials immediately**:
   - **Username**: (token username, e.g., `AbCdEfGh`)
   - **Password**: (token password, long random string)

**⚠️ Critical Warning:**
- Tokens **cannot be retrieved** once the modal closes
- Save both username and password securely (password manager recommended)
- If lost, you must generate a new token
- The token username/password pair is used as publishing credentials

**Note:** While legacy OSSRH username/password still works, Portal Tokens are the recommended modern approach and provide better security through expiration dates and granular control.

### 5. Create Gradle Plugin Portal Account

The Gradle plugin needs to be published to the Gradle Plugin Portal.

#### Sign Up

1. Go to https://plugins.gradle.org/
2. Click **"Log in"** in the top right
3. Sign in with GitHub, Google, or create an account
4. Accept the Terms of Service

#### Generate API Keys

1. Once logged in, click your username in the top right
2. Click **"API Keys"**
3. Click **"Create new key"**
4. Give it a descriptive name (e.g., "GitHub Actions Publishing")
5. Copy the **Key** and **Secret** - you'll need these for GitHub secrets
   - ⚠️ The secret is only shown once - save it securely

**Note:** These credentials allow publishing plugins under your account. Keep them secure.

### 6. Configure GitHub Secrets

GitHub Actions needs access to your credentials to publish artifacts.

#### Navigate to Repository Settings

1. Go to https://github.com/garry-jeromson/junit-request-blocker
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**

#### Add Secrets

Add the following secrets (click "New repository secret" for each):

**Maven Central Credentials:**

| Secret Name | Value | How to Get It |
|------------|-------|---------------|
| `MAVEN_CENTRAL_USERNAME` | Your Portal Token username | From step 4 (Central Portal Token username, e.g., `AbCdEfGh`) |
| `MAVEN_CENTRAL_PASSWORD` | Your Portal Token password | From step 4 (Central Portal Token password - long random string) |
| `SIGNING_KEY_ID` | Your GPG key ID | Short key ID from step 2 (e.g., `ABCD1234EFGH5678`) |
| `SIGNING_KEY` | Your GPG private key (base64) | See below |
| `SIGNING_PASSWORD` | Your GPG passphrase | The passphrase you set when creating the key |

**Important:** These credentials are your **Portal Token** (username and password from the token generation page), NOT your Central Portal login credentials.

**Gradle Plugin Portal Credentials:**

| Secret Name | Value | How to Get It |
|------------|-------|---------------|
| `GRADLE_PUBLISH_KEY` | Your Plugin Portal API key | From step 5 (Plugin Portal API Keys) |
| `GRADLE_PUBLISH_SECRET` | Your Plugin Portal API secret | From step 5 (Plugin Portal API Keys) |

#### Quick Reference: GPG Commands

**List your GPG keys:**
```bash
make gpg-list
```

**Export public key (if needed):**
```bash
make gpg-export-public
```

---

## Publishing a Release

You can publish either from your local machine or via GitHub Actions.

### Option A: Publish from Local Machine

This is useful for testing the publishing process or for quick releases.

#### 1. Set Up Local Environment

Create a `.env` file in the project root (copy from `.env.example`):

```bash
cp .env.example .env
```

Edit `.env` and fill in your credentials:

```bash
# Maven Central Portal Token (from https://central.sonatype.com/account)
export ORG_GRADLE_PROJECT_mavenCentralUsername=YourTokenUsername
export ORG_GRADLE_PROJECT_mavenCentralPassword=YourTokenPassword

# GPG Signing Credentials
export ORG_GRADLE_PROJECT_signingInMemoryKey=YourBase64EncodedPrivateKey
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=YourGPGPassphrase

# Gradle Plugin Portal Credentials
export GRADLE_PUBLISH_KEY=YourPluginPortalKey
export GRADLE_PUBLISH_SECRET=YourPluginPortalSecret
```

**Get your credentials:**
- **Maven Central Portal Token**: Generate at https://central.sonatype.com/account
- **GPG Key**: Run `make gpg-export-private`
- **GPG Passphrase**: The passphrase you set when creating your GPG key
- **Gradle Plugin Portal**: Login at https://plugins.gradle.org/ → API Keys

**⚠️ Security:** Never commit the `.env` file! It's already in `.gitignore`.

#### 2. Publish

Run the publish-local target:

```bash
make publish-local
```

This will:
- Check that `.env` exists
- Load your credentials
- Prompt for confirmation (publishing is irreversible!)
- Publish `junit-airgap` library to Maven Central
- Publish `gradle-plugin` to both Maven Central and Gradle Plugin Portal
- Automatically release after validation

### Option B: Publish via GitHub Actions

This is the recommended approach for production releases.

### 1. Prepare the Release

1. **Ensure all tests pass:**
   ```bash
   make test
   make test-plugin-integration
   ```

2. **Update CHANGELOG** (if you maintain one)
   - Document new features, bug fixes, and breaking changes

3. **Commit all changes:**
   ```bash
   git add .
   git commit -m "Prepare release X.Y.Z"
   git push origin main
   ```

### 2. Create Git Tag

The version is determined from git tags:

```bash
# Create an annotated tag
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push the tag to GitHub (optional - only if you want auto-publish on tag push)
git push origin v1.0.0
```

**Version format:** `vX.Y.Z` (e.g., `v1.0.0`, `v1.2.3`, `v2.0.0-beta.1`)

### 3. Trigger GitHub Actions Publish Workflow

1. Go to https://github.com/garry-jeromson/junit-request-blocker/actions
2. Click on **"Publish to Maven Central and Gradle Plugin Portal"** workflow
3. Click **"Run workflow"** button
4. **Optional:** Override version (leave empty to auto-detect from tag)
5. Click **"Run workflow"** green button

### 4. Monitor the Workflow

1. Click on the running workflow to see progress
2. The workflow will:
   - ✅ Detect version from git tag
   - ✅ Run all tests (211 integration tests)
   - ✅ Sign all artifacts with your GPG key
   - ✅ Publish to Maven Central via Central Portal API
   - ✅ Automatically validate and release artifacts
   - ✅ Publish Gradle plugin to Plugin Portal
   - ✅ Create a GitHub release

3. **Typical duration:** 5-10 minutes

**Note:** The new Central Portal API automatically validates and publishes artifacts without manual staging repository management.

### 5. Verify Publication

**Central Portal - Immediate:**
- Check deployment status at https://central.sonatype.com/publishing
- Log in with your GitHub account
- Navigate to **Deployments** to see your published artifacts
- Status will show: VALIDATED → PUBLISHING → PUBLISHED

**Maven Central - Within 30 minutes:**
- Maven Central search: https://search.maven.org/search?q=g:io.github.garryjeromson
- Central Portal page: https://central.sonatype.com/artifact/io.github.garryjeromson/junit-airgap

**Maven Central - Within 2 hours:**
- Full Maven Central CDN synchronization
- Gradle/Maven can reliably download artifacts from all mirrors

**Gradle Plugin Portal - Within 30 minutes:**
- Plugin Portal: https://plugins.gradle.org/plugin/io.github.garryjeromson.junit-airgap
- Plugin should appear in search results
- Users can apply the plugin with `id("io.github.garryjeromson.junit-airgap")`

**Test Maven Central artifacts:**
```gradle
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.github.garryjeromson:junit-airgap:1.0.0")
}
```

**Test Gradle Plugin:**
```gradle
plugins {
    id("io.github.garryjeromson.junit-airgap") version "1.0.0"
}
```

---

## Troubleshooting

### Issue: "Authentication failed" or "Invalid credentials"

**Cause:** Missing or incorrect Maven Central Portal Token credentials

**Solution:**
- Verify GitHub secrets `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` are set correctly
- These should contain your **Portal Token credentials** (from https://central.sonatype.com/account), not your login credentials
- Test by generating a new Portal Token:
  1. Log into https://central.sonatype.com
  2. Navigate to Account → Generate User Token
  3. Copy both the username and password from the token
  4. Update GitHub secrets with these values
- **Note:** Portal Tokens have expiration dates - check if your token has expired and generate a new one if needed

### Issue: "Could not load PGP key"

**Cause:** Incorrect GPG key format or encoding

**Solution:**
- Verify `SIGNING_KEY` is base64-encoded (should be a very long single line)
- Re-export the key using the Makefile target:
  ```bash
  make gpg-export-private
  ```
- Copy the entire output (no line breaks) and update the GitHub secret

### Issue: "Invalid signature" or "No public key"

**Cause:** GPG public key not published to keyservers

**Solution:**
- Verify key is published:
  ```bash
  gpg --keyserver keyserver.ubuntu.com --search-keys your.email@example.com
  ```
- If not found, publish it:
  ```bash
  make gpg-publish
  ```
- Wait 5-10 minutes for propagation across keyservers

### Issue: "Version X.Y.Z already exists"

**Cause:** Attempting to publish the same version twice

**Solution:**
- Maven Central does not allow overwriting published versions
- Increment the version number:
  ```bash
  git tag -d vX.Y.Z          # Delete local tag
  git push --delete origin vX.Y.Z  # Delete remote tag
  git tag -a vX.Y.Z+1 -m "..."     # Create new tag with incremented version
  ```

### Issue: "Publishing failed" or "Validation errors"

**Cause:** Validation errors (missing signatures, invalid POM, incorrect metadata, etc.)

**Solution:**
1. Log into https://central.sonatype.com
2. Navigate to **Deployments** tab
3. Find your deployment (sorted by date, most recent first)
4. Click on the deployment to see validation status and error messages
5. Common issues:
   - Missing or invalid GPG signatures
   - Incomplete POM metadata (missing license, SCM, or developer info)
   - Invalid artifact coordinates or naming
6. Fix the issues in your build configuration and re-run the workflow

### Issue: Tests fail during publish workflow

**Cause:** Code changes broke tests

**Solution:**
- Run tests locally first:
  ```bash
  make test
  make test-plugin-integration
  ```
- Fix failing tests before publishing
- The workflow will not publish if tests fail (this is intentional!)

---

## Additional Resources

- [Central Portal Publishing Guide](https://central.sonatype.org/publish/publish-portal-guide/)
- [vanniktech Maven Publish Plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/)
- [Central Portal API Documentation](https://central.sonatype.org/publish/publish-portal-api/)
- [GPG Tutorial](https://central.sonatype.org/publish/requirements/gpg/)
- [GitHub Actions Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Semantic Versioning](https://semver.org/)

---

## Support

If you encounter issues not covered here:

1. Check GitHub Actions logs for detailed error messages
2. Review Sonatype Nexus staging repository activity
3. Search [Sonatype JIRA](https://issues.sonatype.org/) for similar issues
4. Open an issue in this repository with details
