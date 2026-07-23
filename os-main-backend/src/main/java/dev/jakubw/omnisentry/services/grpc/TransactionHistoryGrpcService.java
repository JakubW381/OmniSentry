package dev.jakubw.omnisentry.services.grpc;

import dev.jakubw.omnisentry.dto.TransactionDto;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.proto.AnalyticsDataServiceGrpc.AnalyticsDataServiceImplBase;
import dev.jakubw.omnisentry.proto.Transactions;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import dev.jakubw.omnisentry.services.internal.TransactionService;
import dev.jakubw.omnisentry.services.internal.UserService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@GrpcService
@Service
@RequiredArgsConstructor
public class TransactionHistoryGrpcService extends AnalyticsDataServiceImplBase {

    private final TransactionService transactionService;
    private final ConnectionRepository connectionRepository;

    @Override
    public void getHistory(Transactions.HistoryRequest request, StreamObserver<Transactions.TransactionList> responseObserver) {
        try {
            boolean isOwner = connectionRepository.existsBySaltEdgeConnectionIdAndUserSaltEdgeCustomerId(
                    request.getConnectionId(),
                    request.getCustomerId()
            );
            if (!isOwner) {
                log.warn("Unauthorized access attempt: User {} tried to access connection {}",
                        request.getCustomerId(), request.getConnectionId());

                responseObserver.onError(Status.PERMISSION_DENIED
                        .withDescription("User is not associated with this connection")
                        .asRuntimeException());
                return;
            }

            int months = request.getMonthsLimit() > 0 ? request.getMonthsLimit() : 12;
            LocalDate after = LocalDate.now().minusMonths(months);

            List<Transactions.TransactionDto> historyProto = transactionService
                    .getTransactionsAfter(request.getConnectionId(), after)
                    .stream()
                    .map(this::mapToProto)
                    .toList();

            Transactions.TransactionList response = Transactions.TransactionList.newBuilder()
                    .addAllTransactions(historyProto)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error during getHistory: ", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private Transactions.TransactionDto mapToProto(TransactionDto dto) {
        dto.getAmount();
        return Transactions.TransactionDto.newBuilder()
                .setTransactionId(dto.getTransactionId())
                .setAccountId(dto.getAccountId())
                .setAmount(dto.getAmount().doubleValue())
                .setCurrency(dto.getCurrency())
                .setDescription(dto.getDescription())
                .setCategory(Objects.requireNonNullElse(dto.getCategory(), "other"))
                .setMadeOn(dto.getMadeOn())
                .setStatus(dto.getStatus())
                .build();
    }
}