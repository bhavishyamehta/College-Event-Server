package routes

import com.example.domain.GenericResponse
import com.example.domain.LoginRequest
import com.example.domain.RegisterRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import services.AuthService

fun Route.authRouting(authService: AuthService) {
    route("/api/auth") {

        // 1. Unified Registration Routing Point supporting Roles mapping validation parameters
        post("/register") {
            val req = call.receiveNullable<RegisterRequest>() ?: return@post call.respond(
                HttpStatusCode.BadRequest, GenericResponse(false, "Invalid Fields Structure")
            )

            // Validate incoming payload inputs parsing correctness constraints
            val validatedRole = req.role.uppercase().trim()
            if (validatedRole != "STUDENT" && validatedRole != "TEACHER" && validatedRole != "ADMIN") {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    GenericResponse(false, "Invalid Access Scope Role definition context assignment parameter error.")
                )
            }

            val response = authService.registerUser(req)
            if (response != null) {
                call.respond(HttpStatusCode.Created, response)
            } else {
                call.respond(HttpStatusCode.Conflict, GenericResponse(false, "Identity credentials verification collision conflict error."))
            }
        }

        // 2. Login Endpoint processing validation structures
        post("/login") {
            val req = call.receiveNullable<LoginRequest>() ?: return@post call.respond(
                HttpStatusCode.BadRequest, GenericResponse(false, "Invalid parameters")
            )

            val response = authService.loginUser(req)
            if (response != null) {
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.Unauthorized, GenericResponse(false, "Invalid Enrollment Identification context keys or authentication password mismatch."))
            }
        }
    }
}