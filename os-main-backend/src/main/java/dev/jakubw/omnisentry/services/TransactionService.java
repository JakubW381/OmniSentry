package dev.jakubw.omnisentry.services;

import dev.jakubw.omnisentry.dto.TransactionDto;
import dev.jakubw.omnisentry.repos.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final SaltEdgeService saltEdgeService;
    private final TransactionRepository transactionRepository;

    public Flux<TransactionDto> getTransactions(String connectionId){

    }




}
