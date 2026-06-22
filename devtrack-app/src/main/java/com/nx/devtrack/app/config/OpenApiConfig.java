package com.nx.devtrack.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 文档。访问 /swagger-ui.html(UI)或 /v3/api-docs(JSON)。
 * 这些路径不以 /devtrack/ 开头,AuthTokenFilter 自动放行。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI devtrackOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DevTrack API")
                        .description("DevTrack 缺陷管理 / 团队协作平台接口文档")
                        .version("v1"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("登录后返回的 token,放入 Authorization: Bearer {token}")));
    }
}
