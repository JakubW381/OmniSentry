package dev.jakubw.omnisentry.services.internal;

import dev.jakubw.omnisentry.dto.ConnectionDto;
import dev.jakubw.omnisentry.dto.LastAttemptDto;
import dev.jakubw.omnisentry.models.ConnectionEntity;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import dev.jakubw.omnisentry.repos.TransactionRepository;
import dev.jakubw.omnisentry.repos.UserRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final SaltEdgeService saltEdgeService;
    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void saveConnection(String customerId, String connectionId){
        System.out.println("SaveConnection()");
        UserEntity user = userRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println("Getting connection from saltEdge");
        ConnectionDto connectionDto = saltEdgeService.getConnection(connectionId).block();
        System.out.println("Save connection connectionDto: " + connectionDto);
        ConnectionEntity connection = mapToEntity(Objects.requireNonNull(connectionDto));
        connection.setCreatedAt(String.valueOf(Instant.now()));
        ConnectionEntity saved = connectionRepository.save(connection);
        user.getConnectionIds().add(String.valueOf(saved.getSaltEdgeConnectionId()));
        userRepository.save(user);
    }

    @Transactional
    public List<ConnectionDto> getConnections(String customerId){
        return connectionRepository.findAllByCustomerId(customerId).stream().map(this::mapToDto).toList();
    }

    @Transactional
    public void removeConnection(String customerId, String connectionId){
        UserEntity user = userRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.getConnectionIds().remove(connectionId);
        transactionRepository.deleteAllBySaltEdgeConnectionId(connectionId);
        connectionRepository.deleteBySaltEdgeConnectionId(connectionId);
        userRepository.save(user);
    }

    private ConnectionEntity mapToEntity(ConnectionDto dto) {
        return ConnectionEntity.builder()
                .saltEdgeConnectionId(dto.getConnectionId())
                .customerId(dto.getCustomerId())
                .providerName(dto.getProviderName())
                .providerCode(dto.getProviderCode())
                .status(dto.getStatus())
                .lastDeviceType(dto.getLastAttempt().getDeviceType())
                .lastRemoteIp(dto.getLastAttempt().getRemoteIp())
                .build();
    }
    private ConnectionDto mapToDto(ConnectionEntity entity) {
        return new ConnectionDto(
                entity.getSaltEdgeConnectionId(),
                entity.getCustomerId(),
                entity.getProviderName(),
                entity.getProviderCode(),
                entity.getCreatedAt(),
                new LastAttemptDto(
                        entity.getLastDeviceType(),
                        entity.getLastRemoteIp()
                ),
                entity.getStatus()
        );
    }

}
