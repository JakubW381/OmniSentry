package dev.jakubw.omnisentry.services.internal;

import dev.jakubw.omnisentry.dto.TransactionDto;
import dev.jakubw.omnisentry.models.AccountEntity;
import dev.jakubw.omnisentry.models.TransactionEntity;
import dev.jakubw.omnisentry.repos.AccountRepository;
import dev.jakubw.omnisentry.repos.TransactionRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final SaltEdgeService saltEdgeService;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public List<TransactionDto> getTransactionsByConnection(String connectionId, int page, int size) {
        syncWithSaltEdge(connectionId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("madeOn").descending());
        return transactionRepository.findAllByAccountConnectionSaltEdgeConnectionId(connectionId, pageable)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public List<TransactionDto> getTransactionsByAccount(String accountId, int page, int size) {
        AccountEntity account = accountRepository.findById(accountId)
                        .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        syncWithSaltEdge(account.getConnection().getSaltEdgeConnectionId());

        Pageable pageable = PageRequest.of(page, size, Sort.by("madeOn").descending());
        return transactionRepository.findAllByAccountSaltEdgeAccountId(accountId, pageable)
                .stream()
                .map(this::mapToDto)
                .toList();
    }


    @Transactional
    public List<TransactionDto> getTransactionsAfter(String connectionId, LocalDate date) {
        syncWithSaltEdge(connectionId);

        return transactionRepository.findAllByAccountConnectionSaltEdgeConnectionIdAndMadeOnAfterOrderByMadeOnDesc(connectionId, date)
                .stream()
                .map(this::mapToDto)
                .toList();
    }


    private void syncWithSaltEdge(String connectionId) {
        Optional<TransactionEntity> lastTxOpt =
                transactionRepository.findFirstByAccountConnectionSaltEdgeConnectionIdOrderByMadeOnDesc(connectionId);

        List<TransactionDto> newDtos;
        if (lastTxOpt.isPresent()) {
            newDtos = saltEdgeService.getTransactions(connectionId, lastTxOpt.get().getSaltEdgeTransactionId());
        } else {
            newDtos = saltEdgeService.getTransactions(connectionId);
        }

        if (newDtos == null || newDtos.isEmpty()) {
            return;
        }

        persistTransactions(connectionId, newDtos);
    }


    public void persistTransactions(String connectionId, List<TransactionDto> newDtos) {
        List<String> incomingIds = newDtos.stream()
                .map(TransactionDto::getTransactionId)
                .toList();

        Set<String> existingIds = transactionRepository.findExistingIdsBySaltEdgeTransactionIdIn(incomingIds);

        List<TransactionDto> filteredDtos = newDtos.stream()
                .filter(dto -> !existingIds.contains(dto.getTransactionId()))
                .toList();

        if (filteredDtos.isEmpty()) {
            return;
        }

        Map<String, AccountEntity> accountMap = accountRepository.findAllByConnectionSaltEdgeConnectionId(connectionId)
                .stream()
                .collect(Collectors.toMap(AccountEntity::getSaltEdgeAccountId, Function.identity()));

        List<TransactionEntity> newEntities = filteredDtos.stream()
                .map(dto -> {
                    AccountEntity account = accountMap.get(dto.getAccountId());
                    if (account == null) {
                        log.warn("Account {} not found for transaction {}", dto.getAccountId(), dto.getTransactionId());
                    }
                    return mapToEntity(dto, account);
                })
                .toList();

        log.info("Saving {} new transactions for connection {}", newEntities.size(), connectionId);
        transactionRepository.saveAll(newEntities);
    }

    private TransactionEntity mapToEntity(TransactionDto dto, AccountEntity account) {
        return TransactionEntity.builder()
                .saltEdgeTransactionId(dto.getTransactionId())
                .account(account)
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .description(dto.getDescription())
                .category(dto.getCategory() != null && !dto.getCategory().isBlank() ? dto.getCategory() : "other")
                .madeOn(LocalDate.parse(dto.getMadeOn()))
                .status(dto.getStatus())
                .extra(dto.getExtra())
                .isSuspicious(false)
                .build();
    }

    private TransactionDto mapToDto(TransactionEntity entity) {
        return new TransactionDto(
                entity.getSaltEdgeTransactionId(),
                entity.getAccount() != null ? entity.getAccount().getSaltEdgeAccountId() : null,
                entity.getAmount(),
                entity.getCurrency(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getMadeOn().toString(),
                entity.getStatus(),
                entity.getExtra() != null ? entity.getExtra() : Map.of()
        );
    }
}