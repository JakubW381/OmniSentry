package dev.jakubw.omnisentry.service.grpc;

import dev.jakubw.omnisentry.dto.TransactionDto;
import dev.jakubw.omnisentry.models.ConnectionEntity;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.proto.Transactions;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import dev.jakubw.omnisentry.services.grpc.TransactionHistoryGrpcService;
import dev.jakubw.omnisentry.services.internal.TransactionService;
import dev.jakubw.omnisentry.services.internal.UserService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionHistoryGrpcServiceTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private StreamObserver<Transactions.TransactionList> responseObserver;

    @InjectMocks
    private TransactionHistoryGrpcService grpcService;

    @Captor
    private ArgumentCaptor<Transactions.TransactionList> responseCaptor;

    @Captor
    private ArgumentCaptor<Throwable> errorCaptor;

    @Test
    @DisplayName("Should return transaction list when user is authorized")
    void shouldReturnTransactionListWhenAuthorized() {
        // Given
        String customerId = "cust_123";
        String connectionId = "conn_456";

        Transactions.HistoryRequest request = Transactions.HistoryRequest.newBuilder()
                .setCustomerId(customerId)
                .setConnectionId(connectionId)
                .setMonthsLimit(3)
                .build();

        when(connectionRepository.existsBySaltEdgeConnectionIdAndUserSaltEdgeCustomerId(connectionId, customerId))
                .thenReturn(true);

        TransactionDto txDto = new TransactionDto(
                "tx_001", "acc_1", BigDecimal.TEN, "EUR",
                "Groceries", "food", "2026-07-17", "OK", Map.of()
        );

        when(transactionService.getTransactionsAfter(eq(connectionId), any(LocalDate.class)))
                .thenReturn(List.of(txDto));

        // When
        grpcService.getHistory(request, responseObserver);

        // Then
        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        Transactions.TransactionList response = responseCaptor.getValue();

        assertThat(response.getTransactionsCount()).isEqualTo(1);

        Transactions.TransactionDto protoTx = response.getTransactions(0);
        assertThat(protoProtoTxMatchDto(protoTx, txDto)).isTrue();

        verify(responseObserver, times(1)).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    @Test
    @DisplayName("Should return PERMISSION_DENIED when connectionId is not linked to user")
    void shouldReturnPermissionDeniedWhenUnauthorized() {
        // Given
        String customerId = "cust_123";
        String connectionId = "conn_456";

        Transactions.HistoryRequest request = Transactions.HistoryRequest.newBuilder()
                .setCustomerId(customerId)
                .setConnectionId("hacker_connection_id")
                .build();

        ConnectionEntity connection = ConnectionEntity.builder().saltEdgeConnectionId(connectionId)
                .providerName("provider")
                .providerCode("123123")
                .status("Cool")
                .createdAt(Instant.now())
                .build();

        UserEntity user = UserEntity.builder()
                .saltEdgeCustomerId(customerId)
                .build();
        user.addConnection(connection);


        // When
        grpcService.getHistory(request, responseObserver);

        // Then
        verify(responseObserver, times(1)).onError(errorCaptor.capture());
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();

        Throwable thrown = errorCaptor.getValue();
        assertThat(thrown).isInstanceOf(StatusRuntimeException.class);

        StatusRuntimeException grpcException = (StatusRuntimeException) thrown;
        assertThat(grpcException.getStatus().getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(grpcException.getStatus().getDescription()).contains("User is not associated with this connection");
    }

    @Test
    @DisplayName("Should return INTERNAL error when internal service throws exception")
    void shouldReturnInternalErrorOnException() {
        // Given
        String customerId = "cust_123";
        String connectionId = "conn_456";

        Transactions.HistoryRequest request = Transactions.HistoryRequest.newBuilder()
                .setCustomerId(customerId)
                .setConnectionId(connectionId)
                .build();

        when(connectionRepository.existsBySaltEdgeConnectionIdAndUserSaltEdgeCustomerId(connectionId, customerId))
                .thenReturn(true);

        when(transactionService.getTransactionsAfter(eq(connectionId), any(LocalDate.class)))
                .thenThrow(new RuntimeException("Database timeout"));

        // When
        grpcService.getHistory(request, responseObserver);

        // Then
        verify(responseObserver, times(1)).onError(errorCaptor.capture());

        StatusRuntimeException grpcException = (StatusRuntimeException) errorCaptor.getValue();
        assertThat(grpcException.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(grpcException.getStatus().getDescription()).contains("Internal server error: Database timeout");
    }

    private boolean protoProtoTxMatchDto(Transactions.TransactionDto proto, TransactionDto dto) {
        return proto.getTransactionId().equals(dto.getTransactionId()) &&
                proto.getAmount() == dto.getAmount().doubleValue() &&
                proto.getDescription().equals(dto.getDescription());
    }
}