package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.MessageResponse
import demircandemir.com.application.dto.UpdateUserRequest
import demircandemir.com.application.dto.UserResponse
import demircandemir.com.domain.model.User
import demircandemir.com.domain.model.UserRole
import demircandemir.com.domain.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.ZoneOffset

fun Application.userRoutes(userRepository: UserRepository) {
    routing {
        route("/api/users") {
            // Admin-only route to view all users
            authenticate("auth-jwt") {
                get {
                    // Admin permission check
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class) ?: ""

                    if (role != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("Admin access required to view all users")
                        )
                        return@get
                    }

                    val users = userRepository.getAllUsers()
                    val userResponses = users.map { it.toUserResponse() }
                    call.respond(userResponses)
                }

                // Get user by ID - own profile or admin access
                get("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID format")

                    // Verify user identity with JWT
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)?.toIntOrNull()
                    val role = principal?.getClaim("role", String::class) ?: ""

                    // Users can only view their own profile, admin can view all profiles
                    if (userId != id && role != "ADMIN") {
                        call.respond(HttpStatusCode.Forbidden, MessageResponse("You can only view your own profile"))
                        return@get
                    }

                    val user = userRepository.getUserById(id)
                        ?: return@get call.respond(HttpStatusCode.NotFound, "User not found")

                    call.respond(user.toUserResponse())
                }

                // Update user - own profile or admin access
                put("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID format")

                    // Verify user identity with JWT
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)?.toIntOrNull()
                    val role = principal?.getClaim("role", String::class) ?: ""

                    // Users can only update their own profile, admin can update all profiles
                    if (userId != id && role != "ADMIN") {
                        call.respond(HttpStatusCode.Forbidden, MessageResponse("You can only update your own profile"))
                        return@put
                    }

                    val existingUser = userRepository.getUserById(id)
                        ?: return@put call.respond(HttpStatusCode.NotFound, "User not found")

                    val request = call.receive<UpdateUserRequest>()

                    // Non-admin users cannot change roles - role is not in UpdateUserRequest anyway
                    
                    val updatedUser = existingUser.copy(
                        firstName = request.firstName ?: existingUser.firstName,
                        lastName = request.lastName ?: existingUser.lastName,
                        phoneNumber = request.phoneNumber ?: existingUser.phoneNumber
                    )

                    val result = userRepository.updateUser(updatedUser)
                    call.respond(result.toUserResponse())
                }

                // Admin-only: Delete user
                delete("/{id}") {
                    // Admin permission check
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class) ?: ""
                    val userId = principal?.getClaim("userId", String::class)?.toIntOrNull()

                    if (role != "ADMIN") {
                        call.respond(HttpStatusCode.Forbidden, MessageResponse("Admin access required to delete users"))
                        return@delete
                    }

                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID format")

                    // Admin cannot delete their own account
                    if (userId == id) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("You cannot delete your own admin account")
                        )
                        return@delete
                    }

                    userRepository.deleteUser(id)
                    call.respond(HttpStatusCode.NoContent)
                }

                // Admin-only: Change user role
                put("/{id}/role") {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID format")

                    // Admin permission check
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class) ?: ""
                    val adminId = principal?.getClaim("userId", String::class)?.toIntOrNull()

                    if (role != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("Admin access required to change user roles")
                        )
                        return@put
                    }

                    // Admin cannot change their own role
                    if (adminId == id) {
                        call.respond(HttpStatusCode.Forbidden, MessageResponse("You cannot change your own role"))
                        return@put
                    }

                    // Get role information
                    val request = call.receive<Map<String, String>>()
                    val newRoleStr = request["role"] ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        MessageResponse("Role is required")
                    )

                    // Convert from String to UserRole
                    val newRole = try {
                        UserRole.valueOf(newRoleStr)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            MessageResponse("Invalid role. Must be CUSTOMER or ADMIN")
                        )
                        return@put
                    }

                    val existingUser = userRepository.getUserById(id)
                        ?: return@put call.respond(HttpStatusCode.NotFound, "User not found")

                    val updatedUser = existingUser.copy(role = newRole)
                    val result = userRepository.updateUser(updatedUser)

                    call.respond(
                        HttpStatusCode.OK,
                        MessageResponse("User role updated to $newRole successfully")
                    )
                }
            }
        }
    }
}

// Extension function to convert User to UserResponse
private fun User.toUserResponse() = UserResponse(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    phoneNumber = phoneNumber,
    role = role.name,
    status = status.name,
    createdAt = registrationDate.toEpochSecond(ZoneOffset.UTC) * 1000,
    updatedAt = registrationDate.toEpochSecond(ZoneOffset.UTC) * 1000
) 