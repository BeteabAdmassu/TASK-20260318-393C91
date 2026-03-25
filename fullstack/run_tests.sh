#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UNIT_SCRIPT="${ROOT_DIR}/unit_tests/run_unit_tests.sh"
API_SCRIPT="${ROOT_DIR}/API_tests/run_api_tests.sh"

hydrate_env_for_tests() {
  local env_file="$1"
  local jwt_placeholder="REPLACE_WITH_BASE64_32B_SECRET"
  local pwd_placeholder="__CHANGE_ME_STRONG_PASSWORD__"

  if command -v python3 >/dev/null 2>&1; then
    python3 - <<'PY' "$env_file" "$jwt_placeholder" "$pwd_placeholder"
import base64, os, secrets, sys

path = sys.argv[1]
jwt_placeholder = sys.argv[2]
pwd_placeholder = sys.argv[3]

with open(path, "r", encoding="utf-8") as f:
    lines = f.read().splitlines()

changed = False
updated = []
for line in lines:
    if line.startswith("JWT_SECRET="):
        val = line.split("=", 1)[1].strip()
        if not val or val == jwt_placeholder:
            val = base64.b64encode(secrets.token_bytes(48)).decode("ascii")
            line = f"JWT_SECRET={val}"
            changed = True
    elif line.startswith("BOOTSTRAP_ADMIN_PASSWORD="):
        val = line.split("=", 1)[1].strip()
        if not val or val == pwd_placeholder:
            val = "RunTestsAdmin!2026"
            line = f"BOOTSTRAP_ADMIN_PASSWORD={val}"
            changed = True
    updated.append(line)

if changed:
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(updated) + "\n")
    print("[WARN] Hydrated placeholder secrets in .env for local test run")
PY
  fi
}

if [ ! -f "${ROOT_DIR}/.env" ]; then
  if [ -f "${ROOT_DIR}/.env.example" ]; then
    cp "${ROOT_DIR}/.env.example" "${ROOT_DIR}/.env"
    echo "[WARN] .env missing; copied .env.example to .env for test run reproducibility"
    hydrate_env_for_tests "${ROOT_DIR}/.env"
  else
    echo "[ERROR] Missing ${ROOT_DIR}/.env and ${ROOT_DIR}/.env.example"
    echo "Create env file first, e.g.: cp .env.example .env"
    exit 1
  fi
else
  hydrate_env_for_tests "${ROOT_DIR}/.env"
fi

if [ -f "${ROOT_DIR}/.env" ]; then
  set -a
  source "${ROOT_DIR}/.env"
  set +a
fi

chmod +x "$UNIT_SCRIPT" "$API_SCRIPT"

TOTAL=0
PASS=0
FAIL=0

run_suite() {
  local name="$1"
  local script="$2"
  echo "===== Running ${name} ====="
  local output status
  set +e
  output=$(bash "$script" 2>&1)
  status=$?
  set -e
  echo "$output"

  local summary
  summary=$(echo "$output" | grep "${name}_SUMMARY" | tail -1 || true)
  if [ -z "$summary" ]; then
    summary=$(echo "$output" | grep "SUMMARY" | tail -1 || true)
  fi

  if [ -n "$summary" ]; then
    local t p f
    t=$(echo "$summary" | sed -E 's/.*total=([0-9]+).*/\1/')
    p=$(echo "$summary" | sed -E 's/.*pass=([0-9]+).*/\1/')
    f=$(echo "$summary" | sed -E 's/.*fail=([0-9]+).*/\1/')
    TOTAL=$((TOTAL + t))
    PASS=$((PASS + p))
    FAIL=$((FAIL + f))
  elif [ "$status" -ne 0 ]; then
    TOTAL=$((TOTAL + 1))
    FAIL=$((FAIL + 1))
  fi
}

ensure_stack_for_api() {
  local health
  health=$(curl --silent --show-error --connect-timeout 3 --max-time 8 -o /dev/null -w "%{http_code}" "http://localhost:8080/actuator/health" || true)
  if [ "$health" = "200" ]; then
    return
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    echo "Bringing up docker stack for API tests..."
    if [ "${TEST_RESET_STACK:-true}" = "true" ]; then
      bash -lc "cd \"${ROOT_DIR}\" && docker-compose --env-file .env down -v --remove-orphans || true"
    fi
    bash -lc "cd \"${ROOT_DIR}\" && docker-compose --env-file .env up -d --build"
    for _ in $(seq 1 60); do
      health=$(curl --silent --show-error --connect-timeout 3 --max-time 8 -o /dev/null -w "%{http_code}" "http://localhost:8080/actuator/health" || true)
      if [ "$health" = "200" ]; then
        break
      fi
      sleep 2
    done
  fi
}

run_suite "UNIT" "$UNIT_SCRIPT"
ensure_stack_for_api
run_suite "API" "$API_SCRIPT"

echo "===== FINAL TEST SUMMARY ====="
echo "Total tests: ${TOTAL}"
echo "Passed: ${PASS}"
echo "Failed: ${FAIL}"

if [ "$FAIL" -gt 0 ]; then
  echo "See detailed failure logs in fullstack/API_tests/error.log (if API suite failed)."
  exit 1
fi
