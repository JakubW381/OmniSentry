package dev.jakubw.omnisentry.controllers

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class JwkController(private val rsaKey: RSAKey) {
    @GetMapping("/.well-known/jwks.json")
    fun getJwk(): Map<String,Any>{
        val jwkSet = JWKSet(rsaKey.toPublicJWK())
        return jwkSet.toJSONObject()
    }
}