#!/usr/bin/env bash
# DevTrack 第二批功能冒烟:RBAC 权限 + 飞书扫码登录(dev模式) + TAPD 迁移
# 前置:服务已在 http://localhost:8080 运行。
set -uo pipefail
BASE="${DEVTRACK_URL:-http://localhost:8080}"
pretty() { if command -v jq >/dev/null 2>&1; then jq -c .; else cat; fi; }
login() { curl -s -X POST "$BASE/devtrack/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"$1\",\"password\":\"$2\"}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'; }
post() { curl -s -X POST "$BASE$1" -H "Content-Type: application/json" "${@:2}"; }

echo "==================== RBAC 权限 ===================="
ADMIN=$(login admin admin123); GUEST=$(login guest guest123); DEV=$(login dev dev123); QA=$(login qa qa123)
echo "tokens: admin=${ADMIN:0:8}.. guest=${GUEST:0:8}.. dev=${DEV:0:8}.. qa=${QA:0:8}.."

echo "-- a) guest 建缺陷(GUEST 只有 BUG:VIEW,应 1004 无权限)"
post /devtrack/defect/create -H "Authorization: Bearer $GUEST" -d '{"projectId":1,"title":"guest 不该能建"}' | pretty

echo "-- b) guest 看列表(GUEST 有 BUG:VIEW,应成功)"
post /devtrack/defect/list -H "Authorization: Bearer $GUEST" -d '{"projectId":1,"pn":1,"ps":3}' | pretty

echo "-- c) admin 建缺陷并推进到 RESOLVED(为下一步验证准备)"
DID=$(post /devtrack/defect/create -H "Authorization: Bearer $ADMIN" -d '{"projectId":1,"title":"RBAC 演示缺陷","priority":"P1"}' | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
for a in confirm start resolve; do post /devtrack/defect/transition -H "Authorization: Bearer $ADMIN" -d "{\"defectId\":$DID,\"transitionCode\":\"$a\"}" >/dev/null; done
echo "   缺陷 #$DID 已到 RESOLVED"

echo "-- d) dev 验证缺陷(verify 限 QA 角色,dev 应 1006 需要角色)"
post /devtrack/defect/transition -H "Authorization: Bearer $DEV" -d "{\"defectId\":$DID,\"transitionCode\":\"verify\"}" | pretty

echo "-- e) qa 验证缺陷(QA 角色,应成功 -> VERIFIED)"
post /devtrack/defect/transition -H "Authorization: Bearer $QA" -d "{\"defectId\":$DID,\"transitionCode\":\"verify\"}" | pretty

echo ""
echo "==================== 飞书扫码登录(dev 模式)===================="
echo "-- f) 查授权 URL(未配 appId,应返回 devMode 提示)"
curl -s "$BASE/devtrack/auth/feishu/authorize-url" | pretty
echo "-- g) 用 code 登录(dev 模式 code 即 open_id,应签发 token 并自动建号)"
curl -s -X POST "$BASE/devtrack/auth/feishu/login?code=ou_demo_user_888" | pretty

echo ""
echo "==================== TAPD 迁移 ===================="
echo "-- h) admin 上传 TAPD 样例 CSV(5 行,1 行无标题外都应导入;含 1 个未映射状态)"
curl -s -X POST "$BASE/devtrack/migration/tapd" -H "Authorization: Bearer $ADMIN" \
  -F "file=@migration-samples/tapd_sample.csv" -F "projectId=1" | pretty
echo "-- i) 再次上传同文件(tapd:id 幂等去重,created 应为 0)"
curl -s -X POST "$BASE/devtrack/migration/tapd" -H "Authorization: Bearer $ADMIN" \
  -F "file=@migration-samples/tapd_sample.csv" -F "projectId=1" | pretty
echo "-- j) guest 尝试迁移(无 MIGRATION:RUN,应 1004)"
curl -s -X POST "$BASE/devtrack/migration/tapd" -H "Authorization: Bearer $GUEST" \
  -F "file=@migration-samples/tapd_sample.csv" -F "projectId=1" | pretty

echo ""
echo "==> 完成。"
