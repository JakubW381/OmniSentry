package dev.jakubw.omnisentry.services.internal;

import dev.jakubw.omnisentry.dto.ConnectionDto;
import dev.jakubw.omnisentry.dto.LastAttemptDto;
import dev.jakubw.omnisentry.models.ConnectionEntity;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import dev.jakubw.omnisentry.repos.TransactionRepository;
import dev.jakubw.omnisentry.repos.UserRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final SaltEdgeService saltEdgeService;
    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveConnection(String customerId, String connectionId) {
        ConnectionDto connectionDto = saltEdgeService.getConnection(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("Connection not found in SaltEdge: " + connectionId));

        persistConnection(customerId, connectionDto);
    }


    public void persistConnection(String customerId, ConnectionDto dto) {
        UserEntity user = userRepository.findBySaltEdgeCustomerId(customerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with customerId: " + customerId));

        user.getConnections().stream()
                .filter(c -> c.getSaltEdgeConnectionId().equals(dto.getConnectionId()))
                .findFirst()
                .ifPresentOrElse(
                        existingConnection -> updateConnectionFields(existingConnection, dto),
                        () -> {
                            ConnectionEntity newConnection = mapToEntity(dto, user);
                            newConnection.setCreatedAt(Instant.now());
                            user.addConnection(newConnection);
                        }
                );
    }

    @Transactional(readOnly = true)
    public List<ConnectionDto> getConnections(String customerId) {
        return connectionRepository.findAllByUserSaltEdgeCustomerId(customerId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public void removeConnection(String customerId, String connectionId) {
        if (!userRepository.existsBySaltEdgeCustomerId(customerId)) {
            throw new EntityNotFoundException("User not found with customerId: " + customerId);
        }
        connectionRepository.deleteById(connectionId);
    }

    private ConnectionEntity mapToEntity(ConnectionDto dto, UserEntity user) {
        LastAttemptDto lastAttemptDto = dto.getLastAttempt();
        return ConnectionEntity.builder()
                .saltEdgeConnectionId(dto.getConnectionId())
                .user(user)
                .providerName(dto.getProviderName())
                .providerCode(dto.getProviderCode())
                .status(dto.getStatus())
                .lastDeviceType(lastAttemptDto.getDeviceType())
                .lastRemoteIp(lastAttemptDto.getRemoteIp())
                .build();
    }


    private ConnectionDto mapToDto(ConnectionEntity entity) {
        return new ConnectionDto(
                entity.getSaltEdgeConnectionId(),
                entity.getUser() != null ? entity.getUser().getSaltEdgeCustomerId() : null ,
                entity.getProviderName(),
                entity.getProviderCode(),
                entity.getCreatedAt().toString(),
                new LastAttemptDto(
                        entity.getLastDeviceType(),
                        entity.getLastRemoteIp()
                ),
                entity.getStatus()
        );
    }
    private void updateConnectionFields(ConnectionEntity connection, ConnectionDto dto) {
        LastAttemptDto lastAttemptDto = dto.getLastAttempt();
        connection.setProviderName(dto.getProviderName());
        connection.setProviderCode(dto.getProviderCode());
        connection.setStatus(dto.getStatus());
        if (lastAttemptDto != null) {
            connection.setLastDeviceType(lastAttemptDto.getDeviceType());
            connection.setLastRemoteIp(lastAttemptDto.getRemoteIp());
        }
    }
}