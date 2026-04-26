package dev.jakubw.omnisentry.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

@Configuration
open class JwksConfig{

    @Value($$"${app.rsa.public-key}")
    private lateinit var publicKeyRaw: String

    @Value($$"${app.rsa.private-key}")
    private lateinit var privateKeyRaw: String

    @Bean
    open fun rsaKey(): RSAKey {
        val keyFactory = KeyFactory.getInstance("RSA")

        val publicBytes = Base64.getDecoder().decode(publicKeyRaw)
        val privateBytes = Base64.getDecoder().decode(privateKeyRaw)

        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicBytes)) as RSAPublicKey
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateBytes)) as RSAPrivateKey

        return RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID("omnisentry-static-key-id")
            .build()
    }

    @Bean
    open fun jwkSource(rsaKey: RSAKey): JWKSource<SecurityContext> {
        val jwkSet = JWKSet(rsaKey)
        return ImmutableJWKSet(jwkSet)
    }
}