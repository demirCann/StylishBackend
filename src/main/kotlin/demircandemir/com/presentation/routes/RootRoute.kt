package demircandemir.com.presentation.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.rootRoutes() = routing {
    // Root route
    routing {
        get("/") {
            call.respond(
                mapOf(
                    "name" to "Stylish Backend API",
                    "version" to "1.0.0",
                    "status" to "running"
                )
            )
        }
    }
}