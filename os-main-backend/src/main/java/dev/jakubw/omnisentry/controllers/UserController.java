package dev.jakubw.omnisentry.controllers;

import dev.jakubw.omnisentry.dto.AccountDto;
import dev.jakubw.omnisentry.dto.ConnectionDto;
import dev.jakubw.omnisentry.dto.TransactionDto;
import dev.jakubw.omnisentry.dto.UserDto;
import dev.jakubw.omnisentry.services.internal.AccountService;
import dev.jakubw.omnisentry.services.internal.ConnectionService;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import dev.jakubw.omnisentry.services.internal.TransactionService;
import dev.jakubw.omnisentry.services.internal.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping
    public ResponseEntity<UserDto> getUser(@RequestHeader("X-User-CustomerId") String customerId) {
        return ResponseEntity.ok(userService.getUserDto(customerId));
    }
    /**
     *  TRANSACTIONS
     */
    @GetMapping(value = "/transactions", params = "connection_id")
    public ResponseEntity<List<TransactionDto>> getTransactionsByConnection(
            @RequestParam("connection_id") String connectionId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        List<TransactionDto> dtos = transactionService.getTransactionsByConnection(connectionId, page, size);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping(value = "/transactions", params = "account_id")
    public ResponseEntity<List<TransactionDto>> getTransactionsByAccount(
            @RequestParam("account_id") String accountId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        List<TransactionDto> dtos = transactionService.getTransactionsByAccount(accountId, page, size);
        return ResponseEntity.ok(dtos);
    }


    /**
     *  CONNECTIONS
     */

    // Register User Connection to the Users Bank Via saltEdge
    // Generates an connectionMenuURL that answer is sent to SaltEdgeWebhook
    @PostMapping("/connection/register")
    public ResponseEntity<?> registerConnection(
            @RequestHeader("X-User-CustomerId") String customerId
    ) {
        String sessionUrl = saltEdgeService.createConnectSession(customerId,"http://localhost");
        return ResponseEntity.ok().body(sessionUrl);
    }


    @GetMapping("/connections")
    public ResponseEntity<List<ConnectionDto>> getConnections(
            @RequestHeader("X-User-CustomerId") String customerId
    ) {
        List<ConnectionDto> dtos = connectionService.getConnections(customerId);
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
