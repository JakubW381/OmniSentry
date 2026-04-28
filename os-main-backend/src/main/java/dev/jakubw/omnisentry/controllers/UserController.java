package dev.jakubw.omnisentry.controllers;

import dev.jakubw.omnisentry.dto.TransactionDto;
import dev.jakubw.omnisentry.services.ConnectionService;
import dev.jakubw.omnisentry.services.TransactionService;
import dev.jakubw.omnisentry.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final ConnectionService connectionService;

    @PostMapping("/connection/register")
    public ResponseEntity<?> registerConnection(@RequestHeader("X-User-Username") String username) {
        log.info("Registering connection for user: {}", username);
        return ResponseEntity.ok().build();
    }

//    @GetMapping("/transactions")
//    public ResponseEntity<Flux<TransactionDto>> getTransactions(/*Principals from gateway/) {
//
//
//
//
//    }
}
