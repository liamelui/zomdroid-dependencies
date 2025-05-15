#!/usr/bin/env bash
set -e

check_env_var() {
    if [ -z "${!1}" ]; then
        echo "Error: $1 is not set."
        exit 1
    fi
}

check_env_var "ANDROID_NDK_HOME"
check_env_var "ANDROID_ABI"
check_env_var "ANDROID_API_LEVEL"
check_env_var "BUILD_TYPE_CMAKE"
check_env_var "TARGET_TRIPLE"

cmake_to_meson_buildtype() {
    case "$1" in
        Release) echo "release" ;;
        Debug) echo "debug" ;;
        RelWithDebInfo) echo "debugoptimized" ;;
        MinSizeRel) echo "minsize" ;;
        *)
            echo "Error: Unknown CMake build type '$1'"
            exit 1
            ;;
    esac
}

ROOT_DIR="$(pwd)"
REPOS_DIR="$ROOT_DIR/repos"
PATCHES_DIR="$ROOT_DIR/patches"
TARGETS_DIR="$ROOT_DIR/targets/libs/android-$ANDROID_ABI"

MESON_BUILD_TYPE=$(cmake_to_meson_buildtype "$BUILD_TYPE_CMAKE")

REPO_NAME="mesa"
REPO_DIR="$REPOS_DIR/$REPO_NAME"
PATCH_DIR="$PATCHES_DIR/$REPO_NAME"
BUILD_DIR="$REPO_DIR/build-android-$ANDROID_ABI"
CROSS_FILE="$REPO_DIR/cross-android-$ANDROID_ABI"
MESA_TAG="mesa-25.0.2"

mkdir -p "$REPOS_DIR"
cd "$REPOS_DIR"

echo "==> Cloning repository..."
git clone https://gitlab.freedesktop.org/mesa/mesa.git

cd "$REPO_NAME"

git checkout "$MESA_TAG"
git checkout -b zomdroid

echo "==> Applying patches..."
git apply "$PATCH_DIR"/*.patch || {
    echo "Error applying patches"
    exit 1
}

echo "==> Setting up cross-file"
sed -i "s|{ANDROID_NDK_HOME}|$ANDROID_NDK_HOME|g" "$CROSS_FILE"
sed -i "s|{TARGET_TRIPLE}|$TARGET_TRIPLE|g" "$CROSS_FILE"
sed -i "s|{ANDROID_API_LEVEL}|$ANDROID_API_LEVEL|g" "$CROSS_FILE"

mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

echo "==> Configuring mesa..."

meson setup . "$REPO_DIR" \
  --cross-file "$CROSS_FILE" \
  -Dplatforms=android \
  -Dplatform-sdk-version="$ANDROID_API_LEVEL" \
  -Dandroid-stub=true \
  -Dandroid-libbacktrace=disabled \
  -Dandroid-strict=true \
  -Dxlib-lease=disabled \
  -Degl=disabled \
  -Dgbm=disabled \
  -Dllvm=disabled \
  -Dgallium-xa=disabled \
  -Dopengl=true \
  -Dvulkan-drivers=freedreno \
  -Dfreedreno-kmds=kgsl \
  -Dosmesa=true \
  -Dzfa=true \
  -Dgallium-drivers=zink \
  -Dshared-glapi=disabled \
  -Dbuildtype="$MESON_BUILD_TYPE"

echo "==> Building mesa..."

meson compile -C .

if [ "$BUILD_TYPE_CMAKE" = "Release" ]; then
  STRIP_BIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"

  echo "==> Stripping libvulkan_freedreno.so..."
  "$STRIP_BIN" --strip-unneeded "src/freedreno/vulkan/libvulkan_freedreno.so"

  echo "==> Stripping libOSMesa.so..."
  "$STRIP_BIN" --strip-unneeded "src/gallium/targets/osmesa/libOSMesa.so"

  echo "==> Stripping libzfa.so..."
  "$STRIP_BIN" --strip-unneeded "src/gallium/targets/zfa/libzfa.so"
fi

mkdir -p "$TARGETS_DIR"

echo "==> Copying mesa targets..."

cp -v "src/freedreno/vulkan/libvulkan_freedreno.so" "$TARGETS_DIR/"
cp -v "src/gallium/targets/osmesa/libOSMesa.so" "$TARGETS_DIR/"
cp -v "src/gallium/targets/zfa/libzfa.so" "$TARGETS_DIR/"