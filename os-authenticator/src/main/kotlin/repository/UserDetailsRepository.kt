package dev.jakubw.omnisentry.repository

import dev.jakubw.omnisentry.model.UserDetailsEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserDetailsRepository : JpaRepository<UserDetailsEntity, String> {
    fun findByUsername(username : String) : Optional<UserDetailsEntity>
    fun existsByUsername(username : String) : Boolean
    fun existsByEmail(email : String) : Boolean
}