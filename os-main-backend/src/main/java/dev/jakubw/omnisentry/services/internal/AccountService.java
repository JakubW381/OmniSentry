package dev.jakubw.omnisentry.services.internal;

import dev.jakubw.omnisentry.dto.AccountDto;
import dev.jakubw.omnisentry.dto.AccountExtraDto;
import dev.jakubw.omnisentry.models.AccountEntity;
import dev.jakubw.omnisentry.models.ConnectionEntity;
import dev.jakubw.omnisentry.repos.AccountRepository;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final ConnectionRepository connectionRepository;
    private final SaltEdgeService saltEdgeService;

    @Transactional
    public List<AccountDto> getAccounts(String connectionId) {
        List<AccountDto> remoteAccounts = saltEdgeService.getAccounts(connectionId);

        if (!remoteAccounts.isEmpty()) {
            syncAccounts(connectionId, remoteAccounts);
        }

        return accountRepository.findAllByConnectionSaltEdgeConnectionId(connectionId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public void syncAccounts(String connectionId, List<AccountDto> dtos) {
        ConnectionEntity connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("Connection not found with id: " + connectionId));

        List<String> accountIds = dtos.stream()
                .map(AccountDto::getSaltEdgeAccountId)
                .toList();

        Map<String, AccountEntity> existingAccountsMap = accountRepository.findAllById(accountIds)
                .stream()
                .collect(Collectors.toMap(AccountEntity::getSaltEdgeAccountId, Function.identity()));

        List<AccountEntity> entitiesToSave = dtos.stream()
                .map(dto -> {
                    AccountEntity entity = existingAccountsMap.getOrDefault(
                            dto.getSaltEdgeAccountId(),
                            mapToEntity(dto, connection)
                    );
                    updateEntityFields(entity, dto, connection);
                    return entity;
                })
                .toList();

        accountRepository.saveAll(entitiesToSave);
    }

    private void updateEntityFields(AccountEntity entity, AccountDto dto, ConnectionEntity connection) {
        entity.setBalance(dto.getBalance());
        entity.setUpdatedAt(dto.getUpdatedAt());
        entity.setName(dto.getName());
        entity.setConnection(connection);

        if (dto.getExtra() != null) {
            entity.setStatus(dto.getExtra().getStatus());
            entity.setHolderName(dto.getExtra().getHolderName());
            entity.setIban(dto.getExtra().getIban());
            entity.setBban(dto.getExtra().getBban());
        }
    }

    private AccountEntity mapToEntity(AccountDto dto, ConnectionEntity connection) {
        return AccountEntity.builder()
                .saltEdgeAccountId(dto.getSaltEdgeAccountId())
                .connection(connection)
                .name(dto.getName())
                .balance(dto.getBalance())
                .currency(dto.getCurrency())
                .nature(dto.getNature())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    private AccountDto mapToDto(AccountEntity entity) {
        AccountExtraDto extra = new AccountExtraDto(
                entity.getIban(),
                entity.getBban(),
                entity.getStatus(),
                entity.getHolderName()
        );

        return new AccountDto(
                entity.getSaltEdgeAccountId(),
                entity.getConnection() != null ? entity.getConnection().getSaltEdgeConnectionId() : null,
                entity.getName(),
                entity.getBalance(),
                entity.getNature(),
                entity.getCurrency(),
                extra,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}