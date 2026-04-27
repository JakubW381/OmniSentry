package dev.jakubw.omnisentry.services;

import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.attribute.UserPrincipalNotFoundException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public   getUserByUsername(String username){
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow( () ->  new UserPrincipalNotFoundException("Username not found"));


    }
    public getUserByEmail(String email){

    }


}
