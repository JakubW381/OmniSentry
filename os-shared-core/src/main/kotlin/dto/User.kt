package dev.jakubw.omnisentry.dto

import java.math.BigDecimal
import java.util.UUID
import java.time.Instant

data class UserDto(
    val id: UUID,
    val username: String,
    val name: String,
    val surname: String,
    val dateOfBirth: Instant,
    val email: String,
    val permissions: Set<String>,
    val isActive: Boolean
)

data class UserPrincipal(
    val userId: UUID,
    val username: String,
    val roles: Set<String>
)

data class Account(
    val id: UUID,
    val userId: UUID,
    val provider: String,
    val externalAccountId: String,
    val accountNumber: String?,
    val balance: BigDecimal,
    val currency: String,
    val name: String
)

enum class AccountStatus { ACTIVE, FROZEN, CLOSED }