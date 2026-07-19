package dev.jakubw.omnisentry.services.internal;

import dev.jakubw.omnisentry.dto.TransactionDto;
import dev.jakubw.omnisentry.models.TransactionEntity;
import dev.jakubw.omnisentry.repos.TransactionRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


/** TODO
 *  IT is a must to add pagination to such things
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final SaltEdgeService saltEdgeService;
    private final TransactionRepository transactionRepository;


    public List<TransactionDto> getTransactions(String connectionId) {
        syncWithSaltEdge(connectionId);

        return transactionRepository.findAllBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId)
                .stream()
                .map(this::mapToDto)
                .toList();

    }

    public List<TransactionDto> getTransactionsAfter(String connectionId, LocalDate date) {
        syncWithSaltEdge(connectionId);

        return transactionRepository.findAllBySaltEdgeConnectionIdAndMadeOnAfterOrderByMadeOnDesc(connectionId, date)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private void syncWithSaltEdge(String connectionId) {
        Optional<TransactionEntity> lastTxOpt =
                transactionRepository.findFirstBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId);

        List<TransactionDto> newDtos = lastTxOpt
                .map(tx -> saltEdgeService.getTransactions(
                        connectionId,
                        tx.getSaltEdgeTransactionId()
                ))
                .orElseGet(() -> saltEdgeService.getTransactions(connectionId))
                .collectList()
                .block();

        if (newDtos == null || newDtos.isEmpty()) {
            return;
        }

        Set<String> existingIds =
                transactionRepository.findExistingIdsBySaltEdgeTransactionIdIn(
                        newDtos.stream()
                                .map(TransactionDto::getTransactionId)
                                .toList()
                );

        List<TransactionEntity> newEntities = newDtos.stream()
                .filter(dto -> !existingIds.contains(dto.getTransactionId()))
                .map(dto -> mapToEntity(dto, connectionId))
                .toList();

        if (!newEntities.isEmpty()) {
            log.info(
                    "Saving {} actually new transactions for connection {}",
                    newEntities.size(),
                    connectionId
            );

            transactionRepository.saveAll(newEntities);
        }
    }

    private TransactionEntity mapToEntity(TransactionDto dto, String connectionId) {
        return TransactionEntity.builder()
                .saltEdgeTransactionId(dto.getTransactionId())
                .saltEdgeAccountId(dto.getAccountId())
                .saltEdgeConnectionId(connectionId)
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .description(dto.getDescription())
                .category(dto.getCategory() != null && !dto.getCategory().isBlank() ? dto.getCategory() : "other")
                .madeOn(LocalDate.parse(dto.getMadeOn()))
                .status(dto.getStatus())
                .extra(dto.getExtra())
                .isSuspicious(false)
                .build();
    }

    private TransactionDto mapToDto(TransactionEntity entity) {
        return new TransactionDto(
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