package dev.jakubw.omnisentry.controllers;

import dev.jakubw.omnisentry.dto.AccountDto;
import dev.jakubw.omnisentry.dto.ConnectionDto;
import dev.jakubw.omnisentry.dto.TransactionDto;
import dev.jakubw.omnisentry.dto.UserDto;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.services.internal.AccountService;
import dev.jakubw.omnisentry.services.internal.ConnectionService;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import dev.jakubw.omnisentry.services.internal.TransactionService;
import dev.jakubw.omnisentry.services.internal.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final ConnectionService connectionService;
    private final SaltEdgeService saltEdgeService;
    private final AccountService accountService;


    /**
     *  USER
     */
    @GetMapping()
    public ResponseEntity<UserDto> getUser(@RequestHeader("X-User-Username") String username) {
        return ResponseEntity.ok(userService.getUserDto(username));
    }
    /**
     *  TRANSACTIONS
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDto>> getTransactions(
            @RequestParam("connection_id") String connectionId
    ) {
        List<TransactionDto> dtos = transactionService.getTransactions(connectionId);
        return ResponseEntity.ok(dtos);
    }


    /**
     *  CONNECTIONS
     */

    // Register User Connection to the Users Bank Via saltEdge
    // Generates an connectionMenuURL that answer is sent to SaltEdgeWebhook
    @PostMapping("/connection/register")
    public ResponseEntity<?> registerConnection(
            @RequestHeader("X-User-Username") String username
    ) {
        UserEntity user = userService.getByUsername(username);
        String sessionUrl = saltEdgeService.createConnectSession(user.getCustomerId(),"http://localhost").block();
        return ResponseEntity.ok().body(sessionUrl);
    }


    @GetMapping("/connections")
    public ResponseEntity<List<ConnectionDto>> getConnections(
            @RequestHeader("X-User-Username") String username
    ) {
        UserEntity user = userService.getByUsername(username);
        List<ConnectionDto> dtos = connectionService.getConnections(user.getCustomerId());
        return ResponseEntity.ok(dtos);
    }

    /**
     *  ACCOUNTS
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDto>> getAccounts(
            @RequestParam("connection_id") String connectionId
    ) {
        return ResponseEntity.ok(accountService.getAccounts(connectionId));
    }
}
