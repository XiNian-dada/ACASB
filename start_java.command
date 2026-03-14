#!/bin/zsh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"
CONFIG_FILE="$SCRIPT_DIR/config.properties"

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

echo "Starting Java Backend Service..."
echo

if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  if JAVA_HOME_CANDIDATE="$(/usr/libexec/java_home -v 17+ 2>/dev/null)"; then
    export JAVA_HOME="$JAVA_HOME_CANDIDATE"
  elif JAVA_HOME_CANDIDATE="$(/usr/libexec/java_home 2>/dev/null)"; then
    export JAVA_HOME="$JAVA_HOME_CANDIDATE"
  fi
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
  echo "Using Java from: $JAVA_HOME"
else
  echo "Using Java from PATH"
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Java not found. Install Java 17 or newer and try again."
  echo
  read -r "?Press Enter to close..."
  exit 1
fi

java -version
echo

DB_HOST="$(read_prop "db.host" "127.0.0.1")"
DB_PORT="$(read_prop "db.port" "3306")"
DB_NAME="$(read_prop "db.name" "acasb")"
DB_USERNAME="$(read_prop "db.username" "root")"
DB_PASSWORD="$(read_prop "db.password" "")"
PYTHON_HOST="$(read_prop "python.host" "localhost")"
PYTHON_PORT="$(read_prop "python.port" "5000")"
TEMP_FOLDER="$(read_prop "temp.folder" "./temp")"
STORAGE_FOLDER="$(read_prop "storage.folder" "./uploads")"
DATASET_STORAGE_FOLDER="$(read_prop "dataset.storage.folder" "./dataset-storage")"
AI_ENABLED="$(read_prop "ai.analysis.enabled" "false")"
AI_BASE_URL="$(read_prop "ai.analysis.base-url" "https://api.openai.com")"
AI_CHAT_PATH="$(read_prop "ai.analysis.chat-completions-path" "/v1/chat/completions")"
AI_API_KEY="$(read_prop "ai.analysis.api-key" "")"
AI_MODEL="$(read_prop "ai.analysis.model" "gpt-4.1-mini")"

SPRING_ARGS=(
  "--spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
  "--spring.datasource.username=${DB_USERNAME}"
  "--spring.datasource.password=${DB_PASSWORD}"
  "--python.service.host=${PYTHON_HOST}"
  "--python.service.port=${PYTHON_PORT}"
  "--app.temp-folder=${TEMP_FOLDER}"
  "--app.storage-folder=${STORAGE_FOLDER}"
  "--app.dataset-storage-folder=${DATASET_STORAGE_FOLDER}"
  "--ai.analysis.enabled=${AI_ENABLED}"
  "--ai.analysis.base-url=${AI_BASE_URL}"
  "--ai.analysis.chat-completions-path=${AI_CHAT_PATH}"
  "--ai.analysis.api-key=${AI_API_KEY}"
  "--ai.analysis.model=${AI_MODEL}"
)

echo "Database target: ${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "Python service: ${PYTHON_HOST}:${PYTHON_PORT}"
echo

if [[ -f "$SCRIPT_DIR/ACASB.jar" ]]; then
  JAR_PATH="$SCRIPT_DIR/ACASB.jar"
elif [[ -f "$SCRIPT_DIR/ACASB-0.0.1-SNAPSHOT.jar" ]]; then
  JAR_PATH="$SCRIPT_DIR/ACASB-0.0.1-SNAPSHOT.jar"
else
  echo "Could not find ACASB.jar or ACASB-0.0.1-SNAPSHOT.jar."
  echo
  read -r "?Press Enter to close..."
  exit 1
fi

echo "Starting ACASB Java Backend on port 8080..."
java -jar "$JAR_PATH" "${SPRING_ARGS[@]}"
STATUS=$?

echo
if [[ $STATUS -eq 0 ]]; then
  echo "Java service stopped."
else
  echo "Java service exited with code $STATUS."
fi

read -r "?Press Enter to close..."
exit $STATUS
