#!/usr/bin/env bash
#
# Runs the logic tests without Gradle.
#
# `./gradlew test` is the usual command; this script is the equivalent for
# machines that can't reach the Android SDK. It covers the classes that are
# plain Java with no Android dependencies, so it needs nothing but a JDK.
# The UI tests need Robolectric and the Android framework — run those with
# tools/run-ui-tests.sh.
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

# Only the Android-free classes and their tests; MainActivity and the UI tests
# need the framework on the classpath, which tools/run-ui-tests.sh provides.
javac -encoding UTF-8 -nowarn -d "$WORK" -cp "$JUNIT_JAR" \
    "$PROJECT_ROOT"/app/src/main/java/com/example/calculator/{Evaluator,Numbers,Expression}.java \
    "$PROJECT_ROOT"/app/src/test/java/com/example/calculator/{EvaluatorTest,NumbersTest,ExpressionTest}.java

java -jar "$JUNIT_JAR" execute --class-path "$WORK" --scan-class-path --details=summary
