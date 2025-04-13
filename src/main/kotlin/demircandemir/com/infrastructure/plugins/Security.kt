package demircandemir.com.infrastructure.plugins

import demircandemir.com.infrastructure.security.JwtConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*

fun Application.configureSecurity(jwtConfig: JwtConfig) {
    // Configure JWT authentication
    authentication {
        jwt("auth-jwt") {
            realm = jwtConfig.getRealm()
            verifier(jwtConfig.verifier)

            validate { credential ->
                try {
                    val userId = credential.payload.getClaim("userId").asInt()
                    if (userId != 0) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }

    // Configure CORS
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)

        // Configure your frontend domain in production
        anyHost()

        // Allow credentials
        allowCredentials = true
    }
} 