package dev.jakubw.omnisentry.repos;

import dev.jakubw.omnisentry.models.AccountEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, String> {
    @EntityGraph(attributePaths = {"connection"})
    List<AccountEntity> findAllByConnectionSaltEdgeConnectionId(String saltEdgeConnectionId);
}
