#!/bin/zsh
set -e

SCRIPT_DIR="${0:A:h}"

cd $SCRIPT_DIR/..
python3 -m http.server 8000
