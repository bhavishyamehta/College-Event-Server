package routes

import domain.GenericResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import services.UserService

fun Route.userRouting(userService: UserService) {
    authenticate("auth-jwt") {
        route("/api/user") {
            get("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val enrollmentNo = principal?.getClaim("enrollmentNumber", String::class) ?: ""

                val profile = userService.getStudentProfile(enrollmentNo)
                if (profile != null) {
                    call.respond(HttpStatusCode.OK, profile)
                } else {
                    call.respond(HttpStatusCode.NotFound, GenericResponse(false, "Student record missing"))
                }
            }
        }
    }
}