package dev.jakubw.omnisentry.repository;


import dev.jakubw.omnisentry.models.AccountEntity;
import dev.jakubw.omnisentry.repos.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;



public class AccountRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;


    @Test
    @DisplayName("Test findAllByConnectionId")
    public void testFindAllByConnectionId() {
        List<AccountEntity> mockEntities = List.of(AccountEntity.builder().saltEdgeAccountId(UUID.randomUUID().toString()).connectionId("123").build(),
                AccountEntity.builder().saltEdgeAccountId(UUID.randomUUID().toString()).connectionId("123").build(),
                AccountEntity.builder().saltEdgeAccountId(UUID.randomUUID().toString()).connectionId("123").build());
        accountRepository.saveAll(mockEntities);

        assertEquals(3, accountRepository.findAllByConnectionId("123").size());
        assertEquals(accountRepository.findAllByConnectionId("123"), mockEntities);
    }

    @Test
    @DisplayName("Test findBySaltEdgeAccountId")
    public void testFindBySaltEdgeAccountId() {
        String accountId = UUID.randomUUID().toString();
        List<AccountEntity> mockEntities = List.of(AccountEntity.builder().saltEdgeAccountId(accountId).build(),
                AccountEntity.builder().saltEdgeAccountId(UUID.randomUUID().toString()).build(),
                AccountEntity.builder().saltEdgeAccountId(UUID.randomUUID().toString()).build(),
                AccountEntity.builder().saltEdgeAccountId(UUID.randomUUID().toString()).build(),
                AccountEntity.builder().saltEdgeAccountId(UUID.randomUUID().toString()).build());
        accountRepository.saveAll(mockEntities);

        assertTrue(accountRepository.findBySaltEdgeAccountId(accountId).isPresent());
        assertEquals(accountRepository.findBySaltEdgeAccountId(accountId).get().getSaltEdgeAccountId(), accountId);
        assertEquals(accountRepository.findBySaltEdgeAccountId(accountId).get(), mockEntities.getFirst());
    }
}
