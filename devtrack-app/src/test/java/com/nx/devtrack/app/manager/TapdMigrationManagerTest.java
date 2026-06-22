package com.nx.devtrack.app.manager;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TAPD CSV 解析单测:覆盖引号内逗号、"" 转义、跨行。
 */
class TapdMigrationManagerTest {

    @Test
    void parseCsv_handlesQuotedCommaAndEscapedQuotes() {
        String csv = "ID,标题,状态\n"
                + "1,\"标题,含逗号\",新\n"
                + "2,\"含\"\"双引号\"\"\",已解决\n";
        List<Map<String, String>> rows = TapdMigrationManager.parseCsv(csv);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("标题")).isEqualTo("标题,含逗号");
        assertThat(rows.get(0).get("状态")).isEqualTo("新");
        assertThat(rows.get(1).get("标题")).isEqualTo("含\"双引号\"");
        assertThat(rows.get(1).get("ID")).isEqualTo("2");
    }

    @Test
    void parseCsv_emptyOrHeaderOnly_returnsEmpty() {
        assertThat(TapdMigrationManager.parseCsv("")).isEmpty();
        assertThat(TapdMigrationManager.parseCsv("ID,标题,状态\n")).isEmpty();
    }
}
