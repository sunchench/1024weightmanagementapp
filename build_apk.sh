#!/bin/bash
# 构建 weight management APK（所有依赖保留在当前目录，不占用 C 盘）
set -e
cd "$(dirname "$0")"

PROJECT_DIR="$(pwd)"
JH=$(cygpath -w "$PROJECT_DIR/toolchain/jdk/jdk-17.0.12+7")
AH=$(cygpath -w "$PROJECT_DIR/toolchain/android-sdk")
GH=$(cygpath -w "$PROJECT_DIR/toolchain/gradle-home")

export JAVA_HOME="$JH"
export ANDROID_HOME="$AH"
export ANDROID_SDK_ROOT="$AH"
export GRADLE_USER_HOME="$GH"

echo "JAVA_HOME=$JAVA_HOME"
echo "ANDROID_HOME=$ANDROID_HOME"
echo "GRADLE_USER_HOME=$GRADLE_USER_HOME"

./gradlew assembleDebug --console=plain --stacktrace "$@" 2>&1 | tee build.log

# 复制产物
OUT="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$OUT" ]; then
  cp "$OUT" "$PROJECT_DIR/weight management.apk"
  echo "已生成: $PROJECT_DIR/weight management.apk"
else
  echo "未找到构建产物: $OUT"
  exit 1
fi
