package domain

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import utils.EnvConfig
import java.util.*

object AuthJWT {

    // Updated signature to accept BOTH enrollment number and role
    fun generateToken(enrollmentNumber: String, role: String): String {
        return JWT.create()
            .withAudience(EnvConfig.jwtAudience)
            .withIssuer(EnvConfig.jwtIssuer)
            .withClaim("enrollmentNumber", enrollmentNumber) // Existing Claim
            .withClaim("role", role)                        // NEW RULE: Embedded user role custom claim
            .withExpiresAt(Date(System.currentTimeMillis() + 36_00_000 * 24 * 7)) // 7 days validity
            .sign(Algorithm.HMAC256(EnvConfig.jwtSecret))
    }
}