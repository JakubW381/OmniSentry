package dev.jakubw.omnisentry.repository;


import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryTest extends BaseRepositoryTest{

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Test find By Username")
    public void findByUsernameTest(){
        // Given
        List<UserEntity> testUsers = List.of(
                UserEntity.builder().saltEdgeCustomerId("test").username("test").build(),
                UserEntity.builder().saltEdgeCustomerId("321").username("321").build(),
                UserEntity.builder().saltEdgeCustomerId("333").username("333").build()
        );
        userRepository.saveAll(testUsers);

        // When
        Optional<UserEntity> user = userRepository.findByUsername("test");

        // Then
        assertTrue(user.isPresent());
        assertEquals("test",user.get().getUsername());
    }
    @Test
    @DisplayName("Test find By CustomerId")
    public void findByCustomerIdTest(){
        // Given
        List<UserEntity> testUsers = List.of(
                UserEntity.builder().saltEdgeCustomerId("123").build(),
                UserEntity.builder().saltEdgeCustomerId("321").build(),
                UserEntity.builder().saltEdgeCustomerId("333").build()
                );
        userRepository.saveAll(testUsers);

        // When
        Optional<UserEntity> user = userRepository.findBySaltEdgeCustomerId("333");

        // Then
        assertTrue(user.isPresent());
        assertEquals("333",user.get().getSaltEdgeCustomerId());
    }
    @Test
    @DisplayName("Test find By Email")
    public void findByEmailTest(){
        // Given
        List<UserEntity> testUsers = List.of(
                UserEntity.builder().saltEdgeCustomerId("123").email("test@123.com").build(),
                UserEntity.builder().saltEdgeCustomerId("321").email("test@321.com").build(),
                UserEntity.builder().saltEdgeCustomerId("333").email("test@333.com").build()
        );
        userRepository.saveAll(testUsers);

        // When
        Optional<UserEntity> user = userRepository.findByEmail("test@321.com");

        // Then
        assertTrue(user.isPresent());
        assertEquals("test@321.com",user.get().getEmail());
    }
    @Test
    @DisplayName("Test exists By Email Or Username")
    public void existsByEmailOrUsernameTest(){
        // Given
        List<UserEntity> testUsers = List.of(
                UserEntity.builder().saltEdgeCustomerId("123").username("123").email("test@123.com").build(),
                UserEntity.builder().saltEdgeCustomerId("321").username("321").email("test@321.com").build(),
                UserEntity.builder().saltEdgeCustomerId("333").username("333").email("test@333.com").build()
        );
        userRepository.saveAll(testUsers);

        // When & Then
        assertTrue(userRepository.existsByEmailOrUsername("test@666.com" , "123")); // by Username True
        assertTrue(userRepository.existsByEmailOrUsername("test@123.com" , "666")); // by Email True
        assertFalse(userRepository.existsByEmailOrUsername("test@666.com" , "666")); // doesn't exist
    }
}
