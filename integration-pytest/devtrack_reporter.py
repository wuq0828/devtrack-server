"""
DevTrack pytest 上报插件
========================
会话结束时读取 allure-results,把用例结果批量上报 DevTrack;失败用例由服务端
按 historyId 指纹自动建/去重缺陷(见后端 AutomationManager)。

零侵入接入(三选一):
  1) 把本文件拷到 pytest 工程根目录(与 conftest.py 同级),在 conftest.py 顶部加一行:
         pytest_plugins = ("devtrack_reporter",)
  2) 或运行时挂载:  pytest -p devtrack_reporter
  3) 或装成包后用 entry point。

开关与配置(全部走环境变量,不设则自动跳过,绝不阻断 CI):
  DEVTRACK_URL          DevTrack 服务地址,如 http://localhost:8080  (未设则本插件 no-op)
  DEVTRACK_TOKEN        机器账号 API Token(Authorization: Bearer)
  DEVTRACK_PROJECT      项目编码,如 DEMO
  DEVTRACK_ITERATION_ID 关联迭代 id(可选)
  DEVTRACK_ALLURE_DIR   allure 结果目录(默认从 --alluredir 读,兜底 reports/allure-results)
  DEVTRACK_ALLURE_URL   Allure 报告可访问链接(可选)
  CI_RUN_ID             CI 运行标识(可选,失败追溯用)
"""
import glob
import json
import os
import urllib.error
import urllib.request


def _alluredir(config):
    try:
        d = config.getoption("allure_report_dir")
        if d:
            return d
    except Exception:
        pass
    return os.environ.get("DEVTRACK_ALLURE_DIR", "reports/allure-results")


def _collect_results(alluredir):
    results = []
    for path in glob.glob(os.path.join(alluredir, "*-result.json")):
        try:
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception:
            continue
        start, stop = data.get("start"), data.get("stop")
        duration = (stop - start) if (start and stop) else None
        results.append({
            "fullName": data.get("fullName") or data.get("name"),
            "historyId": data.get("historyId"),
            "status": data.get("status"),
            "durationMs": duration,
            "errorMessage": (data.get("statusDetails") or {}).get("message"),
        })
    return results


def pytest_sessionfinish(session, exitstatus):
    base = os.environ.get("DEVTRACK_URL")
    if not base:
        return  # 未配置 -> no-op,绝不影响 CI

    config = session.config
    results = _collect_results(_alluredir(config))
    if not results:
        print("[DevTrack] 未找到 allure 结果,跳过上报")
        return

    payload = {
        "projectCode": os.environ.get("DEVTRACK_PROJECT", "DEMO"),
        "iterationId": _to_int(os.environ.get("DEVTRACK_ITERATION_ID")),
        "ciRunId": os.environ.get("CI_RUN_ID", "local-run"),
        "allureUrl": os.environ.get("DEVTRACK_ALLURE_URL", ""),
        "results": results,
        "autoCreateDefect": True,
    }

    url = base.rstrip("/") + "/devtrack/integrations/test-reports"
    body = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    token = os.environ.get("DEVTRACK_TOKEN")
    if token:
        req.add_header("Authorization", "Bearer " + token)

    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            print("[DevTrack] 已上报 %d 条用例结果 -> %s" % (len(results), resp.read().decode("utf-8")))
    except urllib.error.HTTPError as e:
        print("[DevTrack][WARN] 上报失败 HTTP %s: %s" % (e.code, e.read().decode("utf-8", "ignore")))
    except Exception as e:  # noqa: BLE001  上报失败只告警,绝不阻断 CI
        print("[DevTrack][WARN] 上报异常(已忽略): %s" % e)


def _to_int(v):
    try:
        return int(v) if v else None
    except (TypeError, ValueError):
        return None
