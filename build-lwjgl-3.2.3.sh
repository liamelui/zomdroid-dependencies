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

LWJGL_REPO_NAME="lwjgl-3.2.3"
LWJGL_REPO_DIR="$REPOS_DIR/$LWJGL_REPO_NAME"
LWJGL_PATCHES_DIR="$PATCHES_DIR/$LWJGL_REPO_NAME"
LWJGL_BUILD_DIR="$LWJGL_REPO_DIR/build-android-$ANDROID_ABI"
LWJGL_TAG="3.2.3"
LWJGL_TARGET_DIR="$TARGETS_DIR/$LWJGL_REPO_NAME"

DYNCALL_REPO_NAME="dyncall"

mkdir -p "$REPOS_DIR"
cd "$REPOS_DIR"

echo "==> Cloning lwjgl repository..."
git clone https://github.com/LWJGL/lwjgl3.git $LWJGL_REPO_NAME

cd "$LWJGL_REPO_DIR"

git checkout "$LWJGL_TAG"
git checkout -b zomdroid

echo "==> Cloning dyncall repository..."
git clone https://github.com/LWJGL-CI/dyncall.git $DYNCALL_REPO_NAME

echo "==> Applying lwjgl patches..."
git apply "$LWJGL_PATCHES_DIR"/*.patch || {
    echo "Error applying patches"
    exit 1
}

mkdir -p "$LWJGL_BUILD_DIR"
cd "$LWJGL_BUILD_DIR"

echo "==> Configuring lwjgl..."

cmake .. \
  -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN_FILE_CMAKE" \
  -DANDROID_NDK="$ANDROID_NDK_HOME" \
  -DANDROID_ABI="$ANDROID_ABI" \
  -DANDROID_PLATFORM="android-$ANDROID_API_LEVEL" \
  -DCMAKE_BUILD_TYPE="$BUILD_TYPE_CMAKE"

echo "==> Building lwjgl..."

cmake --build . --parallel "$(nproc)"

if [ "$BUILD_TYPE_CMAKE" = "Release" ]; then
    STRIP_BIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"

    for sofile in ./liblwjgl*.so; do
        echo "==> Stripping $sofile..."
        "$STRIP_BIN" --strip-unneeded "$sofile"
    done
fi

mkdir -p "$LWJGL_TARGET_DIR"

echo "==> Copying lwjgl targets..."

cp -v liblwjgl*.so "$LWJGL_TARGET_DIR/"

