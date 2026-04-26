package dev.jakubw.omnisentry.controllers

import dev.jakubw.omnisentry.model.AuthRequest
import dev.jakubw.omnisentry.model.SignUpRequest
import dev.jakubw.omnisentry.service.OmniUserDetailService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val userDetailsService: OmniUserDetailService
) {

    @PostMapping("/login")
    fun login(
        @RequestBody authRequest: AuthRequest,
        response: HttpServletResponse
    ): ResponseEntity<String> {
        return try {
            val token = userDetailsService.login(authRequest)
            response.addCookie(createCookie(token))
            ResponseEntity.ok("Logged in successfully")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @PostMapping("/register")
    fun register(
        @RequestBody signUpRequest: SignUpRequest,
        response: HttpServletResponse
    ): ResponseEntity<String> {
        return try {
            val token = userDetailsService.register(signUpRequest)
            response.addCookie(createCookie(token))
            ResponseEntity.ok("Registered and logged in")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }


    private fun createCookie(token: String): Cookie {
        return Cookie("OmniSentryJwt", token).apply {
            path = "/"
            isHttpOnly = true
            maxAge = 3600
            // secure = true
        }
    }
}