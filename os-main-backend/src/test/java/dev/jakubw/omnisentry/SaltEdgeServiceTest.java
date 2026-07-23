package dev.jakubw.omnisentry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.jakubw.omnisentry.dto.*;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.wiremock.spring.EnableWireMock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.salt-edge.base-url=${wiremock.server.baseUrl}"
        }
)
@EnableWireMock
public class SaltEdgeServiceTest {

    @Autowired
    private SaltEdgeService saltEdgeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    public static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("app.salt-edge.base-url", () -> "${wiremock.server.baseUrl}");
        registry.add("app.salt-edge.app-id", () -> "test-id");
        registry.add("app.salt-edge.secret", () -> "test-secret");

        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    public void shouldCreateACustomer() throws JsonProcessingException {
        // Given
        String userEmail = "john@doe.com";
        String id = "123";

        Map<String, Object> payload = Map.of(
                "data", Map.of("identifier", userEmail)
        );
        String requestJson = objectMapper.writeValueAsString(payload);

        Map<String, Object> responseMap = Map.of(
                "data", Map.of(
                        "customer_id", id,
                        "identifier", userEmail
                )
        );
        String responseJson = objectMapper.writeValueAsString(responseMap);

        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/customers"))
                .withRequestBody(equalToJson(requestJson))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)
                )
        );

        // When
        Optional<CustomerDto> resultOpt = saltEdgeService.createCustomer(userEmail);

        // Then
        assertThat(resultOpt).isPresent();
        CustomerDto result = resultOpt.get();
        assertThat(result.customerId()).isEqualTo(id);
        assertThat(result.identifier()).isEqualTo(userEmail);
    }

    @Test
    public void shouldCreateConnectionSession() throws JsonProcessingException {
        // Given
        String customerId = "123";
        String returnTo = "http://localhost";
        String someConnectUrl = "https://" + UUID.randomUUID() + "/" + customerId;
        Map<String, Object> payload = Map.of(
                "data", Map.of(
                        "customer_id", customerId,
                        "consent", Map.of("scopes", List.of("accounts", "transactions", "holder_info")),
                        "attempt", Map.of("return_to", returnTo),
                        "automatic_refresh", true
                )
        );
        String requestJson = objectMapper.writeValueAsString(payload);

        Map<String, Object> responseMap = Map.of(
                "data", Map.of(
                        "connect_url", someConnectUrl
                )
        );
        String responseJson = objectMapper.writeValueAsString(responseMap);

        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/connections/connect"))
                .withRequestBody(equalToJson(requestJson))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)
                )
        );

        // When
        String connectionUrl = saltEdgeService.createConnectSession(customerId, returnTo);

        // Then
        String id = List.of(connectionUrl.split("/")).getLast();
        assertEquals(someConnectUrl, connectionUrl);
        assertEquals(customerId, id);
    }

    @Test
    public void shouldGetAccounts() throws JsonProcessingException {
        // Given
        String connectionId = "conn_999";
        List<Map<String, Object>> accountList = List.of(
                Map.of("id", "123123", "connection_id", "conn_999", "name", "Savings", "balance", BigDecimal.ONE, "currency_code", "EUR", "extra", Map.of("iban", "IBAN3123"), "nature", "someNature", "created_at", "someDate", "updated_at", "someDate"),
                Map.of("id", "321321", "connection_id", "conn_999", "name", "Normal", "balance", BigDecimal.ONE, "currency_code", "EUR", "extra", Map.of("iban", "IBAN3123"), "nature", "someNature", "created_at", "someDate", "updated_at", "someDate"),
                Map.of("id", "333333", "connection_id", "conn_999", "name", "Stocks", "balance", BigDecimal.ONE, "currency_code", "EUR", "extra", Map.of("iban", "IBAN3123"), "nature", "someNature", "updated_at", "someDate", "created_at", "someDate")
        );

        Map<String, Object> responseMap = Map.of(
                "data", accountList
        );
        String responseJson = objectMapper.writeValueAsString(responseMap);

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/accounts"))
                .withQueryParam("connection_id", WireMock.equalTo(connectionId))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)
                )
        );

        // When
        List<AccountDto> accounts = saltEdgeService.getAccounts(connectionId);

        // Then
        assertThat(accounts).hasSize(3);
        assertThat(accounts.get(0).getSaltEdgeAccountId()).isEqualTo("123123");
        assertThat(accounts.get(1).getSaltEdgeAccountId()).isEqualTo("321321");
        assertThat(accounts.get(2).getSaltEdgeAccountId()).isEqualTo("333333");
    }

    @Test
    public void shouldGetSingleConnection() throws JsonProcessingException {
        // Given
        String connectionId = "conn_123";
        Map<String, Object> responseMap = Map.of(
                "data", Map.of("id", connectionId, "customer_id", "123", "provider_name", "Some provider", "provider_code", "some code", "status", "active", "created_at", "some date", "last_attempt", Map.of())
        );
        String responseJson = objectMapper.writeValueAsString(responseMap);

        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/connections/" + connectionId))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)
                )
        );

        // When
        Optional<ConnectionDto> connectionOpt = saltEdgeService.getConnection(connectionId);

        // Then
        assertThat(connectionOpt).isPresent();
        assertThat(connectionOpt.get().getConnectionId()).isEqualTo(connectionId);
    }

    @Test
    public void shouldGetConnectionsForCustomer() throws JsonProcessingException {
        // Given
        String customerId = "cust_555";
        Map<String, Object> responseMap = Map.of(
                "data", List.of(
                        Map.of("id", "123", "customer_id", customerId, "provider_name", "Some provider", "provider_code", "some code", "status", "active", "created_at", "some date", "last_attempt", Map.of()),
                        Map.of("id", "124", "customer_id", customerId, "provider_name", "Some provider", "provider_code", "some code", "status", "active", "created_at", "some date", "last_attempt", Map.of())
                )
        );
        String responseJson = objectMapper.writeValueAsString(responseMap);

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/connections"))
                .withQueryParam("customer_id", WireMock.equalTo(customerId))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)
                )
        );

        // When
        List<ConnectionDto> connections = saltEdgeService.getConnections(customerId);

        // Then
        assertThat(connections).hasSize(2);
    }

    @Test
    public void shouldGetTransactionsAfterId() throws JsonProcessingException {
        // Given
        String connectionId = "conn_777";
        String fromId = "tx_000";

        Map<String, Object> responseMap = Map.of(
                "data", List.of(
                        Map.of("id", "tx_1", "account_id", "123", "amount", 100.0, "currency_code", "EUR", "description", "some desc", "made_on", "today", "status", "active")
                )
        );
        String responseJson = objectMapper.writeValueAsString(responseMap);

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/transactions"))
                .withQueryParam("connection_id", WireMock.equalTo(connectionId))
                .withQueryParam("from_id", WireMock.equalTo(fromId))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)
                )
        );

        // When
        List<TransactionDto> transactions = saltEdgeService.getTransactions(connectionId, fromId);

        // Then
        assertThat(transactions).hasSize(1);
        assertThat(transactions.getFirst().getTransactionId()).isEqualTo("tx_1");
    }
}