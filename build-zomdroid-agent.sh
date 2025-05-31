#!/bin/bash
set -e

ROOT_DIR="$(pwd)"
TARGETS_DIR="$ROOT_DIR/targets/jars"

cd zomdroid-agent
mvn clean package
mkdir -p "$TARGETS_DIR"
cp "target/zomdroid-agent.jar" "$TARGETS_DIR/"