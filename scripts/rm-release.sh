#!/usr/bin/env bash

set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: $0 <version> [release-repo]"
    echo "Example:"
    echo "  $0 1.0.1"
    echo "  $0 1.0.1 husonlab/jpicl-updates"
    exit 1
fi

VERSION="$1"
TAG="v$VERSION"
RELEASE_REPO="${2:-}"

echo "Removing release/tag for $TAG"

if [[ -n "$RELEASE_REPO" ]]; then
    if gh release view "$TAG" --repo "$RELEASE_REPO" >/dev/null 2>&1; then
        gh release delete "$TAG" --repo "$RELEASE_REPO" --yes
    else
        echo "No release found in $RELEASE_REPO"
    fi
else
    if gh release view "$TAG" >/dev/null 2>&1; then
        gh release delete "$TAG" --yes
    else
        echo "No release found in current repo"
    fi
fi

if git rev-parse "$TAG" >/dev/null 2>&1; then
    git tag -d "$TAG"
else
    echo "No local tag $TAG"
fi

if git ls-remote --tags origin "$TAG" | grep -q "$TAG"; then
    git push origin ":refs/tags/$TAG"
else
    echo "No remote tag $TAG on origin"
fi

echo "Done."