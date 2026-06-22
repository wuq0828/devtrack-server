#!/usr/bin/env bash
# DevTrack Phase 6 冒烟:附件上传/下载 + 操作记录 + 批量流转 + 燃尽图理想线
# 前置:服务已在 http://localhost:8080 运行。
set -uo pipefail
BASE="${DEVTRACK_URL:-http://localhost:8080}"
pretty() { if command -v jq >/dev/null 2>&1; then jq -c .; else cat; fi; }
post() { curl -s -X POST "$BASE$1" -H "Content-Type: application/json" "${@:2}"; }
idof() { sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1; }

ADMIN=$(post /devtrack/auth/login -d '{"username":"admin","password":"admin123"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
A=(-H "Authorization: Bearer $ADMIN")

echo "==================== 附件上传 / 下载 ===================="
D=$(post /devtrack/defect/create "${A[@]}" -d '{"projectId":1,"title":"带附件的缺陷","priority":"P1"}' | idof)
echo "缺陷 #$D;上传一个文本附件"
echo "这是附件内容 attachment content 中文" > /tmp/dt_attach.txt
UP=$(curl -s -X POST "$BASE/devtrack/attachment/upload" "${A[@]}" -F "file=@/tmp/dt_attach.txt" -F "defectId=$D")
echo "$UP" | pretty; AID=$(echo "$UP" | idof)
echo "附件列表:"; post /devtrack/attachment/list "${A[@]}" -d "{\"defectId\":$D}" | pretty
echo "下载附件 #$AID 并比对内容:"
curl -s "$BASE/devtrack/attachment/$AID/download" "${A[@]}" -o /tmp/dt_attach_dl.txt
if diff -q /tmp/dt_attach.txt /tmp/dt_attach_dl.txt >/dev/null; then echo "   ✅ 下载内容与上传一致"; else echo "   ❌ 内容不一致"; fi

echo ""
echo "==================== 操作记录 ===================="
echo "缺陷 #$D 流转一次,再看操作记录(应含 CREATE / ATTACH / TRANSITION):"
post /devtrack/defect/transition "${A[@]}" -d "{\"defectId\":$D,\"transitionCode\":\"confirm\"}" >/dev/null
post /devtrack/defect/activity "${A[@]}" -d "{\"defectId\":$D}" | pretty

echo ""
echo "==================== 批量流转 ===================="
B1=$(post /devtrack/defect/create "${A[@]}" -d '{"projectId":1,"title":"批量1"}' | idof)
B2=$(post /devtrack/defect/create "${A[@]}" -d '{"projectId":1,"title":"批量2"}' | idof)
echo "新建 #$B1 #$B2(NEW);批量 confirm(都应成功):"
post /devtrack/defect/batch-transition "${A[@]}" -d "{\"defectIds\":[$B1,$B2],\"transitionCode\":\"confirm\"}" | pretty
echo "再对 #$B1(已CONFIRMED) 和 #$B2 批量 confirm(应都失败:非法流转):"
post /devtrack/defect/batch-transition "${A[@]}" -d "{\"defectIds\":[$B1,$B2],\"transitionCode\":\"confirm\"}" | pretty

echo ""
echo "==================== 燃尽图理想线 ===================="
IT=$(post /devtrack/iteration/create "${A[@]}" -d '{"projectId":1,"name":"理想线Sprint","startDate":"2026-06-14","endDate":"2026-06-18"}' | idof)
for i in 1 2; do DD=$(post /devtrack/defect/create "${A[@]}" -d "{\"projectId\":1,\"title\":\"燃尽$i\"}" | idof); post /devtrack/defect/assign-iteration "${A[@]}" -d "{\"defectId\":$DD,\"iterationId\":$IT}" >/dev/null; done
echo "燃尽图(每个点应带 idealRemaining 理想线值):"
post /devtrack/iteration/burndown "${A[@]}" -d "{\"iterationId\":$IT}" | pretty

echo ""
echo "==> 完成。"
