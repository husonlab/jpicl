#!/bin/zsh
set -e

# Resolve the script's own directory and the project root.
SCRIPT_DIR="${0:A:h}"
PROJECT_ROOT="$SCRIPT_DIR/.."

# 1. Compile PICL via the shared core script.
"$SCRIPT_DIR/compile-picl-core.sh"

# 2. Detect platform key matching what PiclExtractor expects.
case "$(uname -s)-$(uname -m)" in
    Darwin-arm64)   PLATFORM_KEY="macos-aarch64" ;;
    Darwin-x86_64)  PLATFORM_KEY="macos-x86_64"  ;;
    Linux-x86_64)   PLATFORM_KEY="linux-x86_64"  ;;
    Linux-aarch64)  PLATFORM_KEY="linux-aarch64" ;;
    *) echo "Unknown platform: $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

# 3. Copy the binary into JAR resources so it gets bundled at package time.
DEST_DIR="$PROJECT_ROOT/src/main/resources/native/$PLATFORM_KEY"
mkdir -p "$DEST_DIR"
cp picl "$DEST_DIR/picl"

echo "Built picl ($PLATFORM_KEY) → $DEST_DIR/picl"
