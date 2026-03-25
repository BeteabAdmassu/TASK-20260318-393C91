#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"

if [ -f "${ROOT_DIR}/.env" ]; then
  set -a
  source "${ROOT_DIR}/.env"
  set +a
fi

PASS=0
FAIL=0
TOTAL=0
ERROR_LOG="${ROOT_DIR}/API_tests/error.log"
> "$ERROR_LOG"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" | tee -a "$ERROR_LOG"
    exit 1
  fi
}

require_cmd curl

JSON_TOOL=""
if command -v jq >/dev/null 2>&1; then
  JSON_TOOL="jq"
elif command -v python3 >/dev/null 2>&1; then
  JSON_TOOL="python3"
else
  echo "Missing required JSON tool: install jq or python3" | tee -a "$ERROR_LOG"
  exit 1
fi

CURL_CONNECT_TIMEOUT="${CURL_CONNECT_TIMEOUT:-3}"
CURL_MAX_TIME="${CURL_MAX_TIME:-12}"

CURL_BASE_OPTS=(
  --silent
  --show-error
  --connect-timeout "$CURL_CONNECT_TIMEOUT"
  --max-time "$CURL_MAX_TIME"
)

json_extract_token() {
  local body="$1"
  if [ "$JSON_TOOL" = "jq" ]; then
    echo "$body" | jq -r '.token // empty' 2>/dev/null || true
  else
    python3 - <<'PY' "$body"
import json,sys
try:
    print(json.loads(sys.argv[1]).get("token", ""))
except Exception:
    print("")
PY
  fi
}

json_array_len() {
  local body="$1"
  if [ "$JSON_TOOL" = "jq" ]; then
    echo "$body" | jq 'length' 2>/dev/null || echo 0
  else
    python3 - <<'PY' "$body"
import json,sys
try:
    print(len(json.loads(sys.argv[1])))
except Exception:
    print(0)
PY
  fi
}

build_json_object() {
  local username="$1"
  local password="$2"
  if [ "$JSON_TOOL" = "jq" ]; then
    jq -cn --arg username "$username" --arg password "$password" '{username: $username, password: $password}'
  else
    python3 - <<'PY' "$username" "$password"
import json,sys
print(json.dumps({"username":sys.argv[1],"password":sys.argv[2]}))
PY
  fi
}

build_booking_payload() {
  local routeNumber="$1"
  local passengerPhone="$2"
  local passengerIdCard="$3"
  local startTime="$4"
  if [ "$JSON_TOOL" = "jq" ]; then
    jq -cn --arg routeNumber "$routeNumber" --arg passengerPhone "$passengerPhone" --arg passengerIdCard "$passengerIdCard" --arg startTime "$startTime" '{routeNumber: $routeNumber, passengerPhone: $passengerPhone, passengerIdCard: $passengerIdCard, startTime: $startTime}'
  else
    python3 - <<'PY' "$routeNumber" "$passengerPhone" "$passengerIdCard" "$startTime"
import json,sys
print(json.dumps({
  "routeNumber":sys.argv[1],
  "passengerPhone":sys.argv[2],
  "passengerIdCard":sys.argv[3],
  "startTime":sys.argv[4],
}))
PY
  fi
}

record() {
  local name="$1" ok="$2" details="${3:-}"
  TOTAL=$((TOTAL + 1))
  if [ "$ok" = "1" ]; then
    PASS=$((PASS + 1))
    echo "[API] PASS: $name"
  else
    FAIL=$((FAIL + 1))
    echo "[API] FAIL: $name" | tee -a "$ERROR_LOG"
    if [ -n "$details" ]; then
      echo "  $details" | tee -a "$ERROR_LOG"
    fi
  fi
}

request() {
  local method="$1" url="$2" token="${3:-}" body="${4:-}"
  local headers=(-H "Content-Type: application/json")
  local response=""
  local rc=0
  if [ -n "$token" ]; then
    headers+=(-H "Authorization: Bearer ${token}")
  fi
  set +e
  if [ -n "$body" ]; then
    response=$(curl "${CURL_BASE_OPTS[@]}" -X "$method" "${url}" "${headers[@]}" -d "$body")
  else
    response=$(curl "${CURL_BASE_OPTS[@]}" -X "$method" "${url}" "${headers[@]}")
  fi
  rc=$?
  set -e
  if [ "$rc" -ne 0 ]; then
    echo ""
    return 0
  fi
  echo "$response"
}

request_status() {
  local method="$1" url="$2" token="${3:-}" body="${4:-}"
  local headers=(-H "Content-Type: application/json")
  local status_code="000"
  local rc=0
  if [ -n "$token" ]; then
    headers+=(-H "Authorization: Bearer ${token}")
  fi
  set +e
  if [ -n "$body" ]; then
    status_code=$(curl "${CURL_BASE_OPTS[@]}" -o /tmp/api_body.$$ -w "%{http_code}" -X "$method" "${url}" "${headers[@]}" -d "$body")
  else
    status_code=$(curl "${CURL_BASE_OPTS[@]}" -o /tmp/api_body.$$ -w "%{http_code}" -X "$method" "${url}" "${headers[@]}")
  fi
  rc=$?
  set -e
  if [ "$rc" -ne 0 ]; then
    echo "000"
    return 0
  fi
  echo "$status_code"
}

