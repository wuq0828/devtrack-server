package com.nx.devtrack.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient(Spring 6.1 / Boot 3.2 自带)用于调飞书 OpenAPI / Webhook。
 * 对齐团队用 WebClient 调微服务的习惯,这里同步场景用更轻的 RestClient。
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
