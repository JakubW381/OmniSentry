package dev.jakubw.omnisentry.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey

@Configuration
class JwksConfig {

    @Bean
    fun generateRsaKey(): RSAKey {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048)
        }.generateKeyPair()

        return RSAKey.Builder(keyPair.public as RSAPublicKey)
            .privateKey(keyPair.private)
            .keyID("omnisentry-key-id")
            .build()
    }

    @Bean
    fun jwkSource(rsaKey: RSAKey): JWKSource<SecurityContext> {
        val jwkSet = JWKSet(rsaKey)
        return ImmutableJWKSet(jwkSet)
    }
}