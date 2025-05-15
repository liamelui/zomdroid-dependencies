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

REPO_NAME="assimp"
REPO_DIR="$REPOS_DIR/$REPO_NAME"
PATCH_DIR="$PATCHES_DIR/$REPO_NAME"
BUILD_DIR="$REPO_DIR/build-android-$ANDROID_ABI"
ASSIMP_TAG="v5.4.3"

mkdir -p "$REPOS_DIR"
cd "$REPOS_DIR"

echo "==> Cloning assimp repository..."
git clone https://github.com/assimp/assimp.git $REPO_NAME

cd "$REPO_DIR"

git checkout "$ASSIMP_TAG"
git checkout -b zomdroid

echo "==> Applying assimp patches..."
git apply "$PATCH_DIR"/*.patch || {
    echo "Error applying patches"
    exit 1
}

mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

echo "==> Configuring assimp..."

cmake .. \
  -DBUILD_SHARED_LIBS=OFF \
  -DASSIMP_BUILD_TESTS=OFF \
  -DASSIMP_INSTALL=ON \
  -DASSIMP_NO_EXPORT=ON \
  -DASSIMP_BUILD_ALL_IMPORTERS_BY_DEFAULT=OFF \
  -DASSIMP_BUILD_FBX_IMPORTER=ON \
  -DASSIMP_BUILD_GLTF_IMPORTER=ON \
  -DASSIMP_BUILD_X_IMPORTER=ON \
  -DBUILD_JASSIMP=ON \
  -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN_FILE_CMAKE" \
  -DANDROID_NDK="$ANDROID_NDK_HOME" \
  -DANDROID_ABI="$ANDROID_ABI" \
  -DANDROID_PLATFORM="android-$ANDROID_API_LEVEL" \
  -DCMAKE_BUILD_TYPE="$BUILD_TYPE_CMAKE"

echo "==> Building jassimp..."

cmake --build . --parallel "$(nproc)"

if [ "$BUILD_TYPE_CMAKE" = "Release" ]; then
  STRIP_BIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"

  echo "==> Stripping libjassimp64.so..."
  "$STRIP_BIN" --strip-unneeded "libjassimp64.so"
fi

mkdir -p "$TARGETS_DIR"

echo "==> Copying jassimp targets..."

cp -v "libjassimp64.so" "$TARGETS_DIR/"

