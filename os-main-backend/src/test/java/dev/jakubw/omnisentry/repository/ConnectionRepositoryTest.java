package dev.jakubw.omnisentry.repository;

import dev.jakubw.omnisentry.models.ConnectionEntity;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import dev.jakubw.omnisentry.repos.UserRepository;
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

    @Autowired
    private UserRepository userRepository;

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
        connectionRepository.deleteById("123");

        // Then
        List<ConnectionEntity> list = connectionRepository.findAll();
        assertEquals(1,list.size());
        assertTrue(list.stream().noneMatch(c -> c.getSaltEdgeConnectionId().equals("123")));
    }

    @Test
    @DisplayName("Test find All By CustomerId")
    public void findAllByCustomerId(){
        // Given
        UserEntity user = UserEntity.builder().saltEdgeCustomerId("123").email("test@123.com").build();
        List<ConnectionEntity> entityList = List.of(
                ConnectionEntity.builder().saltEdgeConnectionId("124351").build(),
                ConnectionEntity.builder().saltEdgeConnectionId("45376").build(),
                ConnectionEntity.builder().saltEdgeConnectionId("7689456").build()
        );
        entityList.forEach(user::addConnection);

        userRepository.save(user);

        // When
        List<ConnectionEntity> list = connectionRepository.findAllByUserSaltEdgeCustomerId("123");

        // Then
        assertEquals(3,list.size());
        assertTrue(list.stream().allMatch(c -> c.getUser().getSaltEdgeCustomerId().equals("123")));
    }
}
