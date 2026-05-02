package dev.jakubw.omnisentry.services.internal;

import dev.jakubw.omnisentry.dto.UserDto;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserEntity getByUsername(String username){
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Username not found"));
    }
    public UserEntity getUserByCustomerId(String customerId){
        return userRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer Id not found"));
    }
    public UserDto getUserDto(String username){
        return mapToDto(getByUsername(username));
    }

    private UserDto mapToDto(UserEntity entity) {
        return new UserDto(
                entity.getUsername(),
                entity.getName(),
                entity.getSurname(),
                entity.getDateOfBirth(),
                entity.getEmail()
        );
    }
}