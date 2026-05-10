#!/bin/zsh
set -e

SCRIPT_DIR="${0:A:h}"

case "$(uname -s)-$(uname -m)" in
    Darwin-arm64)   PLATFORM_KEY="macos-aarch64" ;;
    Darwin-x86_64)  PLATFORM_KEY="macos-x86_64"  ;;
    Linux-x86_64)   PLATFORM_KEY="linux-x86_64"  ;;
    Linux-aarch64)  PLATFORM_KEY="linux-aarch64" ;;
    *) echo "Unknown platform: $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

DEST_DIR="$SCRIPT_DIR/../src/main/resources/native/$PLATFORM_KEY"
mkdir -p "$DEST_DIR"

cd "$SCRIPT_DIR/../native/picl/src"
gcc main.c -O2 -lm -o picl
mv picl "$DEST_DIR/picl"

echo "Built picl ($PLATFORM_KEY) → $DEST_DIR/picl"