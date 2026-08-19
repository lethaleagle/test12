#!/usr/bin/env bash
#
# Runs every unit test, including the Robolectric tests that drive the real
# activity, without the Android SDK. Screenshots land in build/screenshots/.
#
# `./gradlew test` is the equivalent on a machine with the SDK installed.
#
# Usage: tools/run-ui-tests.sh

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

command -v gradle >/dev/null 2>&1 || { echo "gradle is required" >&2; exit 1; }

# Robolectric needs the compiled resources and generated R.java.
if [ ! -f "$PROJECT_ROOT/build/no-sdk/base.apk" ]; then
    echo "==> building resources first"
    "$PROJECT_ROOT/tools/build-apk.sh" >/dev/null
fi

gradle -p "$PROJECT_ROOT/tools/uitest" test --console=plain "$@"
