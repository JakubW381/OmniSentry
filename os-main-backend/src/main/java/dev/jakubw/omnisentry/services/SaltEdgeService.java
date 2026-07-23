package dev.jakubw.omnisentry.services;

import dev.jakubw.omnisentry.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SaltEdgeService {

    private final WebClient webClient;

    public SaltEdgeService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Optional<CustomerDto> createCustomer(String userEmail) {
        Map<String, Object> payload = Map.of(
                "data", Map.of("identifier", userEmail)
        );

        return webClient.post()
                .uri("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("SaltEdge Error: " + errorBody)))
                )
                .bodyToMono(new ParameterizedTypeReference<SaltEdgeResponse<CustomerDto>>() {})
                .map(SaltEdgeResponse::data)
                .blockOptional();
    }

    public String createConnectSession(String customerId, String returnTo) {
        Map<String, Object> payload = Map.of(
                "data", Map.of(
                        "customer_id", customerId,
                        "consent", Map.of("scopes", List.of("accounts", "transactions", "holder_info")),
                        "attempt", Map.of("return_to", returnTo),
                        "automatic_refresh" , true
                )
        );

        return webClient.post()
                .uri("/connections/connect")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    return (String) data.get("connect_url");
                }).blockOptional()
                .orElseThrow(EntityNotFoundException::new);
    }

    public List<AccountDto> getAccounts(String connectionId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/accounts")
                        .queryParam("connection_id", connectionId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SaltEdgeResponse<List<AccountDto>>>() {})
                .map(SaltEdgeResponse::data)
                .blockOptional()
                .orElseGet(List::of);
    }

    public Optional<ConnectionDto> getConnection(String connectionId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/connections/" + connectionId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SaltEdgeResponse<ConnectionDto>>() {})
                .map(SaltEdgeResponse::data)
                .blockOptional();
    }

    public List<ConnectionDto> getConnections(String customerId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/connections")
                        .queryParam("customer_id", customerId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SaltEdgeResponse<List<ConnectionDto>>>() {})
                .map(SaltEdgeResponse::data)
                .blockOptional()
                .orElseGet(List::of);
    }

    public List<TransactionDto> getTransactions(String connectionId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transactions")
                        .queryParam("connection_id", connectionId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SaltEdgeResponse<List<TransactionDto>>>() {})
                .map(SaltEdgeResponse::data)
                .blockOptional()
                .orElseGet(List::of);
    }

    public List<TransactionDto> getTransactions(String connectionId, String fromId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transactions")
                        .queryParam("connection_id", connectionId)
                        .queryParam("from_id", fromId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SaltEdgeResponse<List<TransactionDto>>>() {})
                .map(SaltEdgeResponse::data)
                .blockOptional()
                .orElseGet(List::of);
    }
}
