package dev.jakubw.omnisentry.repository;

import dev.jakubw.omnisentry.models.AccountEntity;
import dev.jakubw.omnisentry.models.ConnectionEntity;
import dev.jakubw.omnisentry.models.TransactionEntity;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.AccountRepository;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import dev.jakubw.omnisentry.repos.TransactionRepository;
import dev.jakubw.omnisentry.repos.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class TransactionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private UserRepository userRepository;

    private AccountEntity account123;
    private AccountEntity account321;
    private AccountEntity account456;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        connectionRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Tworzymy użytkownika
        UserEntity user = UserEntity.builder()
                .username("john_doe")
                .email("john@example.com")
                .name("John")
                .surname("Doe")
                .saltEdgeCustomerId("cust_100")
                .build();

        // 2. Tworzymy Połączenia
        ConnectionEntity conn123 = ConnectionEntity.builder()
                .saltEdgeConnectionId("123")
                .providerName("Test Bank 123")
                .build();

        ConnectionEntity conn321 = ConnectionEntity.builder()
                .saltEdgeConnectionId("321")
                .providerName("Test Bank 321")
                .build();

        ConnectionEntity conn456 = ConnectionEntity.builder()
                .saltEdgeConnectionId("456")
                .providerName("Test Bank 456")
                .build();

        // Używamy metody pomocniczej addConnection, aby powiązać usera z połączeniami
        user.addConnection(conn123);
        user.addConnection(conn321);
        user.addConnection(conn456);

        userRepository.save(user);

        // 3. Tworzymy Konta
        account123 = AccountEntity.builder()
                .saltEdgeAccountId("acc_123")
                .name("Savings 123")
                .currency("EUR")
                .build();

        account321 = AccountEntity.builder()
                .saltEdgeAccountId("acc_321")
                .name("Savings 321")
                .currency("EUR")
                .build();

        account456 = AccountEntity.builder()
                .saltEdgeAccountId("acc_456")
                .name("Savings 456")
                .currency("EUR")
                .build();

        // Używamy metody pomocniczej addAccount, aby powiązać połączenia z kontami
        conn123.addAccount(account123);
        conn321.addAccount(account321);
        conn456.addAccount(account456);

        connectionRepository.saveAll(List.of(conn123, conn321, conn456));
    }

    private List<TransactionEntity> buildInitialEntities() {
        List<TransactionEntity> list = new ArrayList<>();

        addTransactionToAccount(account123, list, LocalDate.now().plusDays(1));
        addTransactionToAccount(account123, list, LocalDate.now());

        addTransactionToAccount(account321, list, LocalDate.now());
        addTransactionToAccount(account321, list, LocalDate.now().plusDays(2));
        addTransactionToAccount(account321, list, LocalDate.now().plusDays(3));

        addTransactionToAccount(account456, list, LocalDate.now().plusDays(3));
        addTransactionToAccount(account456, list, LocalDate.now().plusDays(3));

        return list;
    }

    private void addTransactionToAccount(AccountEntity account, List<TransactionEntity> list, LocalDate date) {
        TransactionEntity tx = TransactionEntity.builder()
                .status("OK")
                .amount(BigDecimal.valueOf(12.0))
                .currency("EUR")
                .saltEdgeTransactionId(UUID.randomUUID().toString())
                .madeOn(date)
                .build();

        // Użycie helpera przypisującego transakcję do konta
        account.addTransaction(tx);
        list.add(tx);
    }

    /**
     * Should return the most recent transaction for the desired connectionId
     */
    @Test
    @DisplayName("Test find First By SaltEdgeConnectionId Order By MadeOn Desc")
    public void findFirstBySaltEdgeConnectionIdOrderByMadeOnDescTest() {
        // Given
        List<TransactionEntity> entities = buildInitialEntities();

        TransactionEntity target = TransactionEntity.builder()
                .status("OK")
                .amount(BigDecimal.valueOf(12.0))
                .currency("EUR")
                .saltEdgeTransactionId("333888666")
                .madeOn(LocalDate.now().plusDays(2))
                .build();

        account123.addTransaction(target);
        entities.add(target);

        transactionRepository.saveAll(entities);

        // When
        Optional<TransactionEntity> result = transactionRepository
                .findFirstByAccountConnectionSaltEdgeConnectionIdOrderByMadeOnDesc("123");

        // Then
        assertTrue(result.isPresent());
        assertEquals(target.getSaltEdgeTransactionId(), result.get().getSaltEdgeTransactionId());
    }

    /**
     * Should return all transactions for the desired connectionId descending by madeOn
     */
    @Test
    @DisplayName("Test find All By SaltEdgeConnectionId Order By MadeOn Desc")
    public void findAllBySaltEdgeConnectionIdOrderByMadeOnDescTest() {
        // Given
        transactionRepository.saveAll(buildInitialEntities());
        String targetConnectionId = "321";
        Pageable pageable = PageRequest.of(0, 20, Sort.by("madeOn").descending());

        // When
        List<TransactionEntity> transactions = transactionRepository
                .findAllByAccountConnectionSaltEdgeConnectionId(targetConnectionId, pageable)
                .getContent();

        // Then
        assertThat(transactions).hasSize(3);
        LocalDate previousDate = LocalDate.MAX;

        for (TransactionEntity entity : transactions) {
            assertEquals(targetConnectionId, entity.getAccount().getConnection().getSaltEdgeConnectionId());
            // Upewniamy się, że daty idą malejąco (nie są późniejsze niż poprzednia)
            assertFalse(entity.getMadeOn().isAfter(previousDate));
            previousDate = entity.getMadeOn();
        }
    }

    /**
     * Should return all transactions for the desired connectionId descending by madeOn after the given date
     */
    @Test
    @DisplayName("Test find All By SaltEdgeConnectionId And MadeOn After Order By MadeOn Desc")
    public void findAllBySaltEdgeConnectionIdAndMadeOnAfterOrderByMadeOnDescTest() {
        // Given
        transactionRepository.saveAll(buildInitialEntities());
        LocalDate after = LocalDate.now();

        // When
        List<TransactionEntity> response = transactionRepository
                .findAllByAccountConnectionSaltEdgeConnectionIdAndMadeOnAfterOrderByMadeOnDesc("321", after);

        // Then
        assertThat(response).isNotEmpty();
        LocalDate previousDate = LocalDate.MAX;

        for (TransactionEntity r : response) {
            // Każdą transakcja musi być stricte PO dacie 'after'
            assertTrue(r.getMadeOn().isAfter(after));
            // Asercja sortowania malejącego: kolejna data musi być <= poprzednia data
            assertFalse(r.getMadeOn().isAfter(previousDate));
            previousDate = r.getMadeOn();
        }
    }

    /**
     * Should return all saltEdgeTransactionId that are contained in the given list
     */
    @Test
    public void findExistingIdsBySaltEdgeTransactionIdIn() {
        // Given
        List<TransactionEntity> savedEntities = transactionRepository.saveAll(buildInitialEntities());

        String txId1 = savedEntities.get(0).getSaltEdgeTransactionId();
        String txId2 = savedEntities.get(2).getSaltEdgeTransactionId();
        List<String> searchIds = List.of("555", txId1, txId2);

        // When
        Set<String> existing = transactionRepository.findExistingIdsBySaltEdgeTransactionIdIn(searchIds);

        // Then
        assertThat(existing).hasSize(2);
        assertThat(existing).containsExactlyInAnyOrder(txId1, txId2);
        assertThat(existing).doesNotContain("555");
    }
}