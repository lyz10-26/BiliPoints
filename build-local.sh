#!/usr/bin/env bash
set -euo pipefail
project="$(cd "$(dirname "$0")" && pwd)"
toolchain="${ANDROID_LOCAL_TOOLS:?Set ANDROID_LOCAL_TOOLS to Android build-tools/ecj toolchain root}"
build="$project/build"; out="$project/../out"
python3 - "$build" <<'CLEAN'
from pathlib import Path
import shutil,sys
for name in ('classes','dex'):
 shutil.rmtree(Path(sys.argv[1])/name,ignore_errors=True)
CLEAN
mkdir -p "$build/classes" "$build/dex" "$out"
java -jar "$toolchain/ecj.jar" -8 -warn:none -bootclasspath "$toolchain/android-35/android.jar:$toolchain/android-15/core-lambda-stubs.jar" -d "$build/classes" "$project"/app/src/main/java/cn/pi/bilipoints/*.java
"$toolchain/android-15/aapt" package -f -M "$project/app/src/main/AndroidManifest.xml" -S "$project/app/src/main/res" -I "$toolchain/android-35/android.jar" -F "$build/unsigned.apk"
python3 - "$build" <<'PY'
import zipfile,pathlib,sys
p=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(p/'classes.jar','w') as z:
 for f in (p/'classes').rglob('*.class'):z.write(f,str(f.relative_to(p/'classes')))
PY
"$toolchain/android-15/d8" --min-api 26 --lib "$toolchain/android-35/android.jar" --output "$build/dex" "$build/classes.jar"
(cd "$build/dex" && "$toolchain/android-15/aapt" add "$build/unsigned.apk" classes*.dex)
"$toolchain/android-15/zipalign" -f -p 4 "$build/unsigned.apk" "$build/aligned.apk"
if [ ! -f "$build/test-signing.jks" ]; then
 keytool -genkeypair -keystore "$build/test-signing.jks" -storepass android -keypass android -alias beta -keyalg RSA -keysize 2048 -validity 3650 -dname "CN=BiliPoints Local Test" >/dev/null 2>&1
fi
"$toolchain/android-15/apksigner" sign --ks "$build/test-signing.jks" --ks-pass pass:android --out "$out/BiliPoints-1.1.3-release.apk" "$build/aligned.apk"
"$toolchain/android-15/apksigner" verify --verbose "$out/BiliPoints-1.1.3-release.apk"
"$toolchain/android-15/zipalign" -c 4 "$out/BiliPoints-1.1.3-release.apk"
