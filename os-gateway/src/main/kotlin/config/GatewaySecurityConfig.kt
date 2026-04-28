package config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class GatewaySecurityConfig {

    @Value("\${security.oauth2.resourceserver.jwt.jwt-set-uri}")
    val jwksUri: String? = null

    @Bean
    fun jwtDecored(): JwtDecoder{
        return NimbusJwtDecoder
            .withJwkSetUri(jwksUri)
            .build()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain{
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/backend//callbacks/saltedge/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth ->
                oauth.jwt(Customizer.withDefaults())
                oauth.bearerTokenResolver(cookieTokenResolver())
            }
        return http.build()
    }

    private fun cookieTokenResolver() = BearerTokenResolver { request ->
        request.cookies?.find { it.name == "OmniSentryJwt" }?.value
    }
}