#!/usr/bin/env bash
# DevTrack Phase 3 冒烟:BCrypt 登录 + 迭代/燃尽图 + 飞书可交互卡片回调
# 前置:服务已在 http://localhost:8080 运行。
set -uo pipefail
BASE="${DEVTRACK_URL:-http://localhost:8080}"
pretty() { if command -v jq >/dev/null 2>&1; then jq -c .; else cat; fi; }
login() { curl -s -X POST "$BASE/devtrack/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"$1\",\"password\":\"$2\"}"; }
post() { curl -s -X POST "$BASE$1" -H "Content-Type: application/json" "${@:2}"; }
idof() { sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1; }

echo "==================== BCrypt 登录 ===================="
echo "-- admin/admin123(密码已 BCrypt 哈希,应仍能登录)"
LOGIN=$(login admin admin123); echo "$LOGIN" | pretty
ADMIN=$(echo "$LOGIN" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo "-- 错误密码(应 1003)"; login admin wrongpass | pretty
A=(-H "Authorization: Bearer $ADMIN")

echo ""
echo "==================== 迭代 + 燃尽图 ===================="
echo "-- 建迭代(2026-06-14 ~ 06-25)"
IT=$(post /devtrack/iteration/create "${A[@]}" -d '{"projectId":1,"name":"Sprint-1","startDate":"2026-06-14","endDate":"2026-06-25"}')
echo "$IT" | pretty; ITID=$(echo "$IT" | idof)
echo "-- 建 3 个缺陷并全部归入迭代 #$ITID;解决其中 2 个"
DIDS=()
for i in 1 2 3; do
  D=$(post /devtrack/defect/create "${A[@]}" -d "{\"projectId\":1,\"title\":\"迭代缺陷$i\",\"priority\":\"P1\"}" | idof)
  DIDS+=("$D")
  post /devtrack/defect/assign-iteration "${A[@]}" -d "{\"defectId\":$D,\"iterationId\":$ITID}" >/dev/null
done
for D in "${DIDS[0]}" "${DIDS[1]}"; do
  for a in confirm start resolve; do post /devtrack/defect/transition "${A[@]}" -d "{\"defectId\":$D,\"transitionCode\":\"$a\"}" >/dev/null; done
done
echo "   缺陷 ${DIDS[*]};已解决 ${DIDS[0]} ${DIDS[1]}"
echo "-- 燃尽图(total=3,应在今天 06-18 降到 remaining=1)"
post /devtrack/iteration/burndown "${A[@]}" -d "{\"iterationId\":$ITID}" | pretty
echo "-- 迭代列表"
post /devtrack/iteration/list "${A[@]}" -d '{"projectId":1}' | pretty
echo "-- 关闭迭代(状态应变 CLOSED)"
post /devtrack/iteration/close "${A[@]}" -d "{\"iterationId\":$ITID}" | pretty

echo ""
echo "==================== 飞书可交互卡片回调 ===================="
echo "-- a) URL 验证(应回显 challenge)"
post /devtrack/feishu/card-callback -d '{"type":"url_verification","challenge":"abc12345","token":"t"}' | pretty
echo "-- b) 新建缺陷,模拟点「我接单」(admin 绑定 ou_admin_demo,应 toast 已接单)"
CD=$(post /devtrack/defect/create "${A[@]}" -d '{"projectId":1,"title":"卡片接单演示","priority":"P0"}' | idof)
post /devtrack/feishu/card-callback -d "{\"open_id\":\"ou_admin_demo\",\"action\":{\"value\":{\"action\":\"claim\",\"defectId\":\"$CD\"}}}" | pretty
echo "   验证缺陷 #$CD 的 assignee:"
post /devtrack/defect/detail "${A[@]}" -d "{\"defectId\":$CD}" | sed -n 's/.*\("assigneeId":[0-9]*\).*/   \1/p' | head -1
echo "-- c) 把该缺陷推到 IN_PROGRESS,再模拟点「标记已解决」(应 toast 已标记解决)"
for a in confirm start; do post /devtrack/defect/transition "${A[@]}" -d "{\"defectId\":$CD,\"transitionCode\":\"$a\"}" >/dev/null; done
post /devtrack/feishu/card-callback -d "{\"open_id\":\"ou_admin_demo\",\"action\":{\"value\":{\"action\":\"resolve\",\"defectId\":\"$CD\"}}}" | pretty
echo "   验证缺陷 #$CD 状态:"
post /devtrack/defect/detail "${A[@]}" -d "{\"defectId\":$CD}" | sed -n 's/.*"defect":{[^}]*"statusCode":"\([^"]*\)".*/   statusCode=\1/p' | head -1
echo "-- d) 未知 open_id 点按钮(应 toast 提示未识别)"
post /devtrack/feishu/card-callback -d "{\"open_id\":\"ou_stranger\",\"action\":{\"value\":{\"action\":\"claim\",\"defectId\":\"$CD\"}}}" | pretty

echo ""
echo "==> 完成。"
