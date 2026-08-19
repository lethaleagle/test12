#!/usr/bin/env bash
#
# Builds a debug APK without the Android Gradle Plugin.
#
# The normal way to build this project is `./gradlew assembleDebug`, which needs
# the Android SDK from dl.google.com. This script is the fallback for machines
# that can't reach it: it drives aapt2, javac, dx and apksigner directly, taking
# android.jar and the dex compiler from mirrors that don't require the SDK
# manager. The APK it produces is a normal debug-signed APK.
#
# Requirements: a JDK, plus aapt2, apksigner and zipalign on PATH
# (Debian/Ubuntu: apt-get install aapt apksigner zipalign).
#
# Usage: tools/build-apk.sh [output.apk]

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="$PROJECT_ROOT/app/src/main"
OUT="${1:-$PROJECT_ROOT/build/calculator-debug.apk}"
WORK="$PROJECT_ROOT/build/no-sdk"
CACHE="${ANDROID_BUILD_CACHE:-$PROJECT_ROOT/build/toolchain}"

PACKAGE="com.example.calculator"
MIN_SDK=21
TARGET_SDK=34
VERSION_CODE=1
VERSION_NAME="1.0"

ANDROID_JAR_URL="https://raw.githubusercontent.com/Reginer/aosp-android-jar/main/android-34/android.jar"
DX_URL="https://repo1.maven.org/maven2/com/jakewharton/android/repackaged/dalvik-dx/14.0.0_r21/dalvik-dx-14.0.0_r21.jar"

say() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }

for tool in aapt2 apksigner zipalign keytool javac java; do
    command -v "$tool" >/dev/null 2>&1 || { echo "missing required tool: $tool" >&2; exit 1; }
done

mkdir -p "$CACHE" "$WORK" "$(dirname "$OUT")"

ANDROID_JAR="$CACHE/android-$TARGET_SDK.jar"
DX_JAR="$CACHE/dalvik-dx.jar"

if [ ! -f "$ANDROID_JAR" ]; then
    say "fetching android.jar (API $TARGET_SDK)"
    curl -fsSL "$ANDROID_JAR_URL" -o "$ANDROID_JAR.tmp" && mv "$ANDROID_JAR.tmp" "$ANDROID_JAR"
fi
if [ ! -f "$DX_JAR" ]; then
    say "fetching dex compiler"
    curl -fsSL "$DX_URL" -o "$DX_JAR.tmp" && mv "$DX_JAR.tmp" "$DX_JAR"
fi

rm -rf "$WORK"
mkdir -p "$WORK"/{res,gen,classes,dex}

# The manifest carries no package attribute so that AGP 8 can supply the
# namespace itself; aapt2 still wants one, so add it to a staged copy.
say "staging manifest"
sed "s|<manifest |<manifest package=\"$PACKAGE\" |" \
    "$SRC/AndroidManifest.xml" > "$WORK/AndroidManifest.xml"

say "compiling resources"
aapt2 compile --dir "$SRC/res" -o "$WORK/res/resources.zip"

say "linking resources"
aapt2 link \
    -o "$WORK/base.apk" \
    -I "$ANDROID_JAR" \
    --manifest "$WORK/AndroidManifest.xml" \
    --java "$WORK/gen" \
    --min-sdk-version "$MIN_SDK" \
    --target-sdk-version "$TARGET_SDK" \
    --version-code "$VERSION_CODE" \
    --version-name "$VERSION_NAME" \
    --auto-add-overlay \
    "$WORK/res/resources.zip"

say "compiling java"
find "$SRC/java" "$WORK/gen" -name '*.java' > "$WORK/sources.txt"
# dx reads Java 8 bytecode, which is also all this app's language level needs.
javac -source 8 -target 8 -encoding UTF-8 -nowarn \
    -bootclasspath "$ANDROID_JAR" \
    -classpath "$ANDROID_JAR" \
    -d "$WORK/classes" \
    @"$WORK/sources.txt" 2>&1 | grep -v 'bootstrap class path\|source value 8\|target value 8\|deprecat' || true

if [ -z "$(find "$WORK/classes" -name '*.class' -print -quit)" ]; then
    echo "javac produced no classes" >&2
    exit 1
fi

say "dexing"
java -cp "$DX_JAR" com.android.dx.command.Main \
    --dex --min-sdk-version="$MIN_SDK" --output="$WORK/dex/classes.dex" "$WORK/classes"

say "packaging"
cp "$WORK/base.apk" "$WORK/unaligned.apk"
(cd "$WORK/dex" && zip -q -X "$WORK/unaligned.apk" classes.dex)

say "aligning"
zipalign -f -p 4 "$WORK/unaligned.apk" "$WORK/aligned.apk"

KEYSTORE="$CACHE/debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
    say "creating debug keystore"
    keytool -genkeypair -v -keystore "$KEYSTORE" -storepass android -keypass android \
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10950 \
        -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
fi

say "signing"
apksigner sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
    --ks-key-alias androiddebugkey --min-sdk-version "$MIN_SDK" \
    --out "$OUT" "$WORK/aligned.apk"

apksigner verify --min-sdk-version "$MIN_SDK" "$OUT"

say "built $OUT ($(du -h "$OUT" | cut -f1))"
