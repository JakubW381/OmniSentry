package dev.jakubw.omnisentry.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class SaltEdgeConfig {

    @Bean
    public WebClient saltEdgeWebClient(
            @Value("${app.salt-edge.base-url}") String baseUrl,
            @Value("${app.salt-edge.app-id}") String appId,
            @Value("${app.salt-edge.secret}") String secret) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("App-id", appId)
                .defaultHeader("Secret", secret)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
