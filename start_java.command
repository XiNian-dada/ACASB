#!/bin/zsh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

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
java -jar "$JAR_PATH"
STATUS=$?

echo
if [[ $STATUS -eq 0 ]]; then
  echo "Java service stopped."
else
  echo "Java service exited with code $STATUS."
fi

read -r "?Press Enter to close..."
exit $STATUS
