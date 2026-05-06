#!/bin/zsh
set -e

# Resolve the directory containing this script, regardless of where it
# was invoked from.  ${0:A:h} = absolute path of $0, with the last
# component stripped — i.e. the script's own directory.
SCRIPT_DIR="${0:A:h}"

cd "$SCRIPT_DIR/../native/picl/src"
gcc main.c -lm -o picl
