#!/bin/bash
set -e

check_env_var() {
    if [ -z "${!1}" ]; then
        echo "Error: $1 is not set."
        exit 1
    fi
}

check_env_var "ANDROID_NDK_HOME"
check_env_var "TOOLCHAIN_FILE_CMAKE"
check_env_var "ANDROID_ABI"
check_env_var "ANDROID_API_LEVEL"
check_env_var "BUILD_TYPE_CMAKE"

ROOT_DIR="$(pwd)"
REPOS_DIR="$ROOT_DIR/repos"
PATCHES_DIR="$ROOT_DIR/patches"
TARGETS_DIR="$ROOT_DIR/targets/libs/android-$ANDROID_ABI"

REPO_NAME="gl4es"
REPO_DIR="$REPOS_DIR/$REPO_NAME"
PATCH_DIR="$PATCHES_DIR/$REPO_NAME"
BUILD_DIR="$REPO_DIR/build-android-$ANDROID_ABI"
GL4ES_COMMIT="a744af14d4afbda77bf472bc53f43b9ceba39cc0"

mkdir -p "$REPOS_DIR"
cd "$REPOS_DIR"

echo "==> Cloning gl4es repository..."
git clone https://github.com/ptitSeb/gl4es.git $REPO_NAME

cd "$REPO_DIR"

git checkout "$GL4ES_COMMIT"
git checkout -b zomdroid

echo "==> Applying gl4es patches..."
git apply "$PATCH_DIR"/*.patch || {
    echo "Error applying patches"
    exit 1
}

mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

echo "==> Configuring gl4es..."

cmake .. \
  -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN_FILE_CMAKE" \
  -DANDROID_NDK="$ANDROID_NDK_HOME" \
  -DANDROID_ABI="$ANDROID_ABI" \
  -DANDROID_PLATFORM="android-$ANDROID_API_LEVEL" \
  -DCMAKE_BUILD_TYPE="$BUILD_TYPE_CMAKE"

echo "==> Building gl4es..."

cmake --build . --parallel "$(nproc)"

if [ "$BUILD_TYPE_CMAKE" = "Release" ]; then
  STRIP_BIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"

  echo "==> Stripping libgl4es.so..."
  "$STRIP_BIN" --strip-unneeded "libgl4es.so"
fi

mkdir -p "$TARGETS_DIR"

echo "==> Copying gl4es targets..."

cp -v "libgl4es.so" "$TARGETS_DIR/"