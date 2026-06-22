package com.nx.devtrack.app.ai;

import com.nx.devtrack.common.dto.GenCaseDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 启发式兜底:把 PRD 按行/句切成场景,每个场景生成 正常流程 + 异常流程 两条用例草稿。
 * 无需任何外部依赖,保证 AI 不可用时仍能产出可用的用例骨架。
 */
@Component
public class HeuristicTestCaseGenerator implements TestCaseGenerator {

    private static final int MAX_CASES = 16;

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String engine() {
        return "heuristic";
    }

    @Override
    public List<GenCaseDto> generate(String prd) {
        List<GenCaseDto> cases = new ArrayList<>();
        if (prd == null) {
            return cases;
        }
        for (String raw : prd.split("[\\n。;；]")) {
            String line = raw.trim();
            if (line.length() < 4) {
                continue;
            }
            String scenario = line.length() > 40 ? line.substring(0, 40) : line;
            cases.add(buildCase("[正常] " + scenario,
                    "已登录并进入相关功能页面",
                    "1. 按需求执行:" + scenario + "\n2. 提交/确认操作",
                    "功能按需求预期正确响应,无报错"));
            cases.add(buildCase("[异常] " + scenario,
                    "已登录并进入相关功能页面",
                    "1. 用非法/边界输入执行:" + scenario + "\n2. 提交",
                    "系统给出明确校验提示,不产生脏数据"));
            if (cases.size() >= MAX_CASES) {
                break;
            }
        }
        if (cases.isEmpty()) {
            cases.add(buildCase("基础冒烟用例", "已登录", "1. 打开功能\n2. 执行核心操作", "核心流程可用"));
        }
        return cases;
    }

    private GenCaseDto buildCase(String title, String pre, String steps, String expected) {
        GenCaseDto c = new GenCaseDto();
        c.setTitle(title);
        c.setPreconditions(pre);
        c.setSteps(steps);
        c.setExpected(expected);
        return c;
    }
}
