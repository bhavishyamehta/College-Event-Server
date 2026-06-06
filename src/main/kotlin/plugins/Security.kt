package plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import utils.Env

fun Application.configureSecurity() {
    authentication {
        jwt("auth-jwt") {
            realm = "access to college events"
            verifier(
                JWT
                    .require(Algorithm.HMAC256(Env.JWT_SECRET))
                    .withAudience(Env.JWT_AUDIENCE)
                    .withIssuer(Env.JWT_ISSUER)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("enrollmentNumber").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}