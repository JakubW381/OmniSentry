package dev.jakubw.omnisentry.controllers

import dev.jakubw.omnisentry.model.AuthRequest
import dev.jakubw.omnisentry.service.OmniUserDetailService
import jakarta.servlet.http.Cookie
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
        response: jakarta.servlet.http.HttpServletResponse
    ): ResponseEntity<String> {
        return try {
            val token = userDetailsService.login(authRequest)

            val cookie = Cookie("OmniSentryJwt", token).apply {
                path = "/"
                isHttpOnly = true
                maxAge = 3600
                // secure = true
            }
            response.addCookie(cookie)

            ResponseEntity.ok("Logged in successfully")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @PostMapping("/register")
    fun register(
        @RequestBody authRequest: AuthRequest,
        response: jakarta.servlet.http.HttpServletResponse
    ): ResponseEntity<String> {
        return try {
            val token = userDetailsService.register(authRequest)

            val cookie = Cookie("token", token).apply {
                path = "/"
                isHttpOnly = true
                maxAge = 3600
            }
            response.addCookie(cookie)

            ResponseEntity.ok("Registered and logged in")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }
}