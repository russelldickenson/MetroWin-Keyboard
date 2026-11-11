#!/usr/bin/env bash
# Builds a signed release APK into the top-level directory of the project.
#
# Credentials are placeholders. Replace them (environment variables or a keystore.properties of your own) before publishing anything:
# a key made from the values below is only good for testing, because anyone can read its password here.
#
#   KEYSTORE_FILE      path of the keystore            (default: signing/lumina-release.jks)
#   KEYSTORE_PASSWORD  password of the keystore        (default: CHANGE_ME)
#   KEY_ALIAS          alias of the key in it          (default: lumina)
#   KEY_PASSWORD       password of the key             (default: CHANGE_ME)
#
# If keystore.properties exists it is used as it is. Otherwise the script creates the keystore (if it is missing) and writes
# keystore.properties from the values above. Both are ignored by git.
set -euo pipefail
cd "$(dirname "$0")"

KEYSTORE_FILE="${KEYSTORE_FILE:-signing/lumina-release.jks}"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-CHANGE_ME}"
KEY_ALIAS="${KEY_ALIAS:-lumina}"
KEY_PASSWORD="${KEY_PASSWORD:-CHANGE_ME}"

if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/Library/Android/sdk" ]]; then export ANDROID_HOME="$HOME/Library/Android/sdk"; fi
if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" && ! -f local.properties ]]; then
  echo "Set ANDROID_HOME to the Android SDK, or put sdk.dir in local.properties." >&2; exit 1
fi

if [[ ! -f keystore.properties ]]; then
  if [[ ! -f "$KEYSTORE_FILE" ]]; then
    echo "Creating the keystore $KEYSTORE_FILE (placeholder credentials: for testing only)"
    mkdir -p "$(dirname "$KEYSTORE_FILE")"
    keytool -genkeypair -v -keystore "$KEYSTORE_FILE" -alias "$KEY_ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
      -storepass "$KEYSTORE_PASSWORD" -keypass "$KEY_PASSWORD" -dname "CN=Lumina Keyboard, O=Lumina, C=US" >/dev/null
  fi
  printf 'storeFile=%s\nstorePassword=%s\nkeyAlias=%s\nkeyPassword=%s\n' "$KEYSTORE_FILE" "$KEYSTORE_PASSWORD" "$KEY_ALIAS" "$KEY_PASSWORD" > keystore.properties
  echo "Wrote keystore.properties"
fi

./gradlew --console=plain assembleRelease

version="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts | head -1)"
out="LuminaKeyboard-${version}.apk"
cp app/build/outputs/apk/release/app-release.apk "$out"

apksigner="$(ls "${ANDROID_HOME:-}"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1 || true)"
if [[ -n "$apksigner" ]]; then "$apksigner" verify --print-certs "$out" | head -3; else echo "apksigner not found: signature not verified"; fi
echo "Built $out"
