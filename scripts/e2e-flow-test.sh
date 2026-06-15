#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:3001}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-openlab@123}"
TEACHER_USERNAME="${TEACHER_USERNAME:-}"
TEACHER_PASSWORD="${TEACHER_PASSWORD:-}"
STUDENT_USERNAME="${STUDENT_USERNAME:-}"
STUDENT_PASSWORD="${STUDENT_PASSWORD:-}"
TEST_CLASS_ID="${TEST_CLASS_ID:-}"
FIRST_USERNAME="${FIRST_USERNAME:-}"
FIRST_PASSWORD="${FIRST_PASSWORD:-}"
FIRST_NEW_PASSWORD="${FIRST_NEW_PASSWORD:-}"

require_env() {
  local name="$1"
  local value="${!name:-}"
  if [ -z "$value" ]; then
    echo "Missing required env: $name" >&2
    exit 2
  fi
}

json_escape() {
  jq -Rn --arg value "$1" '$value'
}

api() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local body="${4:-}"
  local output
  output="$(mktemp)"
  local args=(-sS --noproxy '*' -X "$method" "$BASE_URL$path" -H 'Content-Type: application/json' -o "$output" -w '%{http_code}')
  if [ -n "$token" ]; then
    args+=(-H "Authorization: Bearer $token")
  fi
  if [ -n "$body" ]; then
    args+=(-d "$body")
  fi
  local http_code
  http_code="$(curl "${args[@]}")"
  jq --arg httpCode "$http_code" '. + {httpCode: ($httpCode|tonumber)}' "$output"
  rm -f "$output"
}

assert_ok() {
  local label="$1"
  local payload="$2"
  local http_code app_code
  http_code="$(jq -r '.httpCode' <<<"$payload")"
  app_code="$(jq -r '.code // empty' <<<"$payload")"
  if [ "$http_code" != "200" ] || [ "$app_code" != "200" ]; then
    echo "FAIL: $label" >&2
    jq . <<<"$payload" >&2
    exit 1
  fi
  echo "PASS: $label"
}

login() {
  local username="$1"
  local password="$2"
  local payload
  payload="$(api POST /auth/login '' "{\"username\":$(json_escape "$username"),\"password\":$(json_escape "$password")}")"
  assert_ok "login $username" "$payload" >&2
  jq -r '.data.token' <<<"$payload"
}

require_env TEACHER_USERNAME
require_env TEACHER_PASSWORD
require_env STUDENT_USERNAME
require_env STUDENT_PASSWORD
require_env TEST_CLASS_ID

ADMIN_TOKEN="$(login "$ADMIN_USERNAME" "$ADMIN_PASSWORD")"
TEACHER_TOKEN="$(login "$TEACHER_USERNAME" "$TEACHER_PASSWORD")"
STUDENT_TOKEN="$(login "$STUDENT_USERNAME" "$STUDENT_PASSWORD")"

SEARCH_RESULT="$(api GET "/api/admin/user/list?page=1&size=10&keyword=$STUDENT_USERNAME&exact=true" "$ADMIN_TOKEN")"
assert_ok "admin exact user search" "$SEARCH_RESULT"
if ! jq -e --arg username "$STUDENT_USERNAME" '.data.records[]? | select(.username == $username)' <<<"$SEARCH_RESULT" >/dev/null; then
  echo "FAIL: exact search did not return $STUDENT_USERNAME" >&2
  jq . <<<"$SEARCH_RESULT" >&2
  exit 1
fi

SYSTEM_CHECK="$(api GET /api/admin/system/check "$ADMIN_TOKEN")"
assert_ok "admin system check" "$SYSTEM_CHECK"

