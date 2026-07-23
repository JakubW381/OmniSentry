package dev.jakubw.omnisentry.service.internal;

import dev.jakubw.omnisentry.dto.ConnectionDto;
import dev.jakubw.omnisentry.dto.LastAttemptDto;
import dev.jakubw.omnisentry.models.ConnectionEntity;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import dev.jakubw.omnisentry.repos.UserRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import dev.jakubw.omnisentry.services.internal.ConnectionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

    @Mock
    private SaltEdgeService saltEdgeService;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConnectionService connectionService;

    @Captor
    private ArgumentCaptor<ConnectionEntity> connectionEntityCaptor;

    @Nested
    @DisplayName("saveConnection() Tests")
    class SaveConnectionTests {

        @Test
        @DisplayName("Should successfully save connection and link to user")
        void shouldSaveConnectionAndLinkToUser() {
            // Given
            String customerId = "cust_123";
            String connectionId = "conn_999";

            UserEntity user = UserEntity.builder()
                    .saltEdgeCustomerId(customerId)
                    .build();

            when(userRepository.findBySaltEdgeCustomerId(customerId))
                    .thenReturn(Optional.of(user));

            LastAttemptDto lastAttempt = new LastAttemptDto("desktop", "192.168.1.1");
            ConnectionDto connectionDto = new ConnectionDto(
                    connectionId,
                    customerId,
                    "mBank",
                    "mbank_pl",
                    LocalDate.now().toString(),
                    lastAttempt,
                    "active"
            );

            when(saltEdgeService.getConnection(connectionId))
                    .thenReturn(Optional.of(connectionDto));

            // When
            connectionService.saveConnection(customerId, connectionId);

            // Then
            verify(connectionRepository).save(connectionEntityCaptor.capture());
            ConnectionEntity capturedConnection = connectionEntityCaptor.getValue();

            assertThat(capturedConnection.getSaltEdgeConnectionId()).isEqualTo(connectionId);
            assertThat(capturedConnection.getUser()).isEqualTo(user);
            assertThat(capturedConnection.getProviderName()).isEqualTo("mBank");
            assertThat(capturedConnection.getProviderCode()).isEqualTo("mbank_pl");
            assertThat(capturedConnection.getStatus()).isEqualTo("active");
            assertThat(capturedConnection.getLastDeviceType()).isEqualTo("desktop");
            assertThat(capturedConnection.getLastRemoteIp()).isEqualTo("192.168.1.1");
            assertThat(capturedConnection.getCreatedAt()).isNotNull();

            assertThat(user.getConnections()).contains(capturedConnection);
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            String customerId = "non_existent_user";
            String connectionId = "conn_123";

            LastAttemptDto lastAttempt = new LastAttemptDto("desktop", "192.168.1.1");
            ConnectionDto connectionDto = new ConnectionDto(
                    connectionId,
                    customerId,
                    "mBank",
                    "mbank_pl",
                    LocalDate.now().toString(),
                    lastAttempt,
                    "active"
            );

            when(saltEdgeService.getConnection(connectionId))
                    .thenReturn(Optional.of(connectionDto));

            when(userRepository.findBySaltEdgeCustomerId(customerId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> connectionService.saveConnection(customerId, connectionId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("User not found with customerId: " + customerId);

            verify(connectionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when connection is not found in SaltEdge")
        void shouldThrowExceptionWhenConnectionNotFoundInSaltEdge() {
            // Given
            String customerId = "cust_123";
            String connectionId = "invalid_conn_id";

            when(saltEdgeService.getConnection(connectionId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> connectionService.saveConnection(customerId, connectionId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Connection not found in SaltEdge: " + connectionId);

            verifyNoInteractions(userRepository, connectionRepository);
        }
    }

    @Nested
    @DisplayName("getConnections() Tests")
    class GetConnectionsTests {

        @Test
        @DisplayName("Should return list of mapped ConnectionDto for given customerId")
        void shouldReturnConnectionsForCustomer() {
            // Given
            String customerId = "cust_123";
            UserEntity user = UserEntity.builder()
                    .saltEdgeCustomerId(customerId)
                    .build();

            ConnectionEntity connection1 = ConnectionEntity.builder()
                    .saltEdgeConnectionId("conn_1")
                    .user(user)
                    .providerName("mBank")
                    .providerCode("mbank_pl")
                    .createdAt(Instant.now().minusSeconds(3600))
                    .lastDeviceType("mobile")
                    .lastRemoteIp("127.0.0.1")
                    .status("active")
                    .build();

            ConnectionEntity connection2 = ConnectionEntity.builder()
                    .saltEdgeConnectionId("conn_2")
                    .user(user)
                    .providerName("PKO")
                    .providerCode("pkobp_pl")
                    .createdAt(Instant.now())
                    .lastDeviceType("desktop")
                    .lastRemoteIp("192.168.0.1")
                    .status("inactive")
                    .build();

            when(connectionRepository.findAllByUserSaltEdgeCustomerId(customerId))
                    .thenReturn(List.of(connection1, connection2));

            // When
            List<ConnectionDto> results = connectionService.getConnections(customerId);

            // Then
            assertThat(results).hasSize(2);

            ConnectionDto dto1 = results.getFirst();
            assertThat(dto1.getConnectionId()).isEqualTo("conn_1");
            assertThat(dto1.getCustomerId()).isEqualTo(customerId);
            assertThat(dto1.getProviderName()).isEqualTo("mBank");
            assertThat(dto1.getLastAttempt().getDeviceType()).isEqualTo("mobile");
            assertThat(dto1.getStatus()).isEqualTo("active");

            ConnectionDto dto2 = results.get(1);
            assertThat(dto2.getConnectionId()).isEqualTo("conn_2");
            assertThat(dto2.getCustomerId()).isEqualTo(customerId);
            assertThat(dto2.getProviderName()).isEqualTo("PKO");
            assertThat(dto2.getLastAttempt().getDeviceType()).isEqualTo("desktop");
            assertThat(dto2.getStatus()).isEqualTo("inactive");
        }
    }

    @Nested
    @DisplayName("removeConnection() Tests")
    class RemoveConnectionTests {

        @Test
        @DisplayName("Should delete connection by ID when user exists")
        void shouldRemoveConnectionSuccessfully() {
            // Given
            String customerId = "cust_123";
            String connectionId = "conn_to_delete";

            when(userRepository.existsBySaltEdgeCustomerId(customerId))
                    .thenReturn(true);

            // When
            connectionService.removeConnection(customerId, connectionId);

            // Then
            verify(connectionRepository, times(1)).deleteById(connectionId);
        }

        @Test
        @DisplayName("Should throw exception when user is not found during removal")
        void shouldThrowExceptionWhenUserNotFoundDuringRemoval() {
            // Given
            String customerId = "missing_user";
            String connectionId = "conn_123";

            when(userRepository.existsBySaltEdgeCustomerId(customerId))
                    .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> connectionService.removeConnection(customerId, connectionId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("User not found with customerId: " + customerId);

            verify(connectionRepository, never()).deleteById(anyString());
        }
    }
}