import io.ktor.server.application.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import routes.authRouting
import routes.eventRouting
import routes.userRouting
import services.AuthService
import services.EventService
import services.UserService

fun Application.configureRouting(
    authService: AuthService,
    userService: UserService,
    eventService: EventService
) {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }

        authRouting(authService)
        eventRouting(eventService)
        userRouting(userService)
    }
}