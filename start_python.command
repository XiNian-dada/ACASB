#!/bin/zsh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/acasb-analysis"
cd "$PROJECT_DIR"

echo "Starting Python API Service..."
echo

if [[ -x "$PROJECT_DIR/.venv/bin/python" ]]; then
  PYTHON_BIN="$PROJECT_DIR/.venv/bin/python"
elif command -v python3 >/dev/null 2>&1; then
  PYTHON_BIN="$(command -v python3)"
elif command -v python >/dev/null 2>&1; then
  PYTHON_BIN="$(command -v python)"
else
  echo "Python 3 not found. Install Python 3.8 or newer and try again."
  echo
  read -r "?Press Enter to close..."
  exit 1
fi

echo "Using Python: $PYTHON_BIN"
"$PYTHON_BIN" --version
echo

if ! "$PYTHON_BIN" -c "import cv2, fastapi, joblib, multipart, numpy, pandas, pydantic, skimage, sklearn, uvicorn" >/dev/null 2>&1; then
  echo "Required Python packages are missing."
  echo "Install them with:"
  echo "  \"$PYTHON_BIN\" -m pip install -r \"$PROJECT_DIR/requirements.txt\""
  echo
  read -r "?Press Enter to close..."
  exit 1
fi

echo "Starting ACASB Python API on port 5000..."
"$PYTHON_BIN" api_server.py
STATUS=$?

echo
if [[ $STATUS -eq 0 ]]; then
  echo "Python service stopped."
else
  echo "Python service exited with code $STATUS."
fi

read -r "?Press Enter to close..."
exit $STATUS
