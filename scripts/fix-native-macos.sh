#!/bin/bash

#
# fix-native-macos.sh Copyright (C) 2026 Daniel H. Huson
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

# Remove macOS quarantine attributes from local development binaries
# and ensure they are executable.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "Project root: $ROOT"

find "$ROOT/packaging/native" -type f | while read -r file; do
    echo "Processing: $file"

    chmod +x "$file" || true

    if xattr "$file" 2>/dev/null | grep -q "com.apple.quarantine"; then
        echo "  Removing quarantine attribute"
        xattr -dr com.apple.quarantine "$file"
    else
        echo "  No quarantine attribute present"
    fi
done

echo
echo "Done."