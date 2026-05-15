package dev.jakubw.omnisentry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

data class UserDto(
    @JsonProperty("username")
    val username : String = "",
    @JsonProperty("name")
    val name : String = "",
    @JsonProperty("surname")
    val surname : String = "",
    @JsonProperty("dateOfBirth")
    val dateOfBirth : Instant = Instant.now(),
    @JsonProperty("email")
    val email : String= "",
)

data class UserRegistrationDto(
    @JsonProperty("username")
    val username : String= "",
    @JsonProperty("name")
    val name : String= "",
    @JsonProperty("surname")
    val surname : String = "",
    @JsonProperty("dateOfBirth")
    val dateOfBirth : Instant= Instant.now(),
    @JsonProperty("email")
    val email : String = "",
    @JsonProperty("pass")
    val pass : String = ""
)

data class UserPrincipal(
    val userId: UUID,
    val username: String,
    val roles: Set<String>
)