if [ -n "$FIRST_USERNAME" ] && [ -n "$FIRST_PASSWORD" ] && [ -n "$FIRST_NEW_PASSWORD" ]; then
  FIRST_LOGIN="$(api POST /auth/login '' "{\"username\":$(json_escape "$FIRST_USERNAME"),\"password\":$(json_escape "$FIRST_PASSWORD")}")"
  assert_ok "first-login account login" "$FIRST_LOGIN"
  if jq -e '.data.mustChangePassword == true' <<<"$FIRST_LOGIN" >/dev/null; then
    FIRST_TOKEN="$(jq -r '.data.token' <<<"$FIRST_LOGIN")"
    CHANGE_RESULT="$(api PUT /user/password "$FIRST_TOKEN" "{\"oldPassword\":$(json_escape "$FIRST_PASSWORD"),\"newPassword\":$(json_escape "$FIRST_NEW_PASSWORD")}")"
    assert_ok "first-login password change" "$CHANGE_RESULT"
  else
    echo "SKIP: first-login password change, account is not marked mustChangePassword"
  fi
else
  echo "SKIP: first-login password change, set FIRST_USERNAME/FIRST_PASSWORD/FIRST_NEW_PASSWORD to enable"
fi

START_TIME="$(date -d '+1 hour' '+%Y-%m-%dT%H:%M:%S')"
END_TIME="$(date -d '+2 days' '+%Y-%m-%dT%H:%M:%S')"
STAMP="$(date '+%Y%m%d%H%M%S')"

EXAM_BODY="{\"title\":\"E2E考试$STAMP\",\"startTime\":\"$START_TIME\",\"endTime\":\"$END_TIME\",\"targetClassIds\":[$TEST_CLASS_ID],\"duration\":30,\"totalScore\":100,\"passScore\":60}"
EXAM_RESULT="$(api POST /api/exam "$TEACHER_TOKEN" "$EXAM_BODY")"
assert_ok "teacher creates exam" "$EXAM_RESULT"
EXAM_ID="$(jq -r '.data.id' <<<"$EXAM_RESULT")"
echo "INFO: created exam $EXAM_ID"

HOMEWORK_BODY="{\"title\":\"E2E作业$STAMP\",\"content\":\"E2E 自动化提交内容\",\"deadline\":\"$END_TIME\",\"targetClassIds\":[$TEST_CLASS_ID]}"
HOMEWORK_RESULT="$(api POST /api/homework "$TEACHER_TOKEN" "$HOMEWORK_BODY")"
assert_ok "teacher creates homework" "$HOMEWORK_RESULT"
HOMEWORK_ID="$(jq -r '.data' <<<"$HOMEWORK_RESULT")"
SUBMIT_HOMEWORK="$(api POST "/api/homework/$HOMEWORK_ID/submit" "$STUDENT_TOKEN" "{\"content\":\"E2E 学生作业提交 $STAMP\"}")"
assert_ok "student submits homework" "$SUBMIT_HOMEWORK"

SURVEY_BODY="{\"title\":\"E2E问卷$STAMP\",\"endTime\":\"$END_TIME\",\"targetClassIds\":[$TEST_CLASS_ID],\"isAnonymousRequired\":0,\"questions\":[{\"type\":\"TEXT\",\"title\":\"E2E反馈\",\"isRequired\":1,\"sortOrder\":1}]}"
SURVEY_RESULT="$(api POST /api/survey "$TEACHER_TOKEN" "$SURVEY_BODY")"
assert_ok "teacher creates survey" "$SURVEY_RESULT"
SURVEY_ID="$(jq -r '.data' <<<"$SURVEY_RESULT")"
PUBLISH_RESULT="$(api POST "/api/survey/$SURVEY_ID/publish" "$TEACHER_TOKEN")"
assert_ok "teacher publishes survey" "$PUBLISH_RESULT"
SURVEY_DETAIL="$(api GET "/api/survey/$SURVEY_ID/detail" "$STUDENT_TOKEN")"
assert_ok "student loads survey detail" "$SURVEY_DETAIL"
QUESTION_ID="$(jq -r '.data.questions[0].id' <<<"$SURVEY_DETAIL")"
SUBMIT_SURVEY="$(api POST "/api/survey/$SURVEY_ID/submit" "$STUDENT_TOKEN" "{\"answers\":[{\"questionId\":$QUESTION_ID,\"answerValue\":\"E2E反馈$STAMP\"}]}")"
assert_ok "student submits survey" "$SUBMIT_SURVEY"

echo "E2E flow completed."
