package dev.jakubw.omnisentry.controllers;

import org.apache.catalina.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @PostMapping("/register")
    public ResponseEntity<?> registerUser() {
        return ResponseEntity.ok().build();
    }


    @PostMapping("/connection/register")
    public ResponseEntity<?> registerConnection() {
        return ResponseEntity.ok().build();
    }
}
