package dev.jakubw.omnisentry.repos;

import dev.jakubw.omnisentry.models.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
    Optional<TransactionEntity> findFirstBySaltEdgeConnectionIdOrderByMadeOnDesc(String saltEdgeConnectionId);

    List<TransactionEntity> findAllBySaltEdgeConnectionIdOrderByMadeOnDesc(String saltEdgeConnectionId);
    List<TransactionEntity> findAllBySaltEdgeConnectionIdAndMadeOnAfterOrderByMadeOnDesc(
            String saltEdgeConnectionId,
            LocalDate date
    );
    @Query("SELECT t.saltEdgeTransactionId FROM TransactionEntity t WHERE t.saltEdgeTransactionId IN :ids")
    Set<String> findExistingIdsBySaltEdgeTransactionIdIn(@Param("ids") List<String> ids);
    void deleteAllBySaltEdgeConnectionId(String saltEdgeAccountId);
}
