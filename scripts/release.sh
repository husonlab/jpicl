#!/usr/bin/env bash

#
# release.sh Copyright (C) 2026 Daniel H. Huson
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

set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.0"
    exit 1
fi

VERSION="$1"
TAG="v$VERSION"
NOTES="release-notes/$TAG.md"

if [[ ! "$VERSION" =~ ^[0-9]+(\.[0-9]+){1,2}([.-][A-Za-z0-9]+)?$ ]]; then
    echo "Error: version does not look valid: $VERSION"
    echo "Use something like: 1.0.0"
    exit 1
fi

if [[ ! -f "$NOTES" ]]; then
    echo "Error: release notes file missing:"
    echo "  $NOTES"
    exit 1
fi

if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "Error: working tree has uncommitted changes."
    echo "Commit or stash them before releasing."
    exit 1
fi

git fetch --tags

if git rev-parse "$TAG" >/dev/null 2>&1; then
    echo "Error: tag already exists locally: $TAG"
    exit 1
fi

if git ls-remote --tags origin "$TAG" | grep -q "$TAG"; then
    echo "Error: tag already exists on origin: $TAG"
    exit 1
fi

echo "Release notes found:"
echo "  $NOTES"
echo
echo "Creating and pushing tag:"
echo "  $TAG"
echo

git tag -a "$TAG" -F "$NOTES"
git push origin "$TAG"

echo
echo "Done. GitHub Actions should now build the installers and create the release."