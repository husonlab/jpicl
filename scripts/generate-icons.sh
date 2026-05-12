#!/usr/bin/env bash
#
# generate-icons.sh Copyright (C) 2026 Daniel H. Huson
#
#  (Some files contain contributions from other authors, who are then mentioned separately.)
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
#

#
# generate-icons.sh — produce platform-specific icon files from a
# single source PNG, ready for jpackage --icon to consume.
#
# Usage:
#   ./scripts/generate-icons.sh [SOURCE_PNG]
#
# Default source: src/main/resources/jpicl/main/PICL-512.png
#
# Output:
#   packaging/mac/PICL.icns       (macOS  — multi-resolution .icns)
#   packaging/windows/PICL.ico    (Windows — multi-resolution .ico)
#   packaging/linux/PICL.png      (Linux  — single PNG, copied through)
#
# Requires (run on macOS — outputs are committed to the repo, then
# the GitHub workflow uses them on every platform):
#   - sips      built into macOS
#   - iconutil  built into macOS
#   - magick    brew install imagemagick   (for .ico)
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."

SOURCE="${1:-$PROJECT_ROOT/src/main/resources/jpicl/main/PICL-512.png}"

if [[ ! -f "$SOURCE" ]]; then
    echo "Source PNG not found: $SOURCE" >&2
    exit 1
fi

PACKAGING="$PROJECT_ROOT/packaging"
MAC_DIR="$PACKAGING/mac"
WIN_DIR="$PACKAGING/windows"
LIN_DIR="$PACKAGING/linux"
mkdir -p "$MAC_DIR" "$WIN_DIR" "$LIN_DIR"

# ----------------------------------------------------------------------
# macOS .icns
#
# .icns is a container of pre-rendered PNGs at specific sizes. The
# conventional way is to build an .iconset directory with the @1x and
# @2x variants of each base size, then iconutil compiles it.
# ----------------------------------------------------------------------
echo "Generating macOS icon…"
ICONSET="$(mktemp -d)/PICL.iconset"
mkdir -p "$ICONSET"

resize_to() {
    local size="$1"; local name="$2"
    sips -z "$size" "$size" "$SOURCE" --out "$ICONSET/$name" >/dev/null
}

resize_to   16  icon_16x16.png
resize_to   32  icon_16x16@2x.png
resize_to   32  icon_32x32.png
resize_to   64  icon_32x32@2x.png
resize_to  128  icon_128x128.png
resize_to  256  icon_128x128@2x.png
resize_to  256  icon_256x256.png
resize_to  512  icon_256x256@2x.png
resize_to  512  icon_512x512.png
resize_to 1024  icon_512x512@2x.png

iconutil -c icns "$ICONSET" -o "$MAC_DIR/PICL.icns"
rm -rf "$(dirname "$ICONSET")"
echo "  → $MAC_DIR/PICL.icns"

# ----------------------------------------------------------------------
# Windows .ico
#
# Single .ico containing multiple resolutions; Windows picks the best
# fit for whatever icon view it needs. 256/128/64/48/32/16 is the
# usual set; ImageMagick generates them all in one shot.
# ----------------------------------------------------------------------
echo "Generating Windows icon…"
if ! command -v magick >/dev/null 2>&1; then
    echo "ImageMagick 'magick' not found." >&2
    echo "Install with: brew install imagemagick" >&2
    exit 1
fi
magick "$SOURCE" -define icon:auto-resize=256,128,64,48,32,16 "$WIN_DIR/PICL.ico"
echo "  → $WIN_DIR/PICL.ico"

# ----------------------------------------------------------------------
# Linux PNG
#
# jpackage --type deb expects a single PNG, ideally ≥256×256. The
# 512×512 source is plenty — copy it through unchanged so the colour
# profile and any embedded metadata are preserved exactly.
# ----------------------------------------------------------------------
echo "Generating Linux icon…"
cp "$SOURCE" "$LIN_DIR/PICL.png"
echo "  → $LIN_DIR/PICL.png"

echo
echo "Done. Re-run whenever $SOURCE changes; commit the packaging/ outputs."
