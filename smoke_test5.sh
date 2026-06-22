#!/usr/bin/env bash
# DevTrack Phase 4 冒烟:需求 + 测试用例 + 三向关联 + Swagger/API 文档
# 前置:服务已在 http://localhost:8080 运行。
set -uo pipefail
BASE="${DEVTRACK_URL:-http://localhost:8080}"
pretty() { if command -v jq >/dev/null 2>&1; then jq -c .; else cat; fi; }
post() { curl -s -X POST "$BASE$1" -H "Content-Type: application/json" "${@:2}"; }
idof() { sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1; }

ADMIN=$(post /devtrack/auth/login -d '{"username":"admin","password":"admin123"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
A=(-H "Authorization: Bearer $ADMIN")

echo "==================== 需求 ===================="
RQ=$(post /devtrack/requirement/create "${A[@]}" -d '{"projectId":1,"title":"支持微信登录","description":"接入微信 OAuth","priority":"P1"}')
echo "建需求: $(echo "$RQ" | pretty)"; RQID=$(echo "$RQ" | idof)
echo "需求列表:"; post /devtrack/requirement/list "${A[@]}" -d '{"projectId":1}' | pretty

echo ""
echo "==================== 测试用例 ===================="
TC=$(post /devtrack/testcase/create "${A[@]}" -d '{"projectId":1,"title":"微信登录-正常流程","preconditions":"已安装微信","steps":"1.点微信登录 2.授权","expected":"登录成功跳首页","automationKey":"testcases/test_login.py::test_wx_login"}')
echo "建用例: $(echo "$TC" | pretty)"; TCID=$(echo "$TC" | idof)
echo "改用例状态 -> PASS:"; post /devtrack/testcase/update-status "${A[@]}" -d "{\"testCaseId\":$TCID,\"status\":\"PASS\"}" | pretty
echo "非法状态(应 9001):"; post /devtrack/testcase/update-status "${A[@]}" -d "{\"testCaseId\":$TCID,\"status\":\"XXX\"}" | pretty
echo "用例列表:"; post /devtrack/testcase/list "${A[@]}" -d '{"projectId":1}' | pretty

echo ""
echo "==================== 三向关联 ===================="
DF=$(post /devtrack/defect/create "${A[@]}" -d '{"projectId":1,"title":"微信登录偶发失败","priority":"P1"}' | idof)
echo "用例 #$TCID 验证缺陷 #$DF(VERIFIES):"
post /devtrack/relation/link "${A[@]}" -d "{\"sourceType\":\"TEST_CASE\",\"sourceId\":$TCID,\"targetType\":\"DEFECT\",\"targetId\":$DF,\"relationType\":\"VERIFIES\"}" | pretty
echo "用例 #$TCID 覆盖需求 #$RQID(COVERS):"
RL=$(post /devtrack/relation/link "${A[@]}" -d "{\"sourceType\":\"TEST_CASE\",\"sourceId\":$TCID,\"targetType\":\"REQUIREMENT\",\"targetId\":$RQID,\"relationType\":\"COVERS\"}")
echo "$RL" | pretty; RLID=$(echo "$RL" | idof)
echo "查用例 #$TCID 的关联(应 2 条,带标题):"
post /devtrack/relation/list "${A[@]}" -d "{\"entityType\":\"TEST_CASE\",\"entityId\":$TCID}" | pretty
echo "重复 link(应幂等,返回同一条不重复建):"
post /devtrack/relation/link "${A[@]}" -d "{\"sourceType\":\"TEST_CASE\",\"sourceId\":$TCID,\"targetType\":\"REQUIREMENT\",\"targetId\":$RQID,\"relationType\":\"COVERS\"}" | idof | sed 's/^/   relationId=/'
echo "解除一条关联 #$RLID:"; post /devtrack/relation/unlink "${A[@]}" -d "{\"relationId\":$RLID}" | pretty
echo "再查关联(应剩 1 条):"; post /devtrack/relation/list "${A[@]}" -d "{\"entityType\":\"TEST_CASE\",\"entityId\":$TCID}" | pretty

echo ""
echo "==================== API 文档 / Swagger ===================="
echo "GET /v3/api-docs(应是 openapi JSON):"
curl -s "$BASE/v3/api-docs" | head -c 120; echo
echo "GET /swagger-ui/index.html HTTP 码(应 200):"
curl -s -o /dev/null -w "%{http_code}\n" "$BASE/swagger-ui/index.html"

echo ""
echo "==> 完成。"
