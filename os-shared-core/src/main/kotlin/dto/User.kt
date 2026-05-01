package dev.jakubw.omnisentry.dto

import java.time.Instant
import java.util.*

data class UserDto(
    val username : String,
    val name : String,
    val surname : String,
    val dateOfBirth : Instant,
    val email : String,
)

data class UserRegistrationDto(
    val username : String,
    val name : String,
    val surname : String,
    val dateOfBirth : Instant,
    val email : String,
    val pass : String
)

data class UserPrincipal(
    val userId: UUID,
    val username: String,
    val roles: Set<String>
)
