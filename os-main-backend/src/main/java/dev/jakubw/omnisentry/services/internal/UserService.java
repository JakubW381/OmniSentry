package dev.jakubw.omnisentry.services.internal;

import dev.jakubw.omnisentry.dto.UserDto;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserEntity getByUsername(String username){
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Username not found"));
    }
    public UserEntity getUserByCustomerId(String customerId){
        return userRepository.findBySaltEdgeCustomerId(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer Id not found"));
    }
    public UserDto getUserDto(String customerId){
        return mapToDto(getUserByCustomerId(customerId));
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