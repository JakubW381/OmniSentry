package dev.jakubw.omnisentry.repository

import dev.jakubw.omnisentry.dto.UserPrincipal
import dev.jakubw.omnisentry.model.OmniUserDetails
import dev.jakubw.omnisentry.model.UserDetailsEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserDetailsRepository : JpaRepository<UserDetailsEntity, UUID> {
    fun findByUsername(username : String) : Optional<UserDetailsEntity>
    fun existsByUsername(username : String) : Boolean
    fun existsByEmail(email : String) : Boolean
}