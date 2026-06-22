#!/usr/bin/env bash
# DevTrack Phase 5 冒烟:迭代报表 + API Token 存库 SHA256
# 前置:服务已在 http://localhost:8080 运行。
set -uo pipefail
BASE="${DEVTRACK_URL:-http://localhost:8080}"
pretty() { if command -v jq >/dev/null 2>&1; then jq -c .; else cat; fi; }
post() { curl -s -X POST "$BASE$1" -H "Content-Type: application/json" "${@:2}"; }
idof() { sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1; }

ADMIN=$(post /devtrack/auth/login -d '{"username":"admin","password":"admin123"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
GUEST=$(post /devtrack/auth/login -d '{"username":"guest","password":"guest123"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
A=(-H "Authorization: Bearer $ADMIN")

echo "==================== 迭代报表 ===================="
IT=$(post /devtrack/iteration/create "${A[@]}" -d '{"projectId":1,"name":"报表Sprint","startDate":"2026-06-14","endDate":"2026-06-25"}' | idof)
echo "迭代 #$IT;建 3 缺陷归入,推 1 个到 CLOSED"
for i in 1 2 3; do
  D=$(post /devtrack/defect/create "${A[@]}" -d "{\"projectId\":1,\"title\":\"报表缺陷$i\"}" | idof)
  post /devtrack/defect/assign-iteration "${A[@]}" -d "{\"defectId\":$D,\"iterationId\":$IT}" >/dev/null
  if [ "$i" = "1" ]; then
    for a in confirm start resolve verify close; do post /devtrack/defect/transition "${A[@]}" -d "{\"defectId\":$D,\"transitionCode\":\"$a\"}" >/dev/null; done
  elif [ "$i" = "2" ]; then
    for a in confirm start; do post /devtrack/defect/transition "${A[@]}" -d "{\"defectId\":$D,\"transitionCode\":\"$a\"}" >/dev/null; done
  fi
done
echo "报表(应 total=3, done=1(CLOSED), inProgress=1, todo=1, completionRate≈33.3):"
post /devtrack/iteration/report "${A[@]}" -d "{\"iterationId\":$IT}" | pretty

echo ""
echo "==================== API Token 存库 SHA256 ===================="
echo "-- a) 用播种的 dvt_live_demo_token 上报(库存 SHA256,应成功)"
post /devtrack/integrations/test-reports -H "Authorization: Bearer dvt_live_demo_token" \
  -d '{"projectCode":"DEMO","results":[{"fullName":"t1","historyId":"h1","status":"passed"}]}' | pretty
echo "-- b) 错误 token(应 1004)"
post /devtrack/integrations/test-reports -H "Authorization: Bearer totally-wrong" \
  -d '{"projectCode":"DEMO","results":[]}' | pretty
echo "-- c) admin 创建新 token(返回明文,仅一次)"
NEW=$(post /devtrack/apitoken/create "${A[@]}" -d '{"name":"ci-token"}')
echo "$NEW" | pretty
NEWTOKEN=$(echo "$NEW" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo "-- d) 用新 token 上报(应成功)"
post /devtrack/integrations/test-reports -H "Authorization: Bearer $NEWTOKEN" \
  -d '{"projectCode":"DEMO","results":[{"fullName":"t2","historyId":"h2","status":"passed"}]}' | pretty
echo "-- e) guest 创建 token(非 SYS_ADMIN,应 1004)"
post /devtrack/apitoken/create -H "Authorization: Bearer $GUEST" -d '{"name":"hack"}' | pretty

echo ""
echo "==> 完成。"
