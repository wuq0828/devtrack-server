#!/usr/bin/env bash
# DevTrack Phase 2 冒烟:缺陷详情(历史+评论+可用流转) + 看板 + 统计 + 数据范围
# 前置:服务已在 http://localhost:8080 运行。
set -uo pipefail
BASE="${DEVTRACK_URL:-http://localhost:8080}"
pretty() { if command -v jq >/dev/null 2>&1; then jq -c .; else cat; fi; }
login() { curl -s -X POST "$BASE/devtrack/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"$1\",\"password\":\"$2\"}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'; }
post() { curl -s -X POST "$BASE$1" -H "Content-Type: application/json" "${@:2}"; }

ADMIN=$(login admin admin123)
A=(-H "Authorization: Bearer $ADMIN")

echo "==================== 历史 + 评论 + 详情 ===================="
echo "-- 建缺陷并推进 confirm->start->resolve(生成历史)"
DID=$(post /devtrack/defect/create "${A[@]}" -d '{"projectId":1,"title":"详情演示:支付回调超时","severity":"CRITICAL","priority":"P0"}' | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
for a in confirm start resolve; do post /devtrack/defect/transition "${A[@]}" -d "{\"defectId\":$DID,\"transitionCode\":\"$a\"}" >/dev/null; done
echo "   缺陷 #$DID"
echo "-- 加两条评论"
post /devtrack/defect/comment "${A[@]}" -d "{\"defectId\":$DID,\"content\":\"复现了,日志见附件\"}" | pretty
post /devtrack/defect/comment "${A[@]}" -d "{\"defectId\":$DID,\"content\":\"已定位是网关超时\"}" >/dev/null
echo "-- 缺陷详情(应含 3 条历史 + 2 条评论 + 当前态可用流转)"
post /devtrack/defect/detail "${A[@]}" -d "{\"defectId\":$DID}" | pretty

echo ""
echo "==================== 看板 + 统计 ===================="
echo "-- 看板(按状态分列,带 count)"
post /devtrack/defect/board "${A[@]}" -d '{"projectId":1}' | pretty
echo "-- 统计(总数 / 按状态 / 按优先级 / 按严重度)"
post /devtrack/defect/stats "${A[@]}" -d '{"projectId":1}' | pretty

echo ""
echo "==================== 数据范围 ===================="
echo "-- 飞书新用户登录(无任何项目角色)"
FS=$(curl -s -X POST "$BASE/devtrack/auth/feishu/login?code=ou_scope_test" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo "-- 该用户看 projectId=1 列表(无 BUG:VIEW,应 1004)"
post /devtrack/defect/list -H "Authorization: Bearer $FS" -d '{"projectId":1,"pn":1,"ps":5}' | pretty
echo "-- 该用户不带 projectId 查列表(数据范围空,应 code:0 但 total:0)"
post /devtrack/defect/list -H "Authorization: Bearer $FS" -d '{"pn":1,"ps":5}' | pretty
echo "-- admin 不带 projectId 查列表(SYS_ADMIN 不受限,应能看到数据)"
post /devtrack/defect/list "${A[@]}" -d '{"pn":1,"ps":3}' | pretty

echo ""
echo "==> 完成。"
