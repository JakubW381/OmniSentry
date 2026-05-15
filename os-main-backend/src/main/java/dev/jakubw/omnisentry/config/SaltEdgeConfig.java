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
            @Value("${app.salt-edge.app-id}") String appId,
            @Value("${app.salt-edge.secret}") String secret) {

        log.info("Initializing SaltEdge WebClient");
        log.info("App ID: {}", appId);
        log.info("Secret: {}", secret);
        return WebClient.builder()
                .baseUrl("https://www.saltedge.com/api/v6")
                .defaultHeader("App-id", appId)
                .defaultHeader("Secret", secret)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
