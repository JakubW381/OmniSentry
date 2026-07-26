package dev.jakubw.omnisentry.service.internal;

import dev.jakubw.omnisentry.dto.TransactionDto;
import dev.jakubw.omnisentry.models.AccountEntity;
import dev.jakubw.omnisentry.models.ConnectionEntity;
import dev.jakubw.omnisentry.models.TransactionEntity;
import dev.jakubw.omnisentry.repos.AccountRepository;
import dev.jakubw.omnisentry.repos.TransactionRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import dev.jakubw.omnisentry.services.internal.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<List<TransactionEntity>> savedEntitiesCaptor;

    private AccountEntity mockAccount1;
    private AccountEntity mockAccount2;

    @BeforeEach
    void setUp() {
        ConnectionEntity mockConnection123 = ConnectionEntity.builder()
                .saltEdgeConnectionId("conn_123")
                .build();

        ConnectionEntity mockConnection999 = ConnectionEntity.builder()
                .saltEdgeConnectionId("conn_999")
                .build();

        mockAccount1 = AccountEntity.builder()
                .saltEdgeAccountId("acc_1")
                .connection(mockConnection123)
                .build();

        mockAccount2 = AccountEntity.builder()
                .saltEdgeAccountId("acc_2")
                .connection(mockConnection999)
                .build();
    }

    @Test
    @DisplayName("Should sync and save only new transactions")
    void shouldSyncAndSaveOnlyNewTransactions() {
        // Given
        String connectionId = "conn_123";
        String existingTxId = "tx_existing";
        String newTxId = "tx_new";

        TransactionEntity lastDbTx = TransactionEntity.builder()
                .saltEdgeTransactionId(existingTxId)
                .account(mockAccount1)
                .amount(BigDecimal.TEN)
                .currency("EUR")
                .description("Regular tx")
                .madeOn(LocalDate.now().minusDays(1))
                .status("OK")
                .build();

        when(transactionRepository.findFirstByAccountConnectionSaltEdgeConnectionIdOrderByMadeOnDesc(connectionId))
                .thenReturn(Optional.of(lastDbTx));

        TransactionDto mockDto1 = new TransactionDto(existingTxId, "acc_1", BigDecimal.TEN, "EUR", "Regular tx", "food", "2026-07-16", "OK", Map.of());
        TransactionDto mockDto2 = new TransactionDto(newTxId, "acc_1", BigDecimal.valueOf(25.0), "EUR", "New tx", "rent", "2026-07-17", "OK", Map.of());

        when(saltEdgeService.getTransactions(eq(connectionId), eq(existingTxId)))
                .thenReturn(List.of(mockDto1, mockDto2));

        when(transactionRepository.findExistingIdsBySaltEdgeTransactionIdIn(List.of(existingTxId, newTxId)))
                .thenReturn(Set.of(existingTxId));

        lenient().when(accountRepository.findById("acc_1"))
                .thenReturn(Optional.of(mockAccount1));

        TransactionEntity newDbEntity = TransactionEntity.builder()
                .saltEdgeTransactionId(newTxId)
                .account(mockAccount1)
                .amount(BigDecimal.valueOf(25.0))
                .currency("EUR")
                .description("New tx")
                .category("rent")
                .madeOn(LocalDate.parse("2026-07-17"))
                .status("OK")
                .build();

        // POPRAWKA: Zwracamy PageImpl
        when(transactionRepository.findAllByAccountConnectionSaltEdgeConnectionId(eq(connectionId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(newDbEntity, lastDbTx)));

        // When
        List<TransactionDto> result = transactionService.getTransactionsByConnection(connectionId, 1, 10);

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

        when(transactionRepository.findFirstByAccountConnectionSaltEdgeConnectionIdOrderByMadeOnDesc(connectionId))
                .thenReturn(Optional.empty());

        TransactionDto incomingDto = new TransactionDto(incomingTxId, "acc_2", BigDecimal.TEN, "EUR", "Cinema", "entertainment", "2026-07-17", "OK", Map.of());

        when(saltEdgeService.getTransactions(connectionId))
                .thenReturn(List.of(incomingDto));

        when(transactionRepository.findExistingIdsBySaltEdgeTransactionIdIn(List.of(incomingTxId)))
                .thenReturn(Collections.emptySet());

        lenient().when(accountRepository.findById("acc_2"))
                .thenReturn(Optional.of(mockAccount2));

        TransactionEntity savedEntity = TransactionEntity.builder()
                .saltEdgeTransactionId(incomingTxId)
                .account(mockAccount2)
                .amount(BigDecimal.TEN)
                .currency("EUR")
                .description("Cinema")
                .madeOn(LocalDate.now())
                .status("OK")
                .build();

        // POPRAWKA: Zwracamy PageImpl
        when(transactionRepository.findAllByAccountConnectionSaltEdgeConnectionId(eq(connectionId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(savedEntity)));

        // When
        transactionService.getTransactionsByConnection(connectionId, 0, 20);

        // Then
        verify(saltEdgeService, times(1)).getTransactions(connectionId);
        verify(transactionRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Should not save anything if all transactions already exist")
    void shouldNotSaveAnythingIfAllTransactionsAlreadyExist() {
        // Given
        String connectionId = "conn_123";
        String existingTxId = "tx_old";

        TransactionEntity lastDbTx = TransactionEntity.builder()
                .saltEdgeTransactionId(existingTxId)
                .account(mockAccount1)
                .amount(BigDecimal.TEN)
                .currency("EUR")
                .description("Description")
                .madeOn(LocalDate.now())
                .status("OK")
                .build();

        when(transactionRepository.findFirstByAccountConnectionSaltEdgeConnectionIdOrderByMadeOnDesc(connectionId))
                .thenReturn(Optional.of(lastDbTx));

        TransactionDto mockDto = new TransactionDto(existingTxId, "acc_1", BigDecimal.TEN, "EUR", "Description", "", "2026-07-17", "OK", null);

        when(saltEdgeService.getTransactions(eq(connectionId), eq(existingTxId)))
                .thenReturn(List.of(mockDto));

        when(transactionRepository.findExistingIdsBySaltEdgeTransactionIdIn(List.of(existingTxId)))
                .thenReturn(Set.of(existingTxId));

        // POPRAWKA: Zwracamy PageImpl
        when(transactionRepository.findAllByAccountConnectionSaltEdgeConnectionId(eq(connectionId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(lastDbTx)));

        // When
        transactionService.getTransactionsByConnection(connectionId, 0, 20);

        // Then
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should return transactions after given date")
    void shouldReturnTransactionsAfterGivenDate() {
        // Given
        String connectionId = "conn_123";
        LocalDate filterDate = LocalDate.now().minusDays(5);

        when(transactionRepository.findFirstByAccountConnectionSaltEdgeConnectionIdOrderByMadeOnDesc(connectionId))
                .thenReturn(Optional.empty());

        // ZMIANA: List.of() zamiast Flux.empty()
        when(saltEdgeService.getTransactions(connectionId))
                .thenReturn(List.of());

        TransactionEntity validTx = TransactionEntity.builder()
                .saltEdgeTransactionId("tx_valid")
                .account(mockAccount1)
                .amount(BigDecimal.TEN)
                .currency("EUR")
                .description("Valid transaction")
                .madeOn(LocalDate.now())
                .status("OK")
                .build();

        when(transactionRepository.findAllByAccountConnectionSaltEdgeConnectionIdAndMadeOnAfterOrderByMadeOnDesc(connectionId, filterDate))
                .thenReturn(List.of(validTx));

        // When
        List<TransactionDto> result = transactionService.getTransactionsAfter(connectionId, filterDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTransactionId()).isEqualTo("tx_valid");
        verify(transactionRepository, times(1)).findAllByAccountConnectionSaltEdgeConnectionIdAndMadeOnAfterOrderByMadeOnDesc(connectionId, filterDate);
    }
}