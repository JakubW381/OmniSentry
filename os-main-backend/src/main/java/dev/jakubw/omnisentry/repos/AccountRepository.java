package dev.jakubw.omnisentry.repos;

import dev.jakubw.omnisentry.models.AccountEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    List<AccountEntity> findAllByConnectionId(String connectionId);
    Optional<AccountEntity> findBySaltEdgeAccountId(String saltEdgeAccountId);
}
