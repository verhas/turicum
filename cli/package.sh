#!/bin/sh

PYTHON=$(command -v python3 || command -v python || command -v py || command -v pythong)

if [ -z "$PYTHON" ]; then
  echo "No Python interpreter found." >&2
  exit 1
fi

exec "$PYTHON" package.py