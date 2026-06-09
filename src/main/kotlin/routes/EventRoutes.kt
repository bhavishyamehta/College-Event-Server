package routes

import com.example.domain.GenericResponse
import com.example.domain.CreateEventRequest
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import services.EventService

fun Route.eventRouting(eventService: EventService) {
    authenticate("auth-jwt") {
        route("/api/events") {

            // ── READ ALL EVENTS ──────────────────────────────────────────────────
            get {
                val principal = call.principal<JWTPrincipal>()
                val enrollmentNo = principal?.getClaim("enrollmentNumber", String::class) ?: ""
                val category = call.request.queryParameters["category"]

                val events = eventService.getAllEvents(category, enrollmentNo)
                call.respond(HttpStatusCode.OK, events)
            }

            // ── CREATE EVENT (ROLE GUARD APPLIED) ──────────────────────────────
            post {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.getClaim("role", String::class) ?: "STUDENT"

                // 🔴 AUTHORIZATION EDGE CASE SHIELD
                if (role != "TEACHER" && role != "ADMIN") {
                    return@post call.respond(HttpStatusCode.Forbidden, GenericResponse(false, "Access Denied: Only Teachers or Admins can perform this action."))
                }

                val req = call.receiveNullable<CreateEventRequest>() ?: return@post call.respond(
                    HttpStatusCode.BadRequest, GenericResponse(false, "Invalid Event Data Payload")
                )

                val generatedId = eventService.createEvent(req)
                if (generatedId != null) {
                    call.respond(HttpStatusCode.Created, mapOf("success" to true, "message" to "Event Created Successfully", "id" to generatedId))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, GenericResponse(false, "Failed to create event entry"))
                }
            }

            // ── READ SINGLE EVENT DETAIL ─────────────────────────────────────────
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

            // ── UPDATE EVENT (ROLE GUARD APPLIED) ──────────────────────────────
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.getClaim("role", String::class) ?: "STUDENT"
                val eventId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, GenericResponse(false, "ID missing"))

                if (role != "TEACHER" && role != "ADMIN") {
                    return@put call.respond(HttpStatusCode.Forbidden, GenericResponse(false, "Access Denied."))
                }

                val req = call.receiveNullable<CreateEventRequest>() ?: return@put call.respond(
                    HttpStatusCode.BadRequest, GenericResponse(false, "Invalid Payload structure")
                )

                val successfullyUpdated = eventService.updateEvent(eventId, req)
                if (successfullyUpdated) {
                    call.respond(HttpStatusCode.OK, GenericResponse(true, "Event modified safely"))
                } else {
                    call.respond(HttpStatusCode.NotFound, GenericResponse(false, "Target event entry matching identity records missing"))
                }
            }

            // ── DELETE EVENT (ROLE GUARD APPLIED) ──────────────────────────────
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.getClaim("role", String::class) ?: "STUDENT"
                val eventId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, GenericResponse(false, "ID missing"))

                if (role != "TEACHER" && role != "ADMIN") {
                    return@delete call.respond(HttpStatusCode.Forbidden, GenericResponse(false, "Access Denied."))
                }

                val successfullyDeleted = eventService.deleteEvent(eventId)
                if (successfullyDeleted) {
                    call.respond(HttpStatusCode.OK, GenericResponse(true, "Event dropped completely"))
                } else {
                    call.respond(HttpStatusCode.NotFound, GenericResponse(false, "Event context identifier mismatch data log error"))
                }
            }

            // ── STUDENT SELF REGISTRATION ENPOINTS ───────────────────────────────
            post("/{id}/register") {
                val principal = call.principal<JWTPrincipal>()
                val enrollmentNo = principal?.getClaim("enrollmentNumber", String::class) ?: ""
                val eventId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, GenericResponse(false, "Invalid Event ID"))

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
                val eventId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, GenericResponse(false, "Invalid Event ID"))

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