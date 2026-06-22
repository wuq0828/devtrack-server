# DevTrack × pytest 自动建单集成

把 `devtrack_reporter.py` 挂到你现有的 pytest 框架(`~/Desktop/pytest_requests_api`),
**用例失败时自动在 DevTrack 建缺陷,并用 Allure `historyId` 做指纹去重**(同一失败反复跑不重复建单)。

## 接入(零侵入)

1. 拷贝插件到 pytest 工程根目录(与 `conftest.py` 同级):
   ```bash
   cp ~/Desktop/devtrack-server/integration-pytest/devtrack_reporter.py ~/Desktop/pytest_requests_api/
   ```
2. 在 `conftest.py` 顶部加一行(你现有的钩子和 fixture 都不用动):
   ```python
   pytest_plugins = ("devtrack_reporter",)
   ```
   或者完全不改代码,运行时挂载:`pytest -p devtrack_reporter`

## 运行

```bash
cd ~/Desktop/pytest_requests_api
export DEVTRACK_URL=http://localhost:8080
export DEVTRACK_TOKEN=dvt_live_demo_token
export DEVTRACK_PROJECT=DEMO
export CI_RUN_ID=local-$(date +%s)
pytest                      # 你现有的 pytest.ini 已配 --alluredir=reports/allure-results
```

- 会话结束后,插件读取 `reports/allure-results/*-result.json`,把结果 POST 到 DevTrack。
- 失败用例 → 服务端 `AutomationManager` 自动建 `source=AUTOMATION` 缺陷;再次失败 → 命中去重只追加,不重建。
- **未设 `DEVTRACK_URL` 时插件直接 no-op,绝不阻断 CI**。上报异常也只 `[DevTrack][WARN]` 告警。

## 环境变量

| 变量 | 必填 | 说明 |
|---|---|---|
| `DEVTRACK_URL` | 是(否则跳过) | DevTrack 服务地址 |
| `DEVTRACK_TOKEN` | 是 | 机器账号 API Token(对应后端 `devtrack.integration.api-token`) |
| `DEVTRACK_PROJECT` | 否 | 项目编码,默认 `DEMO` |
| `DEVTRACK_ITERATION_ID` | 否 | 关联迭代 id |
| `DEVTRACK_ALLURE_DIR` | 否 | allure 结果目录,默认从 `--alluredir` 读,兜底 `reports/allure-results` |
| `DEVTRACK_ALLURE_URL` | 否 | Allure 报告可访问链接,写进缺陷描述 |
| `CI_RUN_ID` | 否 | CI 运行标识 |

## 去重原理

失败用例以 `external_ref = "allure:" + historyId` 作指纹。服务端先查「同项目、同指纹、未关闭」的缺陷:
命中则跳过(生产会追加一条「再次失败于 {ciRunId}」评论),否则建新单。
失败率 ≥30%(且总数≥5)判定为环境性大面积失败,只建 1 个汇总单,避免刷屏。
