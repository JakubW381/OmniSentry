package dev.jakubw.omnisentry.service

import com.google.protobuf.Timestamp
import dev.jakubw.omnisentry.dto.UserRegistrationDto
import dev.jakubw.omnisentry.grpc.UserRegistrationRequest
import dev.jakubw.omnisentry.grpc.UserRegistrationResponse
import dev.jakubw.omnisentry.grpc.UserRegistrationServiceGrpc.UserRegistrationServiceBlockingStub
import dev.jakubw.omnisentry.model.AuthRequest
import dev.jakubw.omnisentry.model.Role
import dev.jakubw.omnisentry.model.SignUpRequest
import dev.jakubw.omnisentry.model.UserDetailsEntity
import dev.jakubw.omnisentry.repository.UserDetailsRepository
import jakarta.persistence.EntityExistsException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service


@Service
class OmniUserDetailService(
    private val userDetailsRepository: UserDetailsRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val stub : UserRegistrationServiceBlockingStub
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

        val grpcRequest = UserRegistrationRequest.newBuilder()
            .setUsername(request.username)
            .setEmail(request.email)
            .setName(request.name)
            .setSurname(request.surname)
            .setDateOfBirth(Timestamp.newBuilder().setSeconds(request.dateOfBirth.epochSecond).setNanos(request.dateOfBirth.nano))
            .build()
        var response : UserRegistrationResponse
        try {
            response = stub.registerUser(grpcRequest)
            if (!response.result) {
                throw EntityExistsException("OmniUserDetailService.register ===> " + response.message)
            }
            else{
                println("OmniUserDetailService.register ===> User registered successfully")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Could not connect to Main-Backend via gRPC: ${e.message}")
        }
        val user = UserDetailsEntity(
            id = response.customerId,
            username = request.username,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.pass)!!,
            roles = setOf(Role.USER)
        )

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