#!/usr/bin/env bash
# Test build script for InvOverstack mod

set -e

echo "========================================="
echo "InvOverstack - Build Test Script"
echo "========================================="
echo ""

# Check if we're in a nix shell
if [ -z "$IN_NIX_SHELL" ]; then
    echo "Not in nix shell. Entering nix develop..."
    exec nix develop --command bash "$0" "$@"
fi

echo "✓ Running in nix development shell"
echo ""

echo "Java version:"
java -version
echo ""

echo "Gradle version:"
./gradlew --version | grep "Gradle"
echo ""

echo "========================================="
echo "Building mod..."
echo "========================================="
./gradlew build

echo ""
echo "========================================="
echo "Build complete!"
echo "========================================="
echo ""

# Check if build succeeded
if [ -d "build/libs" ]; then
    echo "✓ Build artifacts created in build/libs/"
    ls -lh build/libs/
else
    echo "✗ Build failed - no artifacts found"
    exit 1
fi

echo ""
echo "To run the client: ./gradlew runClient"
echo "To run the server: ./gradlew runServer"