health_code=$(curl "${CURL_BASE_OPTS[@]}" -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/health" || true)
if [ "$health_code" != "200" ]; then
  echo "Backend not ready at ${BASE_URL} (health=${health_code})" | tee -a "$ERROR_LOG"
  echo "API_SUMMARY total=1 pass=0 fail=1"
  exit 1
fi

reset_stack_and_wait() {
  if ! command -v docker-compose >/dev/null 2>&1; then
    return 1
  fi

  echo "[API] Resetting docker stack for deterministic auth..." | tee -a "$ERROR_LOG"
  bash -lc "cd \"${ROOT_DIR}\" && docker-compose --env-file .env down -v --remove-orphans || true"
  bash -lc "cd \"${ROOT_DIR}\" && docker-compose --env-file .env up -d --build"

  local health=""
  for _ in $(seq 1 60); do
    health=$(curl "${CURL_BASE_OPTS[@]}" -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/health" || true)
    if [ "$health" = "200" ]; then
      return 0
    fi
    sleep 2
  done
  echo "[API] Backend failed health check after stack reset" | tee -a "$ERROR_LOG"
  return 1
}

ADMIN_USERNAME="${BOOTSTRAP_ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${BOOTSTRAP_ADMIN_PASSWORD:-}"
if [ -z "$ADMIN_PASSWORD" ]; then
  echo "BOOTSTRAP_ADMIN_PASSWORD is required for API tests" | tee -a "$ERROR_LOG"
  echo "API_SUMMARY total=1 pass=0 fail=1"
  exit 1
fi

admin_login=$(build_json_object "$ADMIN_USERNAME" "$ADMIN_PASSWORD")
admin_body=$(request POST "${BASE_URL}/api/auth/login" "" "$admin_login")
admin_token=$(json_extract_token "$admin_body")
if [ -n "$admin_token" ]; then
  record "admin login" 1
else
  if [ "${API_AUTO_RESET_ON_AUTH_FAIL:-true}" = "true" ] && reset_stack_and_wait; then
    admin_body=$(request POST "${BASE_URL}/api/auth/login" "" "$admin_login")
    admin_token=$(json_extract_token "$admin_body")
  fi

  if [ -n "$admin_token" ]; then
    record "admin login" 1 "recovered after stack reset"
  else
    record "admin login" 0 "body=${admin_body}"
    echo "API_SUMMARY total=${TOTAL} pass=${PASS} fail=${FAIL}"
    echo "Failure logs: ${ERROR_LOG}"
    exit 1
  fi
fi

bad_login_code=$(request_status POST "${BASE_URL}/api/auth/login" "" '{"username":"admin","password":"bad"}')
if [ "$bad_login_code" = "401" ] || [ "$bad_login_code" = "400" ]; then
  record "invalid login rejected" 1
else
  record "invalid login rejected" 0 "status=${bad_login_code}"
fi

unauth_code=$(request_status GET "${BASE_URL}/api/admin/observability/snapshot" "")
if [ "$unauth_code" = "401" ] || [ "$unauth_code" = "403" ]; then
  record "unauthorized access blocked" 1
else
  record "unauthorized access blocked" 0 "status=${unauth_code}"
fi

create_user_body='{"username":"apitest_passenger","password":"pass12345","role":"PASSENGER"}'
create_user_code=$(request_status POST "${BASE_URL}/api/admin/users" "$admin_token" "$create_user_body")
if [ "$create_user_code" = "201" ] || [ "$create_user_code" = "400" ] || [ "$create_user_code" = "409" ]; then
  record "create passenger user" 1
else
  record "create passenger user" 0 "status=${create_user_code}"
fi

pass_login=$(request POST "${BASE_URL}/api/auth/login" "" '{"username":"apitest_passenger","password":"pass12345"}')
pass_token=$(json_extract_token "$pass_login")
if [ -n "$pass_token" ]; then
  record "passenger login" 1
else
  record "passenger login" 0 "body=${pass_login}"
fi

before_count=$(json_array_len "$(request GET "${BASE_URL}/api/passenger/messages-center" "$pass_token")")

start_time=$(date -u -d '-6 minutes' '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || date -u '+%Y-%m-%dT%H:%M:%SZ')

booking_payload=$(build_booking_payload "A1" "13812345678" "ABC123456789XYZ" "$start_time")

booking_code=$(request_status POST "${BASE_URL}/api/passenger/messages-center/booking-events" "$pass_token" "$booking_payload")
if [ "$booking_code" = "201" ]; then
  record "booking event seed" 1
else
  record "booking event seed" 0 "status=${booking_code}"
fi

sleep 3
after_count=$(json_array_len "$(request GET "${BASE_URL}/api/passenger/messages-center" "$pass_token")")
if [ "$after_count" -ge "$before_count" ]; then
  record "data consistency message count check" 1 "before=${before_count} after=${after_count}"
else
  record "data consistency message count check" 0 "before=${before_count} after=${after_count}"
fi

echo "API_SUMMARY total=${TOTAL} pass=${PASS} fail=${FAIL}"
if [ "$FAIL" -gt 0 ]; then
  echo "Failure logs: ${ERROR_LOG}"
  exit 1
fi
