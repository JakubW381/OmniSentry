package dev.jakubw.omnisentry.repository;


import dev.jakubw.omnisentry.models.AccountEntity;
import dev.jakubw.omnisentry.models.ConnectionEntity;
import dev.jakubw.omnisentry.repos.AccountRepository;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;



public class AccountRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Test
    @DisplayName("Test findAllByConnectionId")
    public void testFindAllByConnectionId() {
        // Given
        ConnectionEntity connection = ConnectionEntity.builder().saltEdgeConnectionId("123")
                .providerName("provider")
                .providerCode("123123")
                .status("Cool")
                .createdAt(Instant.now())
                .build();

        List<AccountEntity> mockEntities = List.of(AccountEntity.builder().saltEdgeAccountId(UUID.randomUUID().toString()).build(),
                AccountEntity.builder().saltEdgeAccountId(UUID.randomUUID().toString()).build(),
                AccountEntity.builder().saltEdgeAccountId(UUID.randomUUID().toString()).build());
        mockEntities.forEach(connection::addAccount);

        connectionRepository.save(connection);
        // When & Then
        assertEquals(3, accountRepository.findAllByConnectionSaltEdgeConnectionId("123").size());
    }
}
