package dev.jakubw.omnisentry.service.internal;

import dev.jakubw.omnisentry.dto.UserDto;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.UserRepository;
import dev.jakubw.omnisentry.services.internal.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Test get user by Username")
    void shouldReturnUserByUsername() {
        // Given
        String username = "John_Doe";
        UserEntity mockUser = UserEntity.builder()
                .saltEdgeCustomerId("123")
                .username(username)
                .name("John")
                .surname("Doe")
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        // When
        UserEntity result = userService.getByUsername(username);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException, when username doesn't exist")
    void shouldThrowExceptionWhenUsernameNotFound() {
        // Given
        String username = "doesn't_exist";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getByUsername(username))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Username not found");
    }

    @Test
    @DisplayName("Should get customerId and map them to Dto")
    void shouldReturnCorrectUserDto() {
        // Given
        String customerId = "123";
        UserEntity mockUser = UserEntity.builder()
                .saltEdgeCustomerId(customerId)
                .username("john99")
                .name("John")
                .surname("Doe")
                .email("john@doe.com")
                .dateOfBirth(Instant.ofEpochMilli(LocalDate.of(1999, 5, 12).toEpochDay() * 1000))
                .build();

        when(userRepository.findBySaltEdgeCustomerId(customerId)).thenReturn(Optional.of(mockUser));

        // When
        UserDto result = userService.getUserDto(customerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("john99");
        assertThat(result.getName()).isEqualTo("John");
        assertThat(result.getEmail()).isEqualTo("john@doe.com");
    }
}
