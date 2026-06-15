#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://127.0.0.1:3001}"
LOGIN_USERNAME="${LOGIN_USERNAME:-}"
LOGIN_PASSWORD="${LOGIN_PASSWORD:-}"

export NO_PROXY="*"
export no_proxy="*"

TMP_DIR="${TMPDIR:-/tmp}/edu-platform-smoke"
mkdir -p "$TMP_DIR"

curl_common=(--noproxy "*" --connect-timeout 3 --max-time 15 -sS)

pass() {
  printf '[PASS] %s\n' "$1"
}

fail() {
  printf '[FAIL] %s\n' "$1" >&2
  exit 1
}

expect_status() {
  local name="$1"
  local url="$2"
  local allowed_codes="$3"
  local body_file="$TMP_DIR/${name//[^a-zA-Z0-9]/_}.out"
  local status

  status="$(curl "${curl_common[@]}" -o "$body_file" -w '%{http_code}' "$url")" || {
    printf '请求失败：%s\n' "$url" >&2
    return 1
  }

  if [[ ",$allowed_codes," == *",$status,"* ]]; then
    pass "$name -> HTTP $status"
    return 0
  fi

  printf '请求地址：%s\n' "$url" >&2
  printf '期望状态码：%s，实际状态码：%s\n' "$allowed_codes" "$status" >&2
  printf '响应内容：\n' >&2
  sed -n '1,20p' "$body_file" >&2
  return 1
}

expect_login() {
  local body_file="$TMP_DIR/login.out"
  local status

  if [[ -z "$LOGIN_USERNAME" || -z "$LOGIN_PASSWORD" ]]; then
    printf '请通过 LOGIN_USERNAME 和 LOGIN_PASSWORD 指定可用测试账号。\n' >&2
    return 1
  fi

  status="$(
    curl "${curl_common[@]}" \
      -H 'Content-Type: application/json' \
      -o "$body_file" \
      -w '%{http_code}' \
      -d "{\"username\":\"$LOGIN_USERNAME\",\"password\":\"$LOGIN_PASSWORD\"}" \
      "$FRONTEND_URL/auth/login"
  )" || {
    printf '登录请求失败：%s/auth/login\n' "$FRONTEND_URL" >&2
    return 1
  }

  if [[ "$status" != "200" ]]; then
    printf '登录接口期望 HTTP 200，实际 HTTP %s\n' "$status" >&2
    sed -n '1,20p' "$body_file" >&2
    return 1
  fi

  if ! grep -q '"code"[[:space:]]*:[[:space:]]*200' "$body_file"; then
    printf '登录响应缺少 code=200：\n' >&2
    sed -n '1,20p' "$body_file" >&2
    return 1
  fi

  if ! grep -q '"token"[[:space:]]*:' "$body_file"; then
    printf '登录响应缺少 token：\n' >&2
    sed -n '1,20p' "$body_file" >&2
    return 1
  fi

  pass "login proxy -> HTTP 200, token present"
}

main() {
  local failed=0

  printf '后端地址：%s\n' "$BACKEND_URL"
  printf '前端地址：%s\n' "$FRONTEND_URL"

  expect_status "backend" "$BACKEND_URL" "401,404" || failed=1
  expect_status "frontend" "$FRONTEND_URL" "200" || failed=1
  expect_status "frontend api proxy" "$FRONTEND_URL/api" "401" || failed=1
  expect_status "frontend login get proxy" "$FRONTEND_URL/auth/login" "405" || failed=1
  expect_login || failed=1

  return "$failed"
}

main "$@" || fail "系统冒烟测试未通过"
