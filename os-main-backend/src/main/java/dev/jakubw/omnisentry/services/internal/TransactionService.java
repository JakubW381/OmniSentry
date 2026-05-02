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
import java.time.LocalDateTime;
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


    public List<TransactionDto> getTransactions(String connectionId) {
        syncWithSaltEdge(connectionId);

        return transactionRepository.findAllBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<TransactionDto> getTransactionsAfter(String connectionId, LocalDateTime date) {
        syncWithSaltEdge(connectionId);

        return transactionRepository.findAllBySaltEdgeConnectionIdAndMadeOnAfterOrderByMadeOnDesc(connectionId, date)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private void syncWithSaltEdge(String connectionId) {
        Optional<TransactionEntity> lastTxOpt = transactionRepository.findFirstBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId);

        List<TransactionDto> newDtos = lastTxOpt
                .map(tx -> saltEdgeService.getTransactions(connectionId, Optional.of(tx.getSaltEdgeTransactionId())))
                .orElseGet(() -> saltEdgeService.getTransactions(connectionId))
                .collectList()
                .block(); // Blokujemy, bo reszta serwisu jest synchroniczna

        if (newDtos != null && !newDtos.isEmpty()) {
            log.info("Saving {} new transactions for connection {}", newDtos.size(), connectionId);
            List<TransactionEntity> newEntities = newDtos.stream()
                    .map(dto -> mapToEntity(dto, connectionId))
                    .toList();
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