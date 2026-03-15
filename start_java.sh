#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/config.properties"
LOG_DIR="${SCRIPT_DIR}/logs"
RUN_DIR="${SCRIPT_DIR}/run"
PID_FILE="${RUN_DIR}/java.pid"
LOG_FILE="${LOG_DIR}/java.log"

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
    echo "Java service is already running with PID ${EXISTING_PID}"
    echo "Log file: ${LOG_FILE}"
    exit 0
  fi
  rm -f "$PID_FILE"
fi

if [[ -f "${SCRIPT_DIR}/ACASB.jar" ]]; then
  JAR_PATH="${SCRIPT_DIR}/ACASB.jar"
elif [[ -f "${SCRIPT_DIR}/ACASB-0.0.1-SNAPSHOT.jar" ]]; then
  JAR_PATH="${SCRIPT_DIR}/ACASB-0.0.1-SNAPSHOT.jar"
else
  echo "Could not find ACASB.jar or ACASB-0.0.1-SNAPSHOT.jar."
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Java not found. Run ./install_linux.sh first."
  exit 1
fi

DB_HOST="$(read_prop "db.host" "127.0.0.1")"
DB_PORT="$(read_prop "db.port" "3306")"
DB_NAME="$(read_prop "db.name" "acasb")"
DB_USERNAME="$(read_prop "db.username" "root")"
DB_PASSWORD="$(read_prop "db.password" "")"
SERVER_PORT="$(read_prop "server.port" "8081")"
PYTHON_HOST="$(read_prop "python.host" "127.0.0.1")"
PYTHON_PORT="$(read_prop "python.port" "5000")"
TEMP_FOLDER="$(read_prop "temp.folder" "./temp")"
STORAGE_FOLDER="$(read_prop "storage.folder" "./uploads")"
DATASET_STORAGE_FOLDER="$(read_prop "dataset.storage.folder" "./dataset-storage")"
AI_ENABLED="$(read_prop "ai.analysis.enabled" "false")"
AI_BASE_URL="$(read_prop "ai.analysis.base-url" "https://api.openai.com")"
AI_INTERFACE="$(read_prop "ai.analysis.api-interface" "responses")"
AI_RESPONSES_PATH="$(read_prop "ai.analysis.responses-path" "/v1/responses")"
AI_CHAT_PATH="$(read_prop "ai.analysis.chat-completions-path" "/v1/chat/completions")"
AI_API_KEY="$(read_prop "ai.analysis.api-key" "")"
AI_MODEL="$(read_prop "ai.analysis.model" "gpt-4.1-mini")"
AUTH_JWT_ENABLED="$(read_prop "auth.jwt.enabled" "true")"
AUTH_JWT_SECRET="$(read_prop "auth.jwt.secret" "")"
AUTH_JWT_EXPIRES_HOURS="$(read_prop "auth.jwt.expires-hours" "720")"

if [[ "${AUTH_JWT_ENABLED,,}" == "true" ]] && [[ -z "$AUTH_JWT_SECRET" ]]; then
  echo "JWT auth is enabled but auth.jwt.secret is empty."
  echo "Run ./install_linux.sh first or set auth.jwt.secret in config.properties."
  exit 1
fi

SPRING_ARGS=(
  "--spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
  "--spring.datasource.username=${DB_USERNAME}"
  "--spring.datasource.password=${DB_PASSWORD}"
  "--server.port=${SERVER_PORT}"
  "--python.service.host=${PYTHON_HOST}"
  "--python.service.port=${PYTHON_PORT}"
  "--app.temp-folder=${TEMP_FOLDER}"
  "--app.storage-folder=${STORAGE_FOLDER}"
  "--app.dataset-storage-folder=${DATASET_STORAGE_FOLDER}"
  "--ai.analysis.enabled=${AI_ENABLED}"
  "--ai.analysis.base-url=${AI_BASE_URL}"
  "--ai.analysis.api-interface=${AI_INTERFACE}"
  "--ai.analysis.responses-path=${AI_RESPONSES_PATH}"
  "--ai.analysis.chat-completions-path=${AI_CHAT_PATH}"
  "--ai.analysis.api-key=${AI_API_KEY}"
  "--ai.analysis.model=${AI_MODEL}"
  "--auth.jwt.enabled=${AUTH_JWT_ENABLED}"
  "--auth.jwt.secret=${AUTH_JWT_SECRET}"
  "--auth.jwt.expires-hours=${AUTH_JWT_EXPIRES_HOURS}"
)

nohup java -jar "$JAR_PATH" "${SPRING_ARGS[@]}" >"$LOG_FILE" 2>&1 &
JAVA_PID=$!
echo "$JAVA_PID" >"$PID_FILE"

sleep 4
if ! kill -0 "$JAVA_PID" 2>/dev/null; then
  echo "Java service failed to start. Check ${LOG_FILE}"
  rm -f "$PID_FILE"
  exit 1
fi

echo "Java service started"
echo "PID: ${JAVA_PID}"
echo "Java service port: ${SERVER_PORT}"
echo "Database: ${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "Python service: ${PYTHON_HOST}:${PYTHON_PORT}"
echo "JWT auth enabled: ${AUTH_JWT_ENABLED}"
echo "Log file: ${LOG_FILE}"
echo
echo "Recent startup log:"
tail -n 20 "$LOG_FILE"
