package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.CreateUserRequest
import demircandemir.com.application.dto.UpdateUserRequest
import demircandemir.com.application.dto.UserResponse
import demircandemir.com.domain.model.User
import demircandemir.com.domain.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime

fun Application.userRoutes(userRepository: UserRepository) {
    routing {
        route("/users") {
            get {
                val users = userRepository.getAllUsers()
                call.respond(users.map { it.toUserResponse() })
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
                val user = userRepository.getUserById(id)
                if (user != null) {
                    call.respond(user.toUserResponse())
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            }

            post {
                val request = call.receive<CreateUserRequest>()
                val now = System.currentTimeMillis()
                val user = User(
                    id = 0, // ID will be assigned by the database
                    email = request.email,
                    password = request.password, // In production, hash the password
                    firstName = request.firstName,
                    lastName = request.lastName,
                    phoneNumber = request.phoneNumber,
                    registrationDate = LocalDateTime.now(),
                    isActive = true
                )
                val createdUser = userRepository.createUser(user)
                call.respond(HttpStatusCode.Created, createdUser.toUserResponse())
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
                val request = call.receive<UpdateUserRequest>()
                val existingUser = userRepository.getUserById(id)
                
                if (existingUser != null) {
                    val updatedUser = existingUser.copy(
                        firstName = request.firstName ?: existingUser.firstName,
                        lastName = request.lastName ?: existingUser.lastName,
                        phoneNumber = request.phoneNumber ?: existingUser.phoneNumber
                    )
                    val savedUser = userRepository.updateUser(updatedUser)
                    call.respond(savedUser.toUserResponse())
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
                userRepository.deleteUser(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun User.toUserResponse() = UserResponse(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    phoneNumber = phoneNumber,
    createdAt = registrationDate.toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
    updatedAt = registrationDate.toEpochSecond(java.time.ZoneOffset.UTC) * 1000
) 