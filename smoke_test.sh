#!/usr/bin/env bash
# DevTrack 端到端冒烟测试:登录 -> 建缺陷 -> 列表 -> 流转 -> pytest 失败上报(自动建单+去重)
# 依赖:服务已在 http://localhost:8080 运行(mvn spring-boot:run -pl devtrack-app)。
# 需要 curl;若有 jq 则输出更美观(没有也能跑)。
set -euo pipefail

BASE="${DEVTRACK_URL:-http://localhost:8080}"
API_TOKEN="${DEVTRACK_API_TOKEN:-dvt_live_demo_token}"

pretty() { if command -v jq >/dev/null 2>&1; then jq .; else cat; fi; }
post() { curl -s -X POST "$BASE$1" -H "Content-Type: application/json" "${@:2}"; }

echo "==> 1) 登录 admin"
LOGIN_RESP=$(post /devtrack/auth/login -d '{"username":"admin","password":"admin123"}')
echo "$LOGIN_RESP" | pretty
TOKEN=$(echo "$LOGIN_RESP" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then echo "登录失败,token 为空"; exit 1; fi
AUTH="Authorization: Bearer $TOKEN"

echo "==> 2) 建缺陷"
CREATE_RESP=$(post /devtrack/defect/create -H "$AUTH" \
  -d '{"projectId":1,"title":"登录按钮点击无响应","description":"iOS 16 复现","severity":"CRITICAL","priority":"P1"}')
echo "$CREATE_RESP" | pretty
DEFECT_ID=$(echo "$CREATE_RESP" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)

echo "==> 3) 缺陷列表(projectId=1)"
post /devtrack/defect/list -H "$AUTH" -d '{"projectId":1,"pn":1,"ps":10}' | pretty

echo "==> 4) 状态流转 confirm -> start -> resolve (缺陷 #$DEFECT_ID)"
for action in confirm start resolve; do
  echo "   - $action"
  post /devtrack/defect/transition -H "$AUTH" \
    -d "{\"defectId\":$DEFECT_ID,\"transitionCode\":\"$action\"}" | pretty
done

echo "==> 5) 模拟 pytest 失败上报(应自动建 1 个 AUTOMATION 缺陷)"
post /devtrack/integrations/test-reports -H "Authorization: Bearer $API_TOKEN" -d '{
  "projectCode":"DEMO","ciRunId":"run-001","allureUrl":"http://allure/run-001",
  "results":[
    {"fullName":"testcases/test_login.py::test_login_ok","historyId":"h-abc-001","status":"passed","durationMs":120},
    {"fullName":"testcases/test_login.py::test_login_fail","historyId":"h-abc-002","status":"failed","durationMs":350,"errorMessage":"AssertionError: status 500"}
  ],"autoCreateDefect":true}' | pretty

echo "==> 6) 同一失败再次上报(应命中去重,defectCreated=0, deduped=1)"
post /devtrack/integrations/test-reports -H "Authorization: Bearer $API_TOKEN" -d '{
  "projectCode":"DEMO","ciRunId":"run-002","allureUrl":"http://allure/run-002",
  "results":[
    {"fullName":"testcases/test_login.py::test_login_fail","historyId":"h-abc-002","status":"failed","durationMs":340,"errorMessage":"AssertionError: status 500"}
  ],"autoCreateDefect":true}' | pretty

echo "==> 完成。"
