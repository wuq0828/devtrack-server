#!/usr/bin/env bash
# DevTrack Phase 8 冒烟:AI 生成用例(启发式兜底)+ 评审入库 + 多端标签 + SLA + 缺陷查重 + 版本
# 前置:服务已在 http://localhost:8080 运行(本地无 ANTHROPIC_API_KEY 时 AI 走启发式兜底)。
set -uo pipefail
BASE="${DEVTRACK_URL:-http://localhost:8080}"
pretty() { if command -v jq >/dev/null 2>&1; then jq -c .; else cat; fi; }
post() { curl -s -X POST "$BASE$1" -H "Content-Type: application/json" "${@:2}"; }
idof() { sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1; }

ADMIN=$(post /devtrack/auth/login -d '{"username":"admin","password":"admin123"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
A=(-H "Authorization: Bearer $ADMIN")

echo "==================== AI 生成用例 + 评审入库 ===================="
echo "-- 从 PRD 生成(无 key 走 heuristic;有 key 走 claude)"
GEN=$(post /devtrack/ai/gen-cases "${A[@]}" -d '{"projectId":1,"prd":"用户可以用手机号+验证码登录。登录后能查看个人余额。余额不足时充值入口要置灰。"}')
echo "$GEN" | sed -n 's/.*"engine":"\([^"]*\)".*/   engine=\1/p'
echo "   生成用例数: $(echo "$GEN" | grep -o '"title"' | wc -l | tr -d ' ')"
echo "-- 评审入库(取前两条标题入库)"
post /devtrack/ai/save-cases "${A[@]}" -d '{"projectId":1,"cases":[{"title":"[正常] 手机号验证码登录","preconditions":"已注册","steps":"输入手机号→获取验证码→提交","expected":"登录成功"},{"title":"[异常] 验证码错误","preconditions":"已注册","steps":"输入错误验证码","expected":"提示验证码错误"}]}' | pretty

echo ""
echo "==================== 多端标签 ===================="
echo "-- 项目标签(应有播种的 iOS/Android/线上/性能):"
post /devtrack/tag/list "${A[@]}" -d '{"projectId":1}' | pretty
D=$(post /devtrack/defect/create "${A[@]}" -d '{"projectId":1,"title":"标签演示缺陷"}' | idof)
T1=$(post /devtrack/tag/list "${A[@]}" -d '{"projectId":1}' | idof)
NEW=$(post /devtrack/tag/create "${A[@]}" -d '{"projectId":1,"name":"回归","color":"#909399"}' | idof)
echo "-- 给缺陷 #$D 打标签 [$T1, $NEW]:"
post /devtrack/defect/set-tags "${A[@]}" -d "{\"defectId\":$D,\"tagIds\":[$T1,$NEW]}" | pretty
echo "-- 查缺陷标签(应 2 个):"; post /devtrack/defect/tags "${A[@]}" -d "{\"defectId\":$D}" | pretty

echo ""
echo "==================== 缺陷查重 ===================="
post /devtrack/defect/create "${A[@]}" -d '{"projectId":1,"title":"登录按钮点击无响应"}' >/dev/null
post /devtrack/defect/create "${A[@]}" -d '{"projectId":1,"title":"登录按钮点击没有反应"}' >/dev/null
echo "-- 查与「登录按钮无响应」相似(应命中上面两条):"
post /devtrack/defect/find-duplicates "${A[@]}" -d '{"projectId":1,"keyword":"登录按钮无响应"}' | sed -n 's/.*\(\("title":"[^"]*"[, ]*\)\{1,3\}\).*/\1/p' | head -c 200; echo

echo ""
echo "==================== 版本关联 ===================="
V=$(post /devtrack/version/create "${A[@]}" -d '{"projectId":1,"name":"v1.2.0"}' | idof)
echo "建版本 v1.2.0 (#$V);列表:"; post /devtrack/version/list "${A[@]}" -d '{"projectId":1}' | pretty
echo "-- 缺陷 #$D 关联修复版本 #$V:"
post /devtrack/defect/set-version "${A[@]}" -d "{\"defectId\":$D,\"versionId\":$V}" | sed -n 's/.*\("fixVersionId":[0-9]*\).*/   \1/p'

echo ""
echo "==================== SLA 超期预警 ===================="
echo "-- 项目 SLA 概览(演示数据创建即今天,通常无超期 -> items 空):"
post /devtrack/sla/overdue "${A[@]}" -d '{"projectId":1}' | head -c 200; echo

echo ""
echo "==> 完成。"
