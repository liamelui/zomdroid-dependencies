#!/bin/bash
set -e

ROOT_DIR="$(pwd)"
TARGETS_DIR="$ROOT_DIR/targets/libs/linux-x86_64"

cd jniwrapper
mkdir build
cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --parallel "$(nproc)"
mkdir -p "$TARGETS_DIR"
cp "libjniwrapper.so" "$TARGETS_DIR/"