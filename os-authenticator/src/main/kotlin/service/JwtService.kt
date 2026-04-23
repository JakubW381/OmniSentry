package dev.jakubw.omnisentry.service

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import dev.jakubw.omnisentry.model.OmniUserDetails
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

@Service
class JwtService(private val rsaKey: RSAKey) {

    fun generateToken(userDetails: OmniUserDetails): String {
        val now = Instant.now()

        val claims = JWTClaimsSet.Builder()
            .issuer("OmniSentry-Auth")
            .subject(userDetails.id.toString())
            .claim("username", userDetails.username)
            .claim("roles", userDetails.authorities.map { it.authority })
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
            .build()

        // RSA Header
        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.keyID)
            .build()

        // Sign
        val signedJWT = SignedJWT(header, claims)
        signedJWT.sign(RSASSASigner(rsaKey))

        return signedJWT.serialize()
    }
}