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
check_env_var "TOOLCHAIN"
check_env_var "ANDROID_ABI"
check_env_var "ANDROID_API_LEVEL"
check_env_var "BUILD_TYPE_CMAKE"
check_env_var "TARGET_TRIPLE"

ROOT_DIR="$(pwd)"
REPOS_DIR="$ROOT_DIR/repos"
PATCHES_DIR="$ROOT_DIR/patches"
TARGETS_DIR="$ROOT_DIR/targets/libs/android-$ANDROID_ABI"

LWJGL_REPO_NAME="lwjgl-3.3.6"
LWJGL_REPO_DIR="$REPOS_DIR/$LWJGL_REPO_NAME"
LWJGL_PATCHES_DIR="$PATCHES_DIR/$LWJGL_REPO_NAME"
LWJGL_BUILD_DIR="$LWJGL_REPO_DIR/build-android-$ANDROID_ABI"
LWJGL_TAG="3.3.6"
LWJGL_TARGET_DIR="$TARGETS_DIR/$LWJGL_REPO_NAME"

LIBFFI_DIR_NAME="libffi"
LIBFFI_DIR="$LWJGL_REPO_DIR/$LIBFFI_DIR_NAME"
LIBFFI_BUILD_DIR="$LIBFFI_DIR/build-android-$ANDROID_ABI"
LIBFFI_VERSION="3.4.8"

mkdir -p "$REPOS_DIR"
cd "$REPOS_DIR"

echo "==> Cloning lwjgl repository..."
git clone https://github.com/LWJGL/lwjgl3.git $LWJGL_REPO_NAME

cd "$LWJGL_REPO_DIR"

git checkout "$LWJGL_TAG"
git checkout -b zomdroid

echo "==> Downloading libffi source..."

wget https://github.com/libffi/libffi/releases/download/v$LIBFFI_VERSION/libffi-$LIBFFI_VERSION.tar.gz
tar xvf libffi-$LIBFFI_VERSION.tar.gz
mv libffi-$LIBFFI_VERSION $LIBFFI_DIR_NAME
cd "$LIBFFI_DIR"

echo "==> Configuring libffi..."

mkdir -p "$LIBFFI_BUILD_DIR"
cd "$LIBFFI_BUILD_DIR"

../configure \
  --host=$TARGET_TRIPLE \
  --disable-shared \
  --enable-static \
  --disable-docs \
  --with-sysroot=$TOOLCHAIN/sysroot \
  CC=$TOOLCHAIN/bin/${TARGET_TRIPLE}${ANDROID_API_LEVEL}-clang \
  AR=$TOOLCHAIN/bin/llvm-ar \
  RANLIB=$TOOLCHAIN/bin/llvm-ranlib \
  STRIP=$TOOLCHAIN/bin/llvm-strip

echo "==> Building libffi..."

make "-j$(nproc)"

cd "$LWJGL_REPO_DIR"

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

echo "==> Copying libffi.a to lwjgl build dir..."
cp "$LIBFFI_BUILD_DIR/.libs/libffi.a" "$LWJGL_BUILD_DIR/libffi.a"

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