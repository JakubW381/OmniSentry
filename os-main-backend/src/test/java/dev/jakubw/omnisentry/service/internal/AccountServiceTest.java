package dev.jakubw.omnisentry.service.internal;

import dev.jakubw.omnisentry.dto.AccountDto;
import dev.jakubw.omnisentry.dto.AccountExtraDto;
import dev.jakubw.omnisentry.models.AccountEntity;
import dev.jakubw.omnisentry.models.ConnectionEntity;
import dev.jakubw.omnisentry.repos.AccountRepository;
import dev.jakubw.omnisentry.repos.ConnectionRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import dev.jakubw.omnisentry.services.internal.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private SaltEdgeService saltEdgeService;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("Should save new account when it does not exist")
    public void shouldSaveNotExistingAccount() {
        // Given
        String id = "123";

        ConnectionEntity connection = ConnectionEntity.builder()
                .saltEdgeConnectionId(id)
                .accounts(new HashSet<>())
                .build();

        AccountExtraDto extraDto = new AccountExtraDto("IBAN123", "BBAN123", "active", "John");

        AccountDto newAccount = new AccountDto(
                id,
                id,
                "John",
                BigDecimal.ONE,
                "nature",
                "EUR",
                extraDto,
                LocalDate.now().toString(),
                LocalDate.now().toString()
        );

        AccountEntity savedEntity = AccountEntity.builder()
                .saltEdgeAccountId(id)
                .name("John")
                .balance(BigDecimal.ONE)
                .currency("EUR")
                .nature("nature")
                .iban("IBAN123")
                .bban("BBAN123")
                .holderName("John")
                .status("active")
                .createdAt(LocalDate.now().toString())
                .updatedAt(LocalDate.now().toString())
                .connection(connection)
                .build();

        when(connectionRepository.findById(id)).thenReturn(Optional.of(connection));
        when(saltEdgeService.getAccounts(id)).thenReturn(List.of(newAccount));

        when(accountRepository.findAllByConnectionSaltEdgeConnectionId(id)).thenReturn(List.of(savedEntity));

        // When
        List<AccountDto> dtoList = accountService.getAccounts(id);

        // Then
        assertThat(connection.getAccounts()).hasSize(1);

        AccountEntity addedAccount = connection.getAccounts().iterator().next();
        assertThat(addedAccount.getSaltEdgeAccountId()).isEqualTo(newAccount.getSaltEdgeAccountId());
        assertThat(addedAccount.getStatus()).isEqualTo("active");
        assertThat(addedAccount.getHolderName()).isEqualTo("John");
        assertThat(addedAccount.getConnection()).isEqualTo(connection);

        assertThat(dtoList).hasSize(1);
        assertThat(dtoList.get(0).getSaltEdgeAccountId()).isEqualTo(id);
    }
}