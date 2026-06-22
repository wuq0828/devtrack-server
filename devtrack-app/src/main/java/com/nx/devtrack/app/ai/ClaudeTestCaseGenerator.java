package com.nx.devtrack.app.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nx.devtrack.common.dto.GenCaseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用 Claude(claude-opus-4-8 + 自适应思考)从 PRD 生成测试用例。
 * 仅当环境变量 ANTHROPIC_API_KEY 存在时可用;否则由 HeuristicTestCaseGenerator 兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeTestCaseGenerator implements TestCaseGenerator {

    private static final String PROMPT_HEADER = """
            你是资深测试工程师。根据下面的 PRD/需求,先拆分关键场景,再生成测试用例。
            只输出一个 JSON 数组,不要任何解释、不要 markdown 代码块。每个元素形如:
            {"title":"用例标题","preconditions":"前置条件","steps":"操作步骤(可多行,用\\n分隔)","expected":"预期结果"}
            覆盖正常流程、边界值、异常输入与权限场景,中文,生成 8-15 条。

            需求:
            """;

    private final ObjectMapper objectMapper;

    @Override
    public boolean available() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        return key != null && !key.isBlank();
    }

    @Override
    public String engine() {
        return "claude";
    }

    @Override
    public List<GenCaseDto> generate(String prd) {
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        MessageCreateParams params = MessageCreateParams.builder()
                .model("claude-opus-4-8")
                .maxTokens(16000L)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .addUserMessage(PROMPT_HEADER + prd)
                .build();
        Message response = client.messages().create(params);
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : response.content()) {
            block.text().ifPresent(t -> sb.append(t.text()));
        }
        return parse(sb.toString());
    }

    private List<GenCaseDto> parse(String text) {
        try {
            int start = text.indexOf('[');
            int end = text.lastIndexOf(']');
            String json = (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
            return objectMapper.readValue(json, new TypeReference<List<GenCaseDto>>() {
            });
        } catch (Exception e) {
            log.warn("[AI] 解析模型输出失败: {}", e.getMessage());
            return List.of();
        }
    }
}
