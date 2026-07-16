package dev.jakubw.omnisentry.repository;

import dev.jakubw.omnisentry.models.ConnectionEntity;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ConnectionRepositoryTest extends BaseRepositoryTest{

    @Autowired
    private ConnectionRepository connectionRepository;

    @Test
    @DisplayName("Test delete By SaltEdgeConnectionId")
    public void deleteBySaltEdgeConnectionId(){
        // Given
        List<ConnectionEntity> entityList = List.of(
                ConnectionEntity.builder().saltEdgeConnectionId("123").build(),
                ConnectionEntity.builder().saltEdgeConnectionId("321").build()
        );
        connectionRepository.saveAll(entityList);

        // When
        connectionRepository.deleteBySaltEdgeConnectionId("123");

        // Then
        List<ConnectionEntity> list = connectionRepository.findAll();
        assertEquals(1,list.size());
        assertTrue(list.stream().noneMatch(c -> c.getSaltEdgeConnectionId().equals("123")));
    }

    @Test
    @DisplayName("Test find All By CustomerId")
    public void findAllByCustomerId(){
        // Given
        List<ConnectionEntity> entityList = List.of(
                ConnectionEntity.builder().saltEdgeConnectionId(UUID.randomUUID().toString()).customerId("123").build(),
                ConnectionEntity.builder().saltEdgeConnectionId(UUID.randomUUID().toString()).customerId("123").build(),
                ConnectionEntity.builder().saltEdgeConnectionId(UUID.randomUUID().toString()).customerId("123").build(),
                ConnectionEntity.builder().saltEdgeConnectionId(UUID.randomUUID().toString()).customerId("321").build(),
                ConnectionEntity.builder().saltEdgeConnectionId(UUID.randomUUID().toString()).customerId("321").build(),
                ConnectionEntity.builder().saltEdgeConnectionId(UUID.randomUUID().toString()).customerId("321").build()
        );
        connectionRepository.saveAll(entityList);

        // When
        List<ConnectionEntity> list = connectionRepository.findAllByCustomerId("123");

        // Then
        assertEquals(3,list.size());
        assertTrue(list.stream().allMatch(c -> c.getCustomerId().equals("123")));
    }
}
