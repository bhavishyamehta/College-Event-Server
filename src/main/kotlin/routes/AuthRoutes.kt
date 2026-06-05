package routes

import com.example.domain.GenericResponse
import com.example.domain.LoginRequest
import com.example.domain.RegisterRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import services.AuthService

fun Route.authRouting(authService: AuthService) {
    route("/api/auth") {
        
        post("/register") {
            val req = call.receiveNullable<RegisterRequest>() ?: return@post call.respond(
                HttpStatusCode.BadRequest, GenericResponse(false, "Invalid Fields Structure")
            )
            
            val response = authService.registerStudent(req)
            if (response != null) {
                call.respond(HttpStatusCode.Created, response)
            } else {
                call.respond(HttpStatusCode.Conflict, GenericResponse(false, "Enrollment or Email already registered"))
            }
        }

        post("/login") {
            val req = call.receiveNullable<LoginRequest>() ?: return@post call.respond(
                HttpStatusCode.BadRequest, GenericResponse(false, "Invalid parameters")
            )

            val response = authService.loginStudent(req)
            if (response != null) {
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.Unauthorized, GenericResponse(false, "Invalid Enrollment Number or Password"))
            }
        }
    }
}