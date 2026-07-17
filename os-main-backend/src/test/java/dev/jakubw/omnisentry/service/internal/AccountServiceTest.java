package dev.jakubw.omnisentry.service.internal;

import dev.jakubw.omnisentry.dto.AccountDto;
import dev.jakubw.omnisentry.dto.AccountExtraDto;
import dev.jakubw.omnisentry.models.AccountEntity;
import dev.jakubw.omnisentry.repos.AccountRepository;
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
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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
    private SaltEdgeService saltEdgeService;

    @InjectMocks
    private AccountService accountService;

    @Captor
    private ArgumentCaptor<AccountEntity> accEntityCaptor;

    @Test
    @DisplayName("Should save new account when it does not exist")
    public void shouldSaveNotExistingAccount(){
        // Given
        String id = "123";

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

        when(accountRepository.findBySaltEdgeAccountId(id)).thenReturn(Optional.empty());
        when(saltEdgeService.getAccounts(id)).thenReturn(Flux.just(newAccount));

        when(accountRepository.findAllByConnectionId(id)).thenReturn(Collections.emptyList());

        // When
        List<AccountDto> dtoList = accountService.getAccounts(id);

        // Then
        verify(accountRepository).save(accEntityCaptor.capture());
        AccountEntity captured = accEntityCaptor.getValue();

        assertThat(captured.getSaltEdgeAccountId()).isEqualTo(newAccount.getSaltEdgeAccountId());
        assertThat(captured.getConnectionId()).isEqualTo(newAccount.getConnectionId());
        assertThat(captured.getStatus()).isEqualTo("active");
        assertThat(captured.getHolderName()).isEqualTo("John");
        assertThat(dtoList.contains(newAccount));
    }
}