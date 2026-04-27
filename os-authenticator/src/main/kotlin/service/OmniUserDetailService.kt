package dev.jakubw.omnisentry.service

import dev.jakubw.omnisentry.dto.UserRegistrationDto
import dev.jakubw.omnisentry.model.AuthRequest
import dev.jakubw.omnisentry.model.Role
import dev.jakubw.omnisentry.model.SignUpRequest
import dev.jakubw.omnisentry.model.UserDetailsEntity
import dev.jakubw.omnisentry.repository.UserDetailsRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class OmniUserDetailService(
    private val userDetailsRepository: UserDetailsRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) : UserDetailsService {

    fun login(request: AuthRequest): String {
        val user = userDetailsRepository.findByUsername(request.username)
            .orElseThrow { UsernameNotFoundException("User not found") }

        if (!passwordEncoder.matches(request.pass, user.passwordHash)) {
            throw IllegalArgumentException("Invalid password")
        }

        return jwtService.generateToken(user.toOmniUserDetails())
    }

    fun register(request: UserRegistrationDto): String {
        val principals = SignUpRequest(request.username, request.pass, request.email)
        validateRegisterRequest(principals)

        val user = UserDetailsEntity(
            username = request.username,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.pass)!!,
            roles = setOf(Role.USER)
        )

        /** TODO
         *  W tym miejscu Auth service powinien wysyłać GRPC do main-backend
         *  UserRegistrationDto
         */

        userDetailsRepository.save(user)

        return login(AuthRequest(request.username, request.pass))
    }

    fun validateRegisterRequest(request: SignUpRequest) {
        if (request.username.length < 8) throw IllegalArgumentException("Username too short")
        if (request.pass.length < 8) throw IllegalArgumentException("Password too short")
        if (userDetailsRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username already exists")
        }
        if (userDetailsRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("User with this email address already exists")
        }
    }

    override fun loadUserByUsername(username: String): UserDetails {
        return userDetailsRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("User not found") }
            .toOmniUserDetails()
    }
}