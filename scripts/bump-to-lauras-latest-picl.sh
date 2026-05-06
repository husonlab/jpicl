#!/usr/bin/env bash
#
# bump-to-lauras-latest-picl.sh
#
# Updates the native/picl git submodule to the latest commit on its
# tracked branch, shows the new commits, and records the new pointer
# in a jpicl commit.
#
# Usage:
#   ./bump-to-lauras-latest-picl.sh             # update + commit
#   ./bump-to-lauras-latest-picl.sh --dry-run   # show what would change, then revert
#   ./bump-to-lauras-latest-picl.sh --help
#
# Run from anywhere inside the jpicl repo. By default the submodule is
# expected at native/picl; override with PICL_SUBMODULE_PATH=...
#
# The submodule's tracked branch is whatever is configured in
# .gitmodules (submodule.<name>.branch). To pin to a specific branch:
#   git config -f .gitmodules submodule.native/picl.branch main
#   git submodule sync

set -euo pipefail

SUBMODULE_PATH="${PICL_SUBMODULE_PATH:-native/picl}"
DRY_RUN=0

for arg in "$@"; do
    case "$arg" in
        -n|--dry-run) DRY_RUN=1 ;;
        -h|--help)
            sed -n '2,18p' "$0" | sed 's/^# \{0,1\}//'
            exit 0 ;;
        *)
            echo "Unknown argument: $arg" >&2
            echo "Try: $(basename "$0") --help" >&2
            exit 2 ;;
    esac
done

# ---------------------------------------------------------------------
# Move to the jpicl repo root so all paths are unambiguous.
# ---------------------------------------------------------------------
if ! REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)"; then
    echo "Not inside a git repository." >&2
    exit 1
fi
cd "$REPO_ROOT"

if [[ ! -e "$SUBMODULE_PATH/.git" ]]; then
    echo "Submodule at '$SUBMODULE_PATH' is not initialised." >&2
    echo "Run:  git submodule update --init --recursive" >&2
    exit 1
fi

# ---------------------------------------------------------------------
# Refuse to run with a dirty working tree — we only want to commit the
# submodule pointer change, nothing else that happens to be lying around.
# (Changes inside the submodule itself are fine; --remote will overwrite
#  the submodule HEAD anyway.)
# ---------------------------------------------------------------------
if ! git diff --quiet -- ":!$SUBMODULE_PATH" \
   || ! git diff --cached --quiet -- ":!$SUBMODULE_PATH"; then
    echo "Working tree has uncommitted changes outside the submodule." >&2
    echo "Commit or stash them first:" >&2
    git status --short
    exit 1
fi

# ---------------------------------------------------------------------
# Fetch and fast-forward the submodule to the latest tracked commit.
# ---------------------------------------------------------------------
OLD_SHA="$(git -C "$SUBMODULE_PATH" rev-parse HEAD)"

echo "Fetching latest PICL from origin..."
git submodule update --init --remote --recursive "$SUBMODULE_PATH"

NEW_SHA="$(git -C "$SUBMODULE_PATH" rev-parse HEAD)"

if [[ "$OLD_SHA" == "$NEW_SHA" ]]; then
    echo "Already at the latest PICL commit ($(git -C "$SUBMODULE_PATH" rev-parse --short HEAD))."
    echo "Nothing to do."
    exit 0
fi

OLD_SHORT="$(git -C "$SUBMODULE_PATH" rev-parse --short "$OLD_SHA")"
NEW_SHORT="$(git -C "$SUBMODULE_PATH" rev-parse --short "$NEW_SHA")"

echo
echo "PICL: $OLD_SHORT  →  $NEW_SHORT"
echo
echo "New commits in PICL:"
git -C "$SUBMODULE_PATH" log --oneline --no-merges "$OLD_SHA..$NEW_SHA"
echo

# ---------------------------------------------------------------------
# Dry run: revert the submodule HEAD so the working tree is clean again.
# ---------------------------------------------------------------------
if [[ "$DRY_RUN" == 1 ]]; then
    echo "[dry-run] Reverting submodule pointer; no commit will be made."
    git -C "$SUBMODULE_PATH" checkout --quiet "$OLD_SHA"
    exit 0
fi

# ---------------------------------------------------------------------
# Stage and commit the new submodule pointer.
# ---------------------------------------------------------------------
git add "$SUBMODULE_PATH"
git commit -m "Bump PICL submodule to ${NEW_SHORT}

Was: ${OLD_SHORT}
Now: ${NEW_SHORT}

$(git -C "$SUBMODULE_PATH" log --oneline --no-merges "$OLD_SHA..$NEW_SHA" | sed 's/^/  /')"

echo
echo "Done. PICL is now pinned to ${NEW_SHORT} in jpicl."
echo "Recommended: run your build (mvn package or equivalent) before pushing."

