#!/usr/bin/env bash

set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: $0 <version> [repo]"
    echo "Example:"
    echo "  $0 1.0.1"
    echo "  $0 1.0.1 husonlab/jpicl-updates"
    exit 1
fi

VERSION="$1"
TAG="v$VERSION"

if [[ $# -eq 2 ]]; then
    REPO="$2"
else
    REPO=""
fi

echo "Removing release and tag:"
echo "  $TAG"

if [[ -n "$REPO" ]]; then
    echo "Repository: $REPO"
fi

echo

if [[ -n "$REPO" ]]; then
    if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
        echo "Deleting GitHub release..."
        gh release delete "$TAG" --repo "$REPO" --yes
    else
        echo "No GitHub release found for $TAG in $REPO"
    fi
else
    if gh release view "$TAG" >/dev/null 2>&1; then
        echo "Deleting GitHub release..."
        gh release delete "$TAG" --yes
    else
        echo "No GitHub release found for $TAG"
    fi
fi

if git rev-parse "$TAG" >/dev/null 2>&1; then
    echo "Deleting local tag..."
    git tag -d "$TAG"
else
    echo "No local tag found for $TAG"
fi

if git ls-remote --tags origin "$TAG" | grep -q "$TAG"; then
    echo "Deleting remote tag..."
    git push origin ":refs/tags/$TAG"
else
    echo "No remote tag found for $TAG"
fi

echo
echo "Done."