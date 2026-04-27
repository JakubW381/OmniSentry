package dev.jakubw.omnisentry.services;

import dev.jakubw.omnisentry.dto.ConnectionDto;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final SaltEdgeService saltEdgeService;
    private final ConnectionRepository connectionRepository;
}
