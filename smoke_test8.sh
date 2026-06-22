#!/usr/bin/env bash
# DevTrack Phase 7 冒烟:自定义字段 + @提及/指派站内信 + 全文搜索 + 数据大屏
# 前置:服务已在 http://localhost:8080 运行。
set -uo pipefail
BASE="${DEVTRACK_URL:-http://localhost:8080}"
pretty() { if command -v jq >/dev/null 2>&1; then jq -c .; else cat; fi; }
post() { curl -s -X POST "$BASE$1" -H "Content-Type: application/json" "${@:2}"; }
idof() { sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1; }

ADMIN_RESP=$(post /devtrack/auth/login -d '{"username":"admin","password":"admin123"}')
ADMIN=$(echo "$ADMIN_RESP" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
QA_RESP=$(post /devtrack/auth/login -d '{"username":"qa","password":"qa123"}')
QA=$(echo "$QA_RESP" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
QA_UID=$(echo "$QA_RESP" | sed -n 's/.*"userId":\([0-9]*\).*/\1/p')
A=(-H "Authorization: Bearer $ADMIN"); Q=(-H "Authorization: Bearer $QA")

echo "==================== 自定义字段 ===================="
echo "字段定义(项目1,应有 root_cause/story_points):"
post /devtrack/customfield/defs "${A[@]}" -d '{"projectId":1}' | pretty
D=$(post /devtrack/defect/create "${A[@]}" -d '{"projectId":1,"title":"自定义字段演示缺陷"}' | idof)
echo "给缺陷 #$D 设置字段值:"
post /devtrack/defect/set-fields "${A[@]}" -d "{\"defectId\":$D,\"fields\":{\"root_cause\":\"代码缺陷\",\"story_points\":3}}" | pretty

echo ""
echo "==================== @提及 站内信 ===================="
echo "admin 在缺陷 #$D 评论中 @qa:"
post /devtrack/defect/comment "${A[@]}" -d "{\"defectId\":$D,\"content\":\"@qa 帮忙复现一下这个\"}" | pretty
echo "qa 的未读数:"; post /devtrack/notification/unread-count "${Q[@]}" -d '{}' | pretty
echo "qa 的通知列表(应含 MENTION):"; post /devtrack/notification/list "${Q[@]}" -d '{}' | pretty

echo ""
echo "==================== 指派 站内信 ===================="
echo "admin 建缺陷直接指派给 qa(userId=$QA_UID):"
post /devtrack/defect/create "${A[@]}" -d "{\"projectId\":1,\"title\":\"指派给QA的缺陷\",\"assigneeId\":$QA_UID}" >/dev/null
echo "qa 通知里应新增 ASSIGN:"; post /devtrack/notification/list "${Q[@]}" -d '{"onlyUnread":true}' | pretty
echo "qa 全部已读后未读数应为 0:"; post /devtrack/notification/mark-all-read "${Q[@]}" -d '{}' >/dev/null; post /devtrack/notification/unread-count "${Q[@]}" -d '{}' | pretty

echo ""
echo "==================== 全文搜索 ===================="
echo "搜 '指派'(标题含此词,应命中):"
post /devtrack/defect/search "${A[@]}" -d '{"keyword":"指派","projectId":1}' | pretty

echo ""
echo "==================== 数据大屏 ===================="
echo "概览(total/openCount/byStatus/byPriority/recentActivities):"
post /devtrack/dashboard/overview "${A[@]}" -d '{"projectId":1}' | head -c 400; echo

echo ""
echo "==> 完成。"
