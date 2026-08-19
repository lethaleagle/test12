# Calculator

A small Android calculator: expression entry with a live running result,
calculator-style percentages, and a dark keypad that adapts to landscape.

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" alt="App icon">

## What it does

- Type a whole expression (`12+7×3`) and see the answer update as you go; `=`
  commits it and leaves the result ready as the start of the next calculation.
- `%` behaves the way a pocket calculator does: `50+10%` is `55`, not `50.1`.
  In a multiplication or division it is a plain divide-by-100, so `200×10%` is `20`.
- `±` flips the sign of the number you are currently typing, wherever it sits in
  the expression.
- Backspace deletes one character; hold it to clear everything.
- Results avoid floating-point dust (`0.1+0.2` shows `0.3`), group long numbers
  with separators, and only fall back to exponents past 10^12 or below 10^-6.
- Division by zero and half-finished expressions report an error instead of
  showing `Infinity` or `NaN`.

The app targets API 34, runs on API 21 and up, and has no dependencies beyond
the Android framework itself.

## Layout of the code

| Path | What lives there |
| --- | --- |
| `app/src/main/java/.../MainActivity.java` | The screen: wires keys to actions and renders the display |
| `app/src/main/java/.../Expression.java` | The expression being typed, and the rules that keep it well-formed |
| `app/src/main/java/.../Evaluator.java` | Recursive-descent parser and evaluator |
| `app/src/main/java/.../Numbers.java` | Result formatting and digit grouping |
| `app/src/test/java/...` | Unit tests for all three |
| `tools/make_icons.py` | Generates the launcher icons |

Keys carry their meaning in `android:tag`, so the keypad layout and the code
that reacts to it stay independent of resource ids.

## Building

The normal way, with the Android SDK installed:

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew test              # unit tests
```

### Building without the Android SDK

`tools/build-apk.sh` produces the same debug APK without the Android Gradle
Plugin, for machines that can't reach `dl.google.com`. It drives `aapt2`,
`javac`, `dx` and `apksigner` directly, taking `android.jar` and the dex
compiler from mirrors that don't need the SDK manager:

```bash
sudo apt-get install aapt apksigner zipalign   # Debian/Ubuntu
tools/build-apk.sh                             # build/calculator-debug.apk
tools/run-tests.sh                             # unit tests, no Gradle
```

Both scripts cache their downloads in `build/toolchain/`.

## Installing

```bash
adb install -r dist/calculator-debug.apk
```

The APK in `dist/` is debug-signed with a generated key, so it installs on a
device or emulator but is not suitable for distribution. For a release build,
sign `./gradlew assembleRelease` output with your own key.
