package dev.jakubw.omnisentry.model

import jakarta.persistence.*

@Entity
@Table(name = "users")
class UserDetailsEntity(

    @field:Id
    var id: String = "",

    @field:Column(unique = true, nullable = false)
    var username: String = "",

    @field:Column(unique = true, nullable = false)
    var email: String = "",

    @field:Column(nullable = false)
    var passwordHash: String = "",

    @field:ElementCollection(fetch = FetchType.EAGER)
    @field:Enumerated(EnumType.STRING)
    @field:CollectionTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @JvmSuppressWildcards
    var roles: Set<Role> = setOf(Role.USER)

) {
    fun toOmniUserDetails() = OmniUserDetails(id, username, passwordHash, roles)
}