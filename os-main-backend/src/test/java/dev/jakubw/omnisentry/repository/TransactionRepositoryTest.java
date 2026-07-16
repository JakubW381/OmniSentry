package dev.jakubw.omnisentry.repository;

import dev.jakubw.omnisentry.models.TransactionEntity;
import dev.jakubw.omnisentry.repos.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;



public class TransactionRepositoryTest extends BaseRepositoryTest{

    @Autowired
    private TransactionRepository transactionRepository;

    private final List<TransactionEntity> entities = new ArrayList<>(List.of(
            TransactionEntity.builder().status("OK").amount(BigDecimal.valueOf(12.0)).currency("EUR").saltEdgeAccountId(UUID.randomUUID().toString()).saltEdgeTransactionId(UUID.randomUUID().toString()).saltEdgeConnectionId("123").madeOn(LocalDate.now().plusDays(1)).build(),
            TransactionEntity.builder().status("OK").amount(BigDecimal.valueOf(12.0)).currency("EUR").saltEdgeAccountId(UUID.randomUUID().toString()).saltEdgeTransactionId(UUID.randomUUID().toString()).saltEdgeConnectionId("123").madeOn(LocalDate.now()).build(),
            TransactionEntity.builder().status("OK").amount(BigDecimal.valueOf(12.0)).currency("EUR").saltEdgeAccountId(UUID.randomUUID().toString()).saltEdgeTransactionId(UUID.randomUUID().toString()).saltEdgeConnectionId("321").madeOn(LocalDate.now()).build(),
            TransactionEntity.builder().status("OK").amount(BigDecimal.valueOf(12.0)).currency("EUR").saltEdgeAccountId(UUID.randomUUID().toString()).saltEdgeTransactionId(UUID.randomUUID().toString()).saltEdgeConnectionId("321").madeOn(LocalDate.now().plusDays(2)).build(),
            TransactionEntity.builder().status("OK").amount(BigDecimal.valueOf(12.0)).currency("EUR").saltEdgeAccountId(UUID.randomUUID().toString()).saltEdgeTransactionId(UUID.randomUUID().toString()).saltEdgeConnectionId("321").madeOn(LocalDate.now().plusDays(3)).build(),
            TransactionEntity.builder().status("OK").amount(BigDecimal.valueOf(12.0)).currency("EUR").saltEdgeAccountId(UUID.randomUUID().toString()).saltEdgeTransactionId(UUID.randomUUID().toString()).saltEdgeConnectionId("456").madeOn(LocalDate.now().plusDays(3)).build(),
            TransactionEntity.builder().status("OK").amount(BigDecimal.valueOf(12.0)).currency("EUR").saltEdgeAccountId(UUID.randomUUID().toString()).saltEdgeTransactionId(UUID.randomUUID().toString()).saltEdgeConnectionId("456").madeOn(LocalDate.now().plusDays(3)).build()
    ));

    /**
     * Should return the most recent transaction for the desired connectionId
     */
    @Test
    @DisplayName("Test find First By SaltEdgeConnectionId Order By MadeOn Desc")
    public void findFirstBySaltEdgeConnectionIdOrderByMadeOnDescTest() {
        // Given
        TransactionEntity target = TransactionEntity.builder().status("OK").amount(BigDecimal.valueOf(12.0)).currency("EUR").saltEdgeAccountId(UUID.randomUUID().toString()).saltEdgeTransactionId(UUID.randomUUID().toString()).saltEdgeConnectionId("123").saltEdgeTransactionId("333888666").madeOn(LocalDate.now().plusDays(2)).build(); // this one
        entities.add(target);

        // When
        transactionRepository.saveAll(entities);

        // Then
        assertTrue(transactionRepository.findFirstBySaltEdgeConnectionIdOrderByMadeOnDesc("123").isPresent());
        assertEquals(target,transactionRepository.findFirstBySaltEdgeConnectionIdOrderByMadeOnDesc("123").get());
        entities.remove(target);
    }


    /**
     * Should return all transactions for the desired connectionId descending by madeOn
     */
    @Test
    @DisplayName("Test find All By SaltEdgeConnectionId Order By MadeOn Desc")
    public void findAllBySaltEdgeConnectionIdOrderByMadeOnDescTest() {
        // Given
        transactionRepository.saveAll(entities);
        String targetId = "321";
        LocalDate max = LocalDate.MAX;

        // When
        List<TransactionEntity> transactions = transactionRepository
                .findAllBySaltEdgeConnectionIdOrderByMadeOnDesc(targetId);

        // Then
        for (TransactionEntity entity : transactions) {
            assertEquals(targetId,entity.getSaltEdgeConnectionId());
            assertTrue(entity.getMadeOn().isBefore(max));
            max = entity.getMadeOn();
        }
    }

    /**
     * Should return all transactions for the desired connectionId descending by madeOn after the given date
     */
    @Test
    @DisplayName("Test find All By SaltEdgeConnectionId And MadeOn After Order By MadeOn Desc")
    public void findAllBySaltEdgeConnectionIdAndMadeOnAfterOrderByMadeOnDescTest() {
        // Given
        transactionRepository.saveAll(entities);
        LocalDate after = LocalDate.now();
        LocalDate max = LocalDate.MAX;

        // When
        List<TransactionEntity> response = transactionRepository.findAllBySaltEdgeConnectionIdAndMadeOnAfterOrderByMadeOnDesc("321", after);

        // Then
        for(TransactionEntity r : response){
            assertTrue(r.getMadeOn().isAfter(after));
            assertTrue(r.getMadeOn().isBefore(max));
            max = r.getMadeOn();
        }
    }

    /**
     * Should return all saltEdgeTransactionId that are contained in the given list
     */
    @Test
    public void findExistingIdsBySaltEdgeTransactionIdIn() {
        // Given
        List<String> ids = List.of("555", "321", "456");
        transactionRepository.saveAll(entities);

        // When
        Set<String> existing = transactionRepository.findExistingIdsBySaltEdgeTransactionIdIn(ids);

        // Then
        for(String id : existing){
            Optional<TransactionEntity> en = transactionRepository.findById(id);
            assertTrue(en.isPresent());
            assertTrue(ids.contains(en.get().getSaltEdgeTransactionId()));
        }
    }

    /**
     *  Should delete all transactions for the given connectionId
     */
    @Test
    @DisplayName("Test delete All By SaltEdgeConnectionId")
    public void deleteAllBySaltEdgeConnectionId() {
        // Given
        transactionRepository.saveAll(entities);
        String target = "123";

        // When
        transactionRepository.deleteAllBySaltEdgeConnectionId(target);
        List<TransactionEntity> transactions = transactionRepository.findAll();

        // Then
        assertTrue(transactions.stream().noneMatch(t -> t.getSaltEdgeConnectionId().equals(target)));
    }
}
