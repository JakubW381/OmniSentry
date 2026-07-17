package dev.jakubw.omnisentry.services.internal;

import dev.jakubw.omnisentry.dto.AccountDto;
import dev.jakubw.omnisentry.dto.AccountExtraDto;
import dev.jakubw.omnisentry.models.AccountEntity;
import dev.jakubw.omnisentry.repos.AccountRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final SaltEdgeService saltEdgeService;

    @Transactional
    public List<AccountDto> getAccounts(String connectionId) {
        List<AccountDto> synced = saltEdgeService.getAccounts(connectionId)
                .map(dto -> {
                    saveOrUpdate(dto);
                    return dto;
                })
                .collectList()
                .block();

        return accountRepository.findAllByConnectionId(connectionId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private void saveOrUpdate(AccountDto dto) {
        AccountEntity entity = accountRepository.findBySaltEdgeAccountId(dto.getSaltEdgeAccountId())
                .orElseGet(() -> mapToEntity(dto));

        entity.setBalance(dto.getBalance());
        entity.setUpdatedAt(dto.getUpdatedAt());
        entity.setName(dto.getName());

        if (dto.getExtra() != null) {
            entity.setStatus(dto.getExtra().getStatus());
            entity.setHolderName(dto.getExtra().getHolderName());
        }

        accountRepository.save(entity);
    }

    private AccountEntity mapToEntity(AccountDto dto) {
        return AccountEntity.builder()
                .saltEdgeAccountId(dto.getSaltEdgeAccountId())
                .connectionId(dto.getConnectionId())
                .name(dto.getName())
                .balance(dto.getBalance())
                .currency(dto.getCurrency())
                .nature(dto.getNature())
                .iban(dto.getExtra() != null ? dto.getExtra().getIban() : null)
                .bban(dto.getExtra() != null ? dto.getExtra().getBban() : null)
                .holderName(dto.getExtra() != null ? dto.getExtra().getHolderName() : null)
                .status(dto.getExtra() != null ? dto.getExtra().getStatus() : null)
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
                entity.getConnectionId(),
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
