#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"
ERROR_LOG="${ROOT_DIR}/unit_tests/error.log"
> "$ERROR_LOG"

TEST_CLASSES=(
  "AuthControllerTest"
  "MessagePrivacyServiceTest"
  "MessageCenterAuthorizationTest"
  "MessageTemplateRuntimeTest"
  "MessageSchedulerBoundaryTest"
  "SearchServiceTest"
  "SearchControllerIntegrationTest"
  "NotificationRuleServiceTest"
  "NotificationSettingsControllerTest"
  "RbacAccessTest"
  "ObservabilityServiceTest"
  "ObservabilityAlertTriggerTest"
  "WorkflowAuthorizationTest"
  "WorkflowEscalationTest"
  "DataImportServiceTest"
  "DataIntegrationControllerTest"
  "ApiSecuritySemanticsTest"
)

TOTAL=${#TEST_CLASSES[@]}
PASS=0
FAIL=0

TEST_ARG=$(IFS=,; echo "${TEST_CLASSES[*]}")

run_maven_local() {
  bash -lc "cd \"${BACKEND_DIR}\" && mvn -q -Dtest=${TEST_ARG} test"
}

run_maven_docker() {
  local m2_volume="mindflow-m2-cache"
  bash -lc "MSYS_NO_PATHCONV=1 docker run --rm -v \"${BACKEND_DIR}:/app\" -v \"${m2_volume}:/root/.m2\" -w \"/app\" maven:3.9.8-eclipse-temurin-17-alpine mvn -q -Dtest=${TEST_ARG} test"
}

echo "[UNIT] Running ${TOTAL} tests in one Maven invocation"
echo "[UNIT] Targets: ${TEST_ARG}"

if command -v mvn >/dev/null 2>&1; then
  if run_maven_local; then
    PASS=$TOTAL
  else
    FAIL=$TOTAL
    echo "[UNIT][FAIL] Maven local run failed" >> "$ERROR_LOG"
  fi
elif command -v docker >/dev/null 2>&1; then
  if run_maven_docker; then
    PASS=$TOTAL
  else
    FAIL=$TOTAL
    echo "[UNIT][FAIL] Maven docker run failed" >> "$ERROR_LOG"
  fi
else
  FAIL=$TOTAL
  echo "[UNIT] neither mvn nor docker found in environment" | tee -a "$ERROR_LOG"
fi

echo "UNIT_SUMMARY total=${TOTAL} pass=${PASS} fail=${FAIL}"
if [ "$FAIL" -gt 0 ]; then
  echo "Failure logs: ${ERROR_LOG}"
  exit 1
fi
