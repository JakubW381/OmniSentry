package dev.jakubw.omnisentry.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

data class OmniUserDetails(
    val id: UUID,
    private val username: String,
    private val pass: String,
    val roles: Set<Role>
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }
    }

    override fun getPassword(): String = pass

    override fun getUsername(): String = username

    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}

data class AuthRequest(val username: String, val pass: String)
data class SignUpRequest(val username: String, val pass: String, val email: String)
enum class Role {
    USER, ADMIN
}