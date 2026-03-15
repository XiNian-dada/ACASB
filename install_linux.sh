#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/config.properties"
ANALYSIS_DIR="${SCRIPT_DIR}/acasb-analysis"

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

upsert_prop() {
  local key="$1"
  local value="$2"
  if [[ -f "$CONFIG_FILE" ]] && grep -q "^${key}=" "$CONFIG_FILE"; then
    sed -i.bak "s#^${key}=.*#${key}=${value}#" "$CONFIG_FILE"
  else
    printf '\n%s=%s\n' "$key" "$value" >>"$CONFIG_FILE"
  fi
}

detect_pkg_manager() {
  if command -v apt-get >/dev/null 2>&1; then
    printf 'apt'
    return
  fi
  if command -v dnf >/dev/null 2>&1; then
    printf 'dnf'
    return
  fi
  if command -v yum >/dev/null 2>&1; then
    printf 'yum'
    return
  fi
  printf 'unknown'
}

run_pkg_install() {
  local manager="$1"
  if [[ "$manager" == "unknown" ]]; then
    if command -v java >/dev/null 2>&1 && command -v python3 >/dev/null 2>&1; then
      echo "No supported package manager found, but Java and Python are already available. Skipping OS package install."
      return 0
    fi
    echo "No supported package manager found. Install Java 17+, Python 3 and pip manually."
    return 1
  fi

  local prefix=""
  if [[ "${EUID}" -ne 0 ]]; then
    if command -v sudo >/dev/null 2>&1; then
      prefix="sudo"
    else
      echo "Please run this script as root or install sudo."
      return 1
    fi
  fi

  case "$manager" in
    apt)
      $prefix apt-get update
      $prefix apt-get install -y openjdk-17-jre-headless python3 python3-pip python3-venv curl libgl1 libglib2.0-0
      ;;
    dnf)
      $prefix dnf install -y java-17-openjdk-headless python3 python3-pip curl mesa-libGL glib2
      ;;
    yum)
      $prefix yum install -y java-17-openjdk-headless python3 python3-pip curl mesa-libGL glib2
      ;;
  esac
}

reinstall_python_requirements() {
  local python_bin="$1"

  "$python_bin" -m pip install --upgrade pip

  # Ensure only the headless OpenCV wheel remains in the venv on servers without X11/GL.
  "$python_bin" -m pip uninstall -y opencv-python opencv-contrib-python opencv-contrib-python-headless opencv-python-headless >/dev/null 2>&1 || true
  "$python_bin" -m pip install --no-cache-dir -r "${ANALYSIS_DIR}/requirements.txt"
}

verify_python_runtime() {
  local python_bin="$1"

  if ! "$python_bin" - <<'PY'
import cv2
import numpy
print("cv2", cv2.__version__)
print("numpy", numpy.__version__)
PY
  then
    echo "Python runtime verification failed. OpenCV could not be imported."
    echo "If this is Debian/Ubuntu, confirm libgl1 and libglib2.0-0 are installed."
    exit 1
  fi
}

ensure_auth_secret() {
  local auth_enabled
  local auth_secret

  auth_enabled="$(read_prop "auth.jwt.enabled" "true")"
  auth_secret="$(read_prop "auth.jwt.secret" "")"

  upsert_prop "auth.jwt.enabled" "${auth_enabled:-true}"
  upsert_prop "auth.jwt.expires-hours" "$(read_prop "auth.jwt.expires-hours" "720")"

  if [[ "${auth_enabled,,}" == "false" ]]; then
    echo "Warning: auth.jwt.enabled is false. This is not recommended for production."
    return
  fi

  if [[ -z "$auth_secret" ]] || [[ "$auth_secret" == "CHANGE_ME_IN_PRODUCTION" ]]; then
    if command -v openssl >/dev/null 2>&1; then
      auth_secret="$(openssl rand -hex 32)"
    else
      auth_secret="$(python3 -c 'import secrets; print(secrets.token_hex(32))')"
    fi
    upsert_prop "auth.jwt.secret" "$auth_secret"
    echo "Generated auth.jwt.secret and wrote it to config.properties"
  fi
}

echo "== ACASB Linux installer =="
echo

PKG_MANAGER="$(detect_pkg_manager)"
run_pkg_install "$PKG_MANAGER"

mkdir -p "${SCRIPT_DIR}/logs" "${SCRIPT_DIR}/run" "${SCRIPT_DIR}/temp" "${SCRIPT_DIR}/uploads" "${SCRIPT_DIR}/dataset-storage"

if [[ ! -d "$ANALYSIS_DIR" ]]; then
  echo "Missing acasb-analysis directory"
  exit 1
fi

if [[ ! -x "${ANALYSIS_DIR}/.venv/bin/python" ]]; then
  python3 -m venv "${ANALYSIS_DIR}/.venv"
fi

reinstall_python_requirements "${ANALYSIS_DIR}/.venv/bin/python"
verify_python_runtime "${ANALYSIS_DIR}/.venv/bin/python"

ensure_auth_secret

chmod +x "${SCRIPT_DIR}/install_linux.sh" "${SCRIPT_DIR}/start_java.sh" "${SCRIPT_DIR}/start_python.sh"

echo
echo "Install complete."
echo "Next steps:"
echo "1. Review ${CONFIG_FILE}"
echo "2. Start Python: ./start_python.sh"
echo "3. Start Java:   ./start_java.sh"
