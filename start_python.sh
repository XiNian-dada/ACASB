#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${SCRIPT_DIR}/acasb-analysis"
CONFIG_FILE="${SCRIPT_DIR}/config.properties"
LOG_DIR="${SCRIPT_DIR}/logs"
RUN_DIR="${SCRIPT_DIR}/run"
PID_FILE="${RUN_DIR}/python.pid"
LOG_FILE="${LOG_DIR}/python.log"

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

mkdir -p "$LOG_DIR" "$RUN_DIR"

if [[ -f "$PID_FILE" ]]; then
  EXISTING_PID="$(cat "$PID_FILE" 2>/dev/null || true)"
  if [[ -n "$EXISTING_PID" ]] && kill -0 "$EXISTING_PID" 2>/dev/null; then
    echo "Python service is already running with PID ${EXISTING_PID}"
    echo "Log file: ${LOG_FILE}"
    exit 0
  fi
  rm -f "$PID_FILE"
fi

PYTHON_HOST="$(read_prop "python.host" "127.0.0.1")"
PYTHON_PORT="$(read_prop "python.port" "5000")"

export ACASB_PYTHON_HOST="$PYTHON_HOST"
export ACASB_PYTHON_PORT="$PYTHON_PORT"

if [[ -x "${PROJECT_DIR}/.venv/bin/python" ]]; then
  PYTHON_BIN="${PROJECT_DIR}/.venv/bin/python"
elif command -v python3 >/dev/null 2>&1; then
  PYTHON_BIN="$(command -v python3)"
elif command -v python >/dev/null 2>&1; then
  PYTHON_BIN="$(command -v python)"
else
  echo "Python 3 not found. Run ./install_linux.sh first."
  exit 1
fi

cd "$PROJECT_DIR"

nohup "$PYTHON_BIN" api_server.py >"$LOG_FILE" 2>&1 &
PYTHON_PID=$!
echo "$PYTHON_PID" >"$PID_FILE"

sleep 2
if ! kill -0 "$PYTHON_PID" 2>/dev/null; then
  echo "Python service failed to start. Check ${LOG_FILE}"
  rm -f "$PID_FILE"
  exit 1
fi

echo "Python service started"
echo "PID: ${PYTHON_PID}"
echo "Target: ${PYTHON_HOST}:${PYTHON_PORT}"
echo "Log file: ${LOG_FILE}"
