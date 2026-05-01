package dev.jakubw.omnisentry.repos;

import dev.jakubw.omnisentry.models.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    Optional<TransactionEntity> findFirstBySaltEdgeConnectionIdOrderByMadeOnDesc(String saltEdgeAccountId);
    List<TransactionEntity> findAllBySaltEdgeConnectionIdOrderByMadeOnDesc(String saltEdgeAccountId);
    void deleteAllByConnectionId(String saltEdgeAccountId);
}
