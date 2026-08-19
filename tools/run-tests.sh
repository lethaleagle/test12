#!/usr/bin/env bash
#
# Runs the unit tests without Gradle.
#
# `./gradlew test` is the usual command; this script is the equivalent for
# machines that can't reach the Android SDK. The classes under test are plain
# Java with no Android dependencies, so they compile and run on any JDK.
#
# Usage: tools/run-tests.sh

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="$PROJECT_ROOT/build/test-no-gradle"
CACHE="${ANDROID_BUILD_CACHE:-$PROJECT_ROOT/build/toolchain}"

JUNIT_VERSION=1.10.2
JUNIT_JAR="$CACHE/junit-platform-console-standalone-$JUNIT_VERSION.jar"
JUNIT_URL="https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$JUNIT_VERSION/junit-platform-console-standalone-$JUNIT_VERSION.jar"

mkdir -p "$CACHE" "$WORK"

if [ ! -f "$JUNIT_JAR" ]; then
    echo "==> fetching JUnit"
    curl -fsSL "$JUNIT_URL" -o "$JUNIT_JAR.tmp" && mv "$JUNIT_JAR.tmp" "$JUNIT_JAR"
fi

# MainActivity is the only class that touches the Android framework, and it holds
# no logic of its own worth testing off-device.
javac -encoding UTF-8 -nowarn -d "$WORK" -cp "$JUNIT_JAR" \
    "$PROJECT_ROOT"/app/src/main/java/com/example/calculator/{Evaluator,Numbers,Expression}.java \
    "$PROJECT_ROOT"/app/src/test/java/com/example/calculator/*.java

java -jar "$JUNIT_JAR" execute --class-path "$WORK" --scan-class-path --details=summary
