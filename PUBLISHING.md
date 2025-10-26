# Publishing Guide

This document describes how to publish the JUnit No-Network Extension to Maven Central and Gradle Plugin Portal.

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

The JUnit No-Network Extension is published to two repositories:

1. **Maven Central** - For the core library artifacts
   - `io.github.garryjeromson:junit-no-network` (JVM)
   - `io.github.garryjeromson:junit-no-network-android` (Android)

2. **Gradle Plugin Portal** - For the Gradle plugin
   - `io.github.garryjeromson.junit-no-network` plugin

The GitHub Actions workflow automates publishing to both repositories simultaneously.

## Prerequisites

Before you can publish, you need:

1. **Sonatype OSSRH Account** - For publishing to Maven Central
2. **GPG Key Pair** - For signing artifacts (required by Maven Central)
3. **Gradle Plugin Portal Account** - For publishing the Gradle plugin
4. **GitHub Repository Access** - Admin access to configure secrets

---

## One-Time Setup

### 1. Create Sonatype OSSRH Account

1. **Sign up** at [https://issues.sonatype.org](https://issues.sonatype.org)
   - Create a Jira account if you don't have one

2. **Request access** to your Group ID
   - Create a new issue: [New Project Ticket](https://issues.sonatype.org/secure/CreateIssue.jspa?issuetype=21&pid=10134)
   - **Project**: Community Support - Open Source Project Repository Hosting (OSSRH)
   - **Issue Type**: New Project
   - **Summary**: "Request publishing rights for io.github.garryjeromson"
   - **Group Id**: `io.github.garryjeromson`
   - **Project URL**: `https://github.com/garry-jeromson/junit-request-blocker`
   - **SCM URL**: `https://github.com/garry-jeromson/junit-request-blocker.git`

3. **Verify ownership** of the Group ID
   - Sonatype will ask you to verify ownership of `github.com/garry-jeromson`
   - Methods to verify:
     - Add a GitHub repository matching the ticket ID (e.g., `OSSRH-12345`)
     - Or add a TXT DNS record to your custom domain

4. **Wait for approval** (usually 1-2 business days)
   - You'll receive a comment on the ticket when approved
   - Save your Sonatype username and password - you'll need these later

### 2. Generate GPG Keys

GPG (GNU Privacy Guard) is used to sign artifacts to verify their authenticity.

#### Install GPG

**macOS:**
```bash
brew install gnupg
```

**Linux:**
```bash
sudo apt-get install gnupg  # Debian/Ubuntu
sudo yum install gnupg      # RHEL/CentOS
```

**Windows:**
Download from [https://gnupg.org/download/](https://gnupg.org/download/)

#### Generate Key Pair

```bash
# Generate a new GPG key pair (interactive)
gpg --full-generate-key
```

**Prompts:**
- **Kind of key**: RSA and RSA (default)
- **Key size**: 4096 bits (more secure)
- **Expiration**: 0 (key does not expire) or set expiration as preferred
- **Real name**: Your name
- **Email**: Your email (should match Git commits)
- **Comment**: Optional (e.g., "Maven Central signing key")
- **Passphrase**: Choose a strong passphrase and **save it securely**

#### Get Key ID

```bash
# List secret keys with long format
gpg --list-secret-keys --keyid-format LONG
```

Output example:
```
sec   rsa4096/ABCD1234EFGH5678 2025-01-26 [SC]
      1234567890ABCDEF1234567890ABCDEF12345678
uid                 [ultimate] Your Name <your.email@example.com>
ssb   rsa4096/IJKL9012MNOP3456 2025-01-26 [E]
```

The key ID is **ABCD1234EFGH5678** (after `rsa4096/`)
The full fingerprint is the long hex string on the second line.

#### Export Private Key

```bash
# Export the private key (ASCII-armored format)
gpg --armor --export-secret-keys ABCD1234EFGH5678 > signing-key.asc

# View the exported key (should start with -----BEGIN PGP PRIVATE KEY BLOCK-----)
cat signing-key.asc
```

**⚠️ Security Warning:**
- Keep `signing-key.asc` **extremely secure** - it's your private key
- Never commit it to version control
- Delete it after configuring GitHub secrets
- Back it up in a secure location (password manager, encrypted drive)

### 3. Publish GPG Public Key

Your public key must be published to key servers so Maven Central can verify signatures.

```bash
# Publish to multiple keyservers for redundancy
gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234EFGH5678
gpg --keyserver keys.openpgp.org --send-keys ABCD1234EFGH5678
gpg --keyserver pgp.mit.edu --send-keys ABCD1234EFGH5678
```

**Verify publication:**
```bash
# Search for your key (wait a few minutes for propagation)
gpg --keyserver keyserver.ubuntu.com --search-keys your.email@example.com
```

### 4. Create Gradle Plugin Portal Account

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

### 5. Configure GitHub Secrets

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
| `OSSRH_USERNAME` | Your Sonatype username | From step 1 (Sonatype OSSRH account) |
| `OSSRH_PASSWORD` | Your Sonatype password | From step 1 (generate a token is recommended) |
| `SIGNING_KEY_ID` | Your GPG key ID | Short key ID from step 2 (e.g., `ABCD1234EFGH5678`) |
| `SIGNING_KEY` | Your GPG private key (base64) | See below |
| `SIGNING_PASSWORD` | Your GPG passphrase | The passphrase you set when creating the key |

**Gradle Plugin Portal Credentials:**

| Secret Name | Value | How to Get It |
|------------|-------|---------------|
| `GRADLE_PUBLISH_KEY` | Your Plugin Portal API key | From step 4 (Plugin Portal API Keys) |
| `GRADLE_PUBLISH_SECRET` | Your Plugin Portal API secret | From step 4 (Plugin Portal API Keys) |

#### Encoding the Private Key

The `SIGNING_KEY` must be base64-encoded:

```bash
# Encode the private key to base64
base64 -i signing-key.asc | tr -d '\n' | pbcopy
```

This copies the encoded key to your clipboard. Paste it as the `SIGNING_KEY` secret value.

**Alternative (Linux):**
```bash
base64 -w 0 < signing-key.asc | xclip -selection clipboard
```

**Alternative (manual):**
```bash
base64 -i signing-key.asc > signing-key-base64.txt
# Copy the contents of signing-key-base64.txt to the secret
```

**⚠️ Cleanup:**
```bash
# Delete temporary files containing your private key
rm signing-key.asc
rm signing-key-base64.txt  # if created
```

---

## Publishing a Release

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
   - ✅ Publish to Maven Central staging repository
   - ✅ Automatically close and release the staging repository
   - ✅ Publish Gradle plugin to Plugin Portal
   - ✅ Create a GitHub release

3. **Typical duration:** 5-10 minutes

### 5. Verify Publication

**Maven Central - Immediate (Staging):**
- Check Sonatype Nexus: https://s01.oss.sonatype.org/
- Log in with your OSSRH credentials
- Navigate to **Staging Repositories**
- Your artifacts should be in a "Released" state

**Maven Central - Within 30 minutes:**
- Maven Central search: https://search.maven.org/search?q=g:io.github.garryjeromson

**Maven Central - Within 2 hours:**
- Full Maven Central availability
- Gradle/Maven can download artifacts

**Gradle Plugin Portal - Within 30 minutes:**
- Plugin Portal: https://plugins.gradle.org/plugin/io.github.garryjeromson.junit-no-network
- Plugin should appear in search results
- Users can apply the plugin with `id("io.github.garryjeromson.junit-no-network")`

**Test Maven Central artifacts:**
```gradle
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.github.garryjeromson:junit-no-network:1.0.0")
}
```

**Test Gradle Plugin:**
```gradle
plugins {
    id("io.github.garryjeromson.junit-no-network") version "1.0.0"
}
```

---

## Troubleshooting

### Issue: "Nexus repository manager credentials are not set"

**Cause:** Missing or incorrect Sonatype credentials

**Solution:**
- Verify GitHub secrets `OSSRH_USERNAME` and `OSSRH_PASSWORD` are set correctly
- Test credentials by logging into https://s01.oss.sonatype.org/
- Consider creating a User Token instead of using your password:
  - Log into Sonatype Nexus
  - Profile → User Token → Access User Token
  - Use token username/password as credentials

### Issue: "Could not load PGP key"

**Cause:** Incorrect GPG key format or encoding

**Solution:**
- Verify `SIGNING_KEY` is base64-encoded (should be a very long single line)
- Re-export and re-encode the key:
  ```bash
  gpg --armor --export-secret-keys YOUR_KEY_ID | base64 -w 0
  ```
- Ensure no line breaks in the secret value

### Issue: "Invalid signature" or "No public key"

**Cause:** GPG public key not published to keyservers

**Solution:**
- Verify key is published:
  ```bash
  gpg --keyserver keyserver.ubuntu.com --search-keys your.email@example.com
  ```
- If not found, re-publish:
  ```bash
  gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
  ```
- Wait 5-10 minutes for propagation

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

### Issue: "Staging repository failed to close"

**Cause:** Validation errors (missing signatures, invalid POM, etc.)

**Solution:**
1. Log into https://s01.oss.sonatype.org/
2. Navigate to **Staging Repositories**
3. Find your repository (search by name: `iogithubgarryjeromson-XXXX`)
4. Click **Activity** tab to see detailed error messages
5. Fix the issues and re-run the workflow

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

- [Maven Central Publishing Guide](https://central.sonatype.org/publish/publish-guide/)
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
