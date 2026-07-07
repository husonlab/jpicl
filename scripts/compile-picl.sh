#!/bin/zsh
set -e

SCRIPT_DIR="${0:A:h}"

SRC_DIR="$SCRIPT_DIR/../native/picl/src"

echo "Building PICL in $SRC_DIR"
make -C "$SRC_DIR"
