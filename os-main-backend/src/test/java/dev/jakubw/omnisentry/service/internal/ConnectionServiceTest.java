package dev.jakubw.omnisentry.service.internal;

import dev.jakubw.omnisentry.dto.ConnectionDto;
import dev.jakubw.omnisentry.dto.LastAttemptDto;
import dev.jakubw.omnisentry.models.ConnectionEntity;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import dev.jakubw.omnisentry.repos.TransactionRepository;
import dev.jakubw.omnisentry.repos.UserRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import dev.jakubw.omnisentry.services.internal.ConnectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;

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

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ConnectionService connectionService;

    @Captor
    private ArgumentCaptor<ConnectionEntity> connectionEntityCaptor;

    @Captor
    private ArgumentCaptor<UserEntity> userEntityCaptor;

    @Nested
    @DisplayName("saveConnection() Tests")
    class SaveConnectionTests {

        @Test
        @DisplayName("Should successfully save connection and update user's connection list")
        void shouldSaveConnectionAndLinkToUser() {
            // Given
            String customerId = "cust_123";
            String connectionId = "conn_999";

            UserEntity user = UserEntity.builder()
                    .customerId(customerId)
                    .connectionIds(new HashSet<>())
                    .build();

            when(userRepository.findByCustomerId(customerId))
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
                    .thenReturn(Mono.just(connectionDto));

            ConnectionEntity savedConnectionInDb = ConnectionEntity.builder()
                    .saltEdgeConnectionId(connectionId)
                    .customerId(customerId)
                    .providerName("mBank")
                    .providerCode("mbank_pl")
                    .status("active")
                    .lastDeviceType("desktop")
                    .lastRemoteIp("192.168.1.1")
                    .build();

            when(connectionRepository.save(any(ConnectionEntity.class)))
                    .thenReturn(savedConnectionInDb);

            // When
            connectionService.saveConnection(customerId, connectionId);

            // Then 1
            verify(connectionRepository).save(connectionEntityCaptor.capture());
            ConnectionEntity capturedConnection = connectionEntityCaptor.getValue();

            assertThat(capturedConnection.getSaltEdgeConnectionId()).isEqualTo(connectionId);
            assertThat(capturedConnection.getCustomerId()).isEqualTo(customerId);
            assertThat(capturedConnection.getProviderName()).isEqualTo("mBank");
            assertThat(capturedConnection.getStatus()).isEqualTo("active");
            assertThat(capturedConnection.getCreatedAt()).isNotNull();

            // Then 2
            verify(userRepository).save(userEntityCaptor.capture());
            UserEntity capturedUser = userEntityCaptor.getValue();

            assertThat(capturedUser.getConnectionIds()).containsExactly(connectionId);
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            String customerId = "non_existent_user";
            String connectionId = "conn_123";

            when(userRepository.findByCustomerId(customerId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> connectionService.saveConnection(customerId, connectionId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");

            verifyNoInteractions(saltEdgeService, connectionRepository);
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
            ConnectionEntity connection1 = ConnectionEntity.builder()
                    .saltEdgeConnectionId("conn_1")
                    .customerId(customerId)
                    .providerName("mBank")
                    .providerCode("mbank_pl")
                    .createdAt("2026-07-17T11:00:00Z")
                    .lastDeviceType("mobile")
                    .lastRemoteIp("127.0.0.1")
                    .status("active")
                    .build();

            ConnectionEntity connection2 = ConnectionEntity.builder()
                    .saltEdgeConnectionId("conn_2")
                    .customerId(customerId)
                    .providerName("PKO")
                    .providerCode("pkobp_pl")
                    .createdAt("2026-07-17T12:00:00Z")
                    .lastDeviceType("desktop")
                    .lastRemoteIp("192.168.0.1")
                    .status("inactive")
                    .build();

            when(connectionRepository.findAllByCustomerId(customerId))
                    .thenReturn(List.of(connection1, connection2));

            // When
            List<ConnectionDto> results = connectionService.getConnections(customerId);

            // Then
            assertThat(results).hasSize(2);

            ConnectionDto dto1 = results.getFirst();
            assertThat(dto1.getConnectionId()).isEqualTo("conn_1");
            assertThat(dto1.getProviderName()).isEqualTo("mBank");
            assertThat(dto1.getLastAttempt().getDeviceType()).isEqualTo("mobile");
            assertThat(dto1.getStatus()).isEqualTo("active");

            ConnectionDto dto2 = results.get(1);
            assertThat(dto2.getConnectionId()).isEqualTo("conn_2");
            assertThat(dto2.getProviderName()).isEqualTo("PKO");
            assertThat(dto2.getLastAttempt().getDeviceType()).isEqualTo("desktop");
            assertThat(dto2.getStatus()).isEqualTo("inactive");
        }
    }

    @Nested
    @DisplayName("removeConnection() Tests")
    class RemoveConnectionTests {

        @Test
        @DisplayName("Should clean up database and update user when removing connection")
        void shouldRemoveConnectionSuccessfully() {
            // Given
            String customerId = "cust_123";
            String connectionId = "conn_to_delete";

            Set<String> userConnections = new HashSet<>(Set.of("conn_to_keep", connectionId));
            UserEntity user = UserEntity.builder()
                    .customerId(customerId)
                    .connectionIds(userConnections)
                    .build();

            when(userRepository.findByCustomerId(customerId))
                    .thenReturn(Optional.of(user));

            // When
            connectionService.removeConnection(customerId, connectionId);

            // Then 1
            verify(transactionRepository, times(1)).deleteAllBySaltEdgeConnectionId(connectionId);
            verify(connectionRepository, times(1)).deleteBySaltEdgeConnectionId(connectionId);

            // Then 2
            verify(userRepository).save(userEntityCaptor.capture());
            UserEntity savedUser = userEntityCaptor.getValue();

            assertThat(savedUser.getConnectionIds())
                    .hasSize(1)
                    .containsExactly("conn_to_keep")
                    .doesNotContain(connectionId);
        }

        @Test
        @DisplayName("Should throw exception when user is not found during removal")
        void shouldThrowExceptionWhenUserNotFoundDuringRemoval() {
            // Given
            String customerId = "missing_user";
            String connectionId = "conn_123";

            when(userRepository.findByCustomerId(customerId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> connectionService.removeConnection(customerId, connectionId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");

            verifyNoInteractions(transactionRepository, connectionRepository);
        }
    }
}