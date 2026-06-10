package routes

import domain.GenericResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import services.ImageService

fun Route.imageRoutes() {
    route("/images") {
        authenticate("auth-jwt") {

            // Image upload endpoint (Role Filter Added)
            post("/upload") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class) ?: "STUDENT"

                    // SECURITY BLOCK: Block normal students from spamming storage uploads
                    if (role != "TEACHER" && role != "ADMIN") {
                        return@post call.respond(HttpStatusCode.Forbidden, GenericResponse(false, "Access Denied: Restricted resources storage privilege verification error."))
                    }

                    val multipart = call.receiveMultipart()
                    val imageUrl = ImageService.uploadImage(multipart)

                    call.respond(UploadResponse(imageUrl))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error uploading image")
                }
            }

            // Image delete endpoint (Role Filter Added)
            delete("/{imageUrl}") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class) ?: "STUDENT"

                    if (role != "TEACHER" && role != "ADMIN") {
                        return@delete call.respond(HttpStatusCode.Forbidden, GenericResponse(false, "Access Denied."))
                    }

                    val imageUrl = call.parameters["imageUrl"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "Image URL required")

                    ImageService.deleteImage(imageUrl)
                    call.respond(MessageResponse("Image deleted successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error deleting image")
                }
            }
        }
    }
}

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class UploadResponse(val url: String)