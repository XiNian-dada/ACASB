#!/bin/zsh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/acasb-analysis"
CONFIG_FILE="$SCRIPT_DIR/config.properties"
cd "$PROJECT_DIR"

read_prop() {
  local key="$1"
  local default_value="$2"
  if [[ ! -f "$CONFIG_FILE" ]]; then
    printf '%s' "$default_value"
    return
  fi

  local value
  value="$(awk -F= -v wanted="$key" '$1==wanted {sub(/^[^=]*=/, "", $0); print $0; exit}' "$CONFIG_FILE" | tr -d '\r')"
  if [[ -n "$value" ]]; then
    printf '%s' "$value"
  else
    printf '%s' "$default_value"
  fi
}

echo "Starting Python API Service..."
echo

PYTHON_HOST="$(read_prop "python.host" "127.0.0.1")"
PYTHON_PORT="$(read_prop "python.port" "5000")"
export ACASB_PYTHON_HOST="$PYTHON_HOST"
export ACASB_PYTHON_PORT="$PYTHON_PORT"

echo "Python API target: ${PYTHON_HOST}:${PYTHON_PORT}"
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

echo "Starting ACASB Python API on port ${PYTHON_PORT}..."
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
