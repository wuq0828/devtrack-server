package com.nx.devtrack.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端集成测试:全量启动(H2 + DataInitializer 播种),走真实 HTTP。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DevTrackIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper om = new ObjectMapper();

    private JsonNode post(String path, Object body, String token) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            h.setBearerAuth(token);
        }
        ResponseEntity<String> resp = rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, h), String.class);
        return om.readTree(resp.getBody());
    }

    @Test
    void endToEnd_loginCreateTransition() throws Exception {
        JsonNode login = post("/devtrack/auth/login",
                Map.of("username", "admin", "password", "admin123"), null);
        assertThat(login.path("code").asInt()).isZero();
        String token = login.path("data").path("token").asText();
        assertThat(token).isNotBlank();

        JsonNode created = post("/devtrack/defect/create",
                Map.of("projectId", 1, "title", "集成测试缺陷", "priority", "P1"), token);
        assertThat(created.path("code").asInt()).isZero();
        long defectId = created.path("data").path("id").asLong();
        assertThat(defectId).isPositive();

        JsonNode trans = post("/devtrack/defect/transition",
                Map.of("defectId", defectId, "transitionCode", "confirm"), token);
        assertThat(trans.path("code").asInt()).isZero();
        assertThat(trans.path("data").path("statusCode").asText()).isEqualTo("CONFIRMED");
    }

    @Test
    void login_wrongPassword_returnsErrorCode() throws Exception {
        JsonNode login = post("/devtrack/auth/login",
                Map.of("username", "admin", "password", "wrong"), null);
        assertThat(login.path("code").asInt()).isEqualTo(1003);
    }

    @Test
    void protectedEndpoint_withoutToken_returnsNeedLogin() throws Exception {
        JsonNode resp = post("/devtrack/defect/list", Map.of("projectId", 1), null);
        assertThat(resp.path("code").asInt()).isEqualTo(1001);
    }
}
