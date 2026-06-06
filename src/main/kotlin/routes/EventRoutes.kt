package routes

import com.example.domain.GenericResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import services.EventService

fun Route.eventRouting(eventService: EventService) {
    authenticate("auth-jwt") {
        route("/api/events") {
            
            get {
                val principal = call.principal<JWTPrincipal>()
                val enrollmentNo = principal?.getClaim("enrollmentNumber", String::class) ?: ""
                val category = call.request.queryParameters["category"]
                
                val events = eventService.getAllEvents(category, enrollmentNo)
                call.respond(HttpStatusCode.OK, events)
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val enrollmentNo = principal?.getClaim("enrollmentNumber", String::class) ?: ""
                val eventId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, GenericResponse(false, "Malformed or missing ID")
                )

                val eventDetail = eventService.getEventDetail(eventId, enrollmentNo)
                if (eventDetail != null) {
                    call.respond(HttpStatusCode.OK, eventDetail)
                } else {
                    call.respond(HttpStatusCode.NotFound, GenericResponse(false, "Event not found"))
                }
            }

            post("/{id}/register") {
                val principal = call.principal<JWTPrincipal>()
                val enrollmentNo = principal?.getClaim("enrollmentNumber", String::class) ?: ""
                val eventId = call.parameters["id"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest, GenericResponse(false, "Invalid Event ID")
                )

                val success = eventService.registerForEvent(eventId, enrollmentNo)
                if (success) {
                    call.respond(HttpStatusCode.OK, GenericResponse(true, "Successfully registered for the event!"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, GenericResponse(false, "Registration failed. Seat filled or Event closing."))
                }
            }

            post("/{id}/deregister") {
                val principal = call.principal<JWTPrincipal>()
                val enrollmentNo = principal?.getClaim("enrollmentNumber", String::class) ?: ""
                val eventId = call.parameters["id"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest, GenericResponse(false, "Invalid Event ID")
                )

                val success = eventService.deregisterFromEvent(eventId, enrollmentNo)
                if (success) {
                    call.respond(HttpStatusCode.OK, GenericResponse(true, "Successfully deregistered from the event."))
                } else {
                    call.respond(HttpStatusCode.BadRequest, GenericResponse(false, "Deregistration failed or record not found."))
                }
            }
        }
    }
}