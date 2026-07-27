#!/bin/bash

# update-picl-submodule.sh
#
# Updates the PICL submodule to the latest upstream commit
# and commits the changed gitlink in the parent repository.
#
# Run this from anywhere inside the jpicl repository.

set -e

SUBMODULE="native/picl"
BRANCH="main"

# move to top-level git directory
ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"

echo "Repository root: $ROOT"
echo "Updating submodule: $SUBMODULE"

# ensure submodule exists
if [ ! -d "$SUBMODULE/.git" ] && [ ! -f "$SUBMODULE/.git" ]; then
    echo "Submodule not initialized, initializing..."
    git submodule update --init --recursive
fi

# remember old commit
OLD=$(git -C "$SUBMODULE" rev-parse --short HEAD)

# update submodule
git -C "$SUBMODULE" fetch origin
git -C "$SUBMODULE" checkout "$BRANCH"
git -C "$SUBMODULE" pull origin "$BRANCH"

# new commit
NEW=$(git -C "$SUBMODULE" rev-parse --short HEAD)

echo "Old PICL commit: $OLD"
echo "New PICL commit: $NEW"

# check whether anything changed
if [ "$OLD" = "$NEW" ]; then
    echo "Submodule already up to date."
    exit 0
fi

# commit updated gitlink
git add "$SUBMODULE"

git commit -m "Update PICL submodule to $NEW"

echo
echo "Submodule updated successfully."
echo
echo "To publish:"
echo "    git push origin main"
echo "then run the *build picl binaries* workflow before releasing"
