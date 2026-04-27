package dev.jakubw.omnisentry.repos;

import dev.jakubw.omnisentry.models.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
}
