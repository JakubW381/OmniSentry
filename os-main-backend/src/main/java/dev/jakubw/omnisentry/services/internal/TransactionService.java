package dev.jakubw.omnisentry.services.internal;

import dev.jakubw.omnisentry.dto.TransactionDto;
import dev.jakubw.omnisentry.models.TransactionEntity;
import dev.jakubw.omnisentry.repos.TransactionRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/** TODO
 *  IT is a must to add pagination to such things
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final SaltEdgeService saltEdgeService;
    private final TransactionRepository transactionRepository;

    public Flux<TransactionDto> getTransactions(String connectionId) {
        Optional<TransactionEntity> lastTxOpt = transactionRepository.findFirstBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId);

        Flux<TransactionDto> newTransactionsFlux = lastTxOpt
                .map(tx -> saltEdgeService.getTransactions(connectionId, Optional.of(tx.getSaltEdgeTransactionId())))
                .orElseGet(() -> saltEdgeService.getTransactions(connectionId));

        return newTransactionsFlux
                .collectList()
                .flatMapMany(newDtos -> {
                    if (!newDtos.isEmpty()) {
                        List<TransactionEntity> newEntities = newDtos.stream()
                                .map(dto -> mapToEntity(dto,connectionId))
                                .toList();
                        transactionRepository.saveAll(newEntities);
                    }
                    List<TransactionDto> allTransactions = transactionRepository.findAllBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId)
                            .stream().map(this::mapToDto).toList();
                    return Flux.fromIterable(allTransactions);
                });
    }

    private TransactionEntity mapToEntity(TransactionDto dto, String connectionId) {
        return TransactionEntity.builder()
                .saltEdgeTransactionId(dto.getTransasctionId())
                .saltEdgeAccountId(dto.getAccountId())
                .saltEdgeConnectionId(connectionId)
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .description(dto.getDescription())
                .category(dto.getCategory() != null ? dto.getCategory() : "uncategorized")
                .madeOn(LocalDate.parse(dto.getMadeOn()))
                .status(dto.getStatus())
                .extra(dto.getExtra())
                .isSuspicious(false)
                .build();
    }
    private TransactionDto mapToDto(TransactionEntity entity) {
        return new TransactionDto(
                entity.getInternalId(),
                entity.getSaltEdgeTransactionId(),
                entity.getSaltEdgeAccountId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getMadeOn().toString(),
                entity.getStatus(),
                entity.getExtra() != null ? entity.getExtra() : Map.of()
        );
    }
}
