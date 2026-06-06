package domain

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import utils.Env
import java.util.*

object AuthJWT {
    fun generateToken(enrollmentNumber: String): String {
        return JWT.create()
            .withAudience(Env.JWT_AUDIENCE)
            .withIssuer(Env.JWT_ISSUER)
            .withClaim("enrollmentNumber", enrollmentNumber)
            .withExpiresAt(Date(System.currentTimeMillis() + 36_00_000 * 24 * 7)) // 7 days validity
            .sign(Algorithm.HMAC256(Env.JWT_SECRET))
    }
}