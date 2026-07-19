package dev.jakubw.omnisentry.service.internal;

import dev.jakubw.omnisentry.dto.TransactionDto;
import dev.jakubw.omnisentry.models.TransactionEntity;
import dev.jakubw.omnisentry.repos.TransactionRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import dev.jakubw.omnisentry.services.internal.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private SaltEdgeService saltEdgeService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<List<TransactionEntity>> savedEntitiesCaptor;

    @Test
    @DisplayName("Should sync and save only new transactions")
    void shouldSyncAndSaveOnlyNewTransactions() {
        // Given
        String connectionId = "conn_123";
        String existingTxId = "tx_existing";
        String newTxId = "tx_new";

        TransactionEntity lastDbTx = TransactionEntity.builder()
                .saltEdgeTransactionId(existingTxId)
                .saltEdgeAccountId("acc_1")
                .amount(BigDecimal.TEN)
                .currency("EUR")
                .description("Regular tx")
                .madeOn(LocalDate.now().minusDays(1))
                .status("OK")
                .build();

        when(transactionRepository.findFirstBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId))
                .thenReturn(Optional.of(lastDbTx));

        TransactionDto mockDto1 = new TransactionDto(existingTxId, "acc_1", BigDecimal.TEN, "EUR", "Regular tx", "food", "2026-07-16", "OK", Map.of());
        TransactionDto mockDto2 = new TransactionDto(newTxId, "acc_1", BigDecimal.valueOf(25.0), "EUR", "New tx", "rent", "2026-07-17", "OK", Map.of());

        when(saltEdgeService.getTransactions(eq(connectionId), eq(existingTxId)))
                .thenReturn(Flux.just(mockDto1, mockDto2));

        when(transactionRepository.findExistingIdsBySaltEdgeTransactionIdIn(List.of(existingTxId, newTxId)))
                .thenReturn(Set.of(existingTxId));

        TransactionEntity newDbEntity = TransactionEntity.builder()
                .saltEdgeTransactionId(newTxId)
                .saltEdgeAccountId("acc_1")
                .saltEdgeConnectionId(connectionId)
                .amount(BigDecimal.valueOf(25.0))
                .currency("EUR")
                .description("New tx")
                .madeOn(LocalDate.parse("2026-07-17"))
                .status("OK")
                .build();

        when(transactionRepository.findAllBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId))
                .thenReturn(List.of(newDbEntity, lastDbTx));

        // When
        List<TransactionDto> result = transactionService.getTransactions(connectionId);

        // Then
        verify(transactionRepository, times(1)).saveAll(savedEntitiesCaptor.capture());
        List<TransactionEntity> savedEntities = savedEntitiesCaptor.getValue();

        assertThat(savedEntities).hasSize(1);
        assertThat(savedEntities.getFirst().getSaltEdgeTransactionId()).isEqualTo(newTxId);
        assertThat(savedEntities.getFirst().getCategory()).isEqualTo("rent");

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getTransactionId()).isEqualTo(newTxId);
    }

    @Test
    @DisplayName("Should sync from scratch when no transactions in database")
    void shouldSyncFromScratchWhenNoTransactionsInDatabase() {
        // Given
        String connectionId = "conn_999";
        String incomingTxId = "tx_brand_new";

        when(transactionRepository.findFirstBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId))
                .thenReturn(Optional.empty());

        TransactionDto incomingDto = new TransactionDto(incomingTxId, "acc_2", BigDecimal.TEN, "EUR", "Cinema", "entertainment", "2026-07-17", "OK", Map.of());
        when(saltEdgeService.getTransactions(connectionId))
                .thenReturn(Flux.just(incomingDto));

        when(transactionRepository.findExistingIdsBySaltEdgeTransactionIdIn(List.of(incomingTxId)))
                .thenReturn(Collections.emptySet());

        when(transactionRepository.findAllBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId))
                .thenReturn(List.of(TransactionEntity.builder()
                        .saltEdgeTransactionId(incomingTxId)
                        .saltEdgeAccountId("acc_2")
                        .amount(BigDecimal.TEN)
                        .currency("EUR")
                        .description("Cinema")
                        .madeOn(LocalDate.now())
                        .status("OK")
                        .build()));

        // When
        transactionService.getTransactions(connectionId);

        // Then
        verify(saltEdgeService, times(1)).getTransactions(connectionId);
        verify(transactionRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Should not save anything if all transactions already exist")
    void shouldNotSaveAnythingIfAllTransactionsAlreadyExist() {
        // Given
        String connectionId = "conn_456";
        String existingTxId = "tx_old";

        TransactionEntity lastDbTx = TransactionEntity.builder()
                .saltEdgeTransactionId(existingTxId)
                .saltEdgeAccountId("acc_1")
                .amount(BigDecimal.TEN)
                .currency("EUR")
                .description("Description")
                .madeOn(LocalDate.now())
                .status("OK")
                .build();

        when(transactionRepository.findFirstBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId))
                .thenReturn(Optional.of(lastDbTx));

        TransactionDto mockDto = new TransactionDto(existingTxId, "acc_1", BigDecimal.TEN, "EUR", "Description", "", "2026-07-17", "OK", null);
        when(saltEdgeService.getTransactions(eq(connectionId), eq(existingTxId)))
                .thenReturn(Flux.just(mockDto));

        when(transactionRepository.findExistingIdsBySaltEdgeTransactionIdIn(List.of(existingTxId)))
                .thenReturn(Set.of(existingTxId));

        when(transactionRepository.findAllBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId))
                .thenReturn(List.of(lastDbTx));

        // When
        transactionService.getTransactions(connectionId);

        // Then
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should return transactions after given date")
    void shouldReturnTransactionsAfterGivenDate() {
        // Given
        String connectionId = "conn_123";
        LocalDate filterDate = LocalDate.now().minusDays(5);

        when(transactionRepository.findFirstBySaltEdgeConnectionIdOrderByMadeOnDesc(connectionId))
                .thenReturn(Optional.empty());
        when(saltEdgeService.getTransactions(connectionId))
                .thenReturn(Flux.empty());

        TransactionEntity validTx = TransactionEntity.builder()
                .saltEdgeTransactionId("tx_valid")
                .saltEdgeAccountId("acc_1")
                .amount(BigDecimal.TEN)
                .currency("EUR")
                .description("Valid transaction")
                .madeOn(LocalDate.now())
                .status("OK")
                .build();

        when(transactionRepository.findAllBySaltEdgeConnectionIdAndMadeOnAfterOrderByMadeOnDesc(connectionId, filterDate))
                .thenReturn(List.of(validTx));

        // When
        List<TransactionDto> result = transactionService.getTransactionsAfter(connectionId, filterDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTransactionId()).isEqualTo("tx_valid");
        verify(transactionRepository, times(1)).findAllBySaltEdgeConnectionIdAndMadeOnAfterOrderByMadeOnDesc(connectionId, filterDate);
    }
}