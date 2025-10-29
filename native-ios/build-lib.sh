#!/bin/bash
#
# Build AirgapURLProtocol as a static library for iOS Simulator (arm64)
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"
# Use the source from junit-airgap cinterop directory (the real implementation)
SRC_DIR="$SCRIPT_DIR/../junit-airgap/src/nativeInterop/cinterop"

# Create build directory
mkdir -p "$BUILD_DIR"

# SDK paths for iOS Simulator
SDK_PATH="$(xcrun --sdk iphonesimulator --show-sdk-path)"
TARGET="arm64-apple-ios15.0-simulator"

echo "Building AirgapURLProtocol static library..."
echo "SDK: $SDK_PATH"
echo "Target: $TARGET"

# Compile Objective-C to object file
clang -c \
    -target "$TARGET" \
    -isysroot "$SDK_PATH" \
    -fobjc-arc \
    -fmodules \
    -I"$SRC_DIR" \
    -o "$BUILD_DIR/AirgapURLProtocol.o" \
    "$SRC_DIR/AirgapURLProtocol.m"

# Create static library
ar rcs "$BUILD_DIR/libAirgapURLProtocol.a" "$BUILD_DIR/AirgapURLProtocol.o"

echo "Static library created at: $BUILD_DIR/libAirgapURLProtocol.a"
