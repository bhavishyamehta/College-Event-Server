package domain

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import utils.EnvConfig
import java.util.*

object AuthJWT {
    fun generateToken(enrollmentNumber: String): String {
        return JWT.create()
            .withAudience(EnvConfig.jwtAudience)
            .withIssuer(EnvConfig.jwtIssuer)
            .withClaim("enrollmentNumber", enrollmentNumber)
            .withExpiresAt(Date(System.currentTimeMillis() + 36_00_000 * 24 * 7)) // 7 days validity
            .sign(Algorithm.HMAC256(EnvConfig.jwtSecret))
    }
}