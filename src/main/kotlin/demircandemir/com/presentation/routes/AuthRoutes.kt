package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.*
import demircandemir.com.application.service.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.authRoutes(authService: AuthService) {
    routing {
        route("/api/auth") {
            // Register a new user
            post("/register") {
                try {
                    val request = call.receive<CreateUserRequest>()

                    authService.registerUser(request).fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.Created, MessageResponse(
                                    "User registered successfully. Please check your email to verify your account."
                                )
                            )
                        },
                        onFailure = { error ->
                            when (error) {
                                is IllegalArgumentException -> call.respond(
                                    HttpStatusCode.BadRequest,
                                    MessageResponse(error.message ?: "Invalid request")
                                )

                                else -> {
                                    call.application.log.error("Error registering user", error)
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        MessageResponse("An unexpected error occurred")
                                    )
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    call.application.log.error("Error processing registration request", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        MessageResponse("An unexpected error occurred")
                    )
                }
            }

            // Login
            post("/login") {
                try {
                    val request = call.receive<LoginRequest>()

                    authService.loginUser(request).fold(
                        onSuccess = { response ->
                            call.respond(response)
                        },
                        onFailure = { error ->
                            when (error) {
                                is IllegalArgumentException -> call.respond(
                                    HttpStatusCode.BadRequest,
                                    MessageResponse(error.message ?: "Invalid credentials")
                                )

                                else -> {
                                    call.application.log.error("Error during login", error)
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        MessageResponse("An unexpected error occurred")
                                    )
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    call.application.log.error("Error processing login request", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        MessageResponse("An unexpected error occurred")
                    )
                }
            }

            // Refresh token
            post("/refresh-token") {
                try {
                    val request = call.receive<RefreshTokenRequest>()

                    authService.refreshToken(request).fold(
                        onSuccess = { response ->
                            call.respond(response)
                        },
                        onFailure = { error ->
                            when (error) {
                                is IllegalArgumentException -> call.respond(
                                    HttpStatusCode.BadRequest,
                                    MessageResponse(error.message ?: "Invalid token")
                                )

                                else -> {
                                    call.application.log.error("Error refreshing token", error)
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        MessageResponse("An unexpected error occurred")
                                    )
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    call.application.log.error("Error processing refresh token request", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        MessageResponse("An unexpected error occurred")
                    )
                }
            }

            // Verify email
            get("/verify-email") {
                try {
                    val token = call.request.queryParameters["token"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Token is required"))

                    val email = call.request.queryParameters["email"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, MessageResponse("Email is required"))

                    val request = VerifyEmailRequest(token, email)

                    authService.verifyEmail(request).fold(
                        onSuccess = { verified ->
                            if (verified) {
                                call.respond(MessageResponse("Email verified successfully. You can now log in."))
                            } else {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    MessageResponse("Failed to verify email. Invalid or expired token.")
                                )
                            }
                        },
                        onFailure = { error ->
                            call.application.log.error("Error verifying email", error)
                            call.respond(
                                HttpStatusCode.BadRequest,
                                MessageResponse(error.message ?: "Failed to verify email")
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.application.log.error("Error processing email verification", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        MessageResponse("An unexpected error occurred")
                    )
                }
            }

            // Request password reset
            post("/forgot-password") {
                try {
                    val request = call.receive<RequestPasswordResetRequest>()

                    authService.requestPasswordReset(request).fold(
                        onSuccess = { _ ->
                            // Always return success even if email doesn't exist for security
                            call.respond(
                                MessageResponse(
                                    "If your email is registered with us, you will receive a password reset link."
                                )
                            )
                        },
                        onFailure = { error ->
                            call.application.log.error("Error requesting password reset", error)
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                MessageResponse("An unexpected error occurred")
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.application.log.error("Error processing password reset request", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        MessageResponse("An unexpected error occurred")
                    )
                }
            }

            // Reset password
            post("/reset-password") {
                try {
                    val request = call.receive<ResetPasswordRequest>()

                    authService.resetPassword(request).fold(
                        onSuccess = { success ->
                            if (success) {
                                call.respond(MessageResponse("Password has been reset successfully"))
                            } else {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    MessageResponse("Failed to reset password")
                                )
                            }
                        },
                        onFailure = { error ->
                            call.application.log.error("Error resetting password", error)
                            call.respond(
                                HttpStatusCode.BadRequest,
                                MessageResponse(error.message ?: "Failed to reset password")
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.application.log.error("Error processing password reset", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        MessageResponse("An unexpected error occurred")
                    )
                }
            }

            // Logout (protected route)
            authenticate("auth-jwt") {
                post("/logout") {
                    try {
                        val refreshToken = call.receive<RefreshTokenRequest>().refreshToken

                        authService.logout(refreshToken).fold(
                            onSuccess = { _ ->
                                call.respond(MessageResponse("Logged out successfully"))
                            },
                            onFailure = { error ->
                                when (error) {
                                    is IllegalArgumentException -> call.respond(
                                        HttpStatusCode.BadRequest,
                                        MessageResponse(error.message ?: "Invalid refresh token")
                                    )

                                    else -> {
                                        call.application.log.error("Error during logout", error)
                                        call.respond(
                                            HttpStatusCode.InternalServerError,
                                            MessageResponse("An unexpected error occurred")
                                        )
                                    }
                                }
                            }
                        )
                    } catch (e: Exception) {
                        call.application.log.error("Error processing logout request", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            MessageResponse("An unexpected error occurred")
                        )
                    }
                }

                // Test protected route
                get("/me") {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.getClaim("userId", Int::class)
                        val email = principal?.getClaim("email", String::class)

                        call.respond(
                            mapOf(
                                "userId" to userId,
                                "email" to email
                            )
                        )
                    } catch (e: Exception) {
                        call.application.log.error("Error processing /me request", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            MessageResponse("An unexpected error occurred")
                        )
                    }
                }
            }
        }
    }
} 