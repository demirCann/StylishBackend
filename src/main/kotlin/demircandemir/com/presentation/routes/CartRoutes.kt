package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.CartResponse
import demircandemir.com.application.dto.CreateCartRequest
import demircandemir.com.application.dto.MessageResponse
import demircandemir.com.application.dto.UpdateCartTotalRequest
import demircandemir.com.domain.model.Cart
import demircandemir.com.domain.repository.CartItemRepository
import demircandemir.com.domain.repository.CartRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

fun Application.cartRoutes(cartRepository: CartRepository, cartItemRepository: CartItemRepository) {
    routing {
        // Authentication required for all cart operations
        authenticate("auth-jwt") {
            route("/api/carts") {
                // GET /api/carts/{id} - Get cart by ID
                get("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid cart ID")

                    // Check if user has permission to access this cart
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", Int::class) ?: 0
                    val role = principal?.getClaim("role", String::class) ?: ""

                    val cartResult = cartRepository.findById(id)

                    cartResult.onSuccess { cart ->
                        if (cart != null) {
                            // Only the cart owner or admin can access
                            if (cart.userId == userId || role == "ADMIN") {
                                // Get item count for this cart
                                val itemCountResult = cartItemRepository.countByCartId(cart.id)
                                val itemCount = itemCountResult.getOrDefault(0)

                                call.respond(cart.toCartResponse(itemCount))
                            } else {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    MessageResponse("You don't have permission to access this cart")
                                )
                            }
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Cart not found")
                        }
                    }.onFailure { error ->
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart: ${error.message}")
                    }
                }

                // GET /api/carts/user/{userId} - Get cart by user ID
                get("/user/{userId}") {
                    val requestedUserId =
                        call.parameters["userId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid user ID")

                    // Check if user has access permission
                    val principal = call.principal<JWTPrincipal>()
                    val authenticatedUserId = principal?.getClaim("userId", Int::class) ?: 0
                    val role = principal?.getClaim("role", String::class) ?: ""

                    // Users can only access their own cart, admin can access all carts
                    if (requestedUserId != authenticatedUserId && role != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("You don't have permission to access this cart")
                        )
                        return@get
                    }

                    val cartResult = cartRepository.findByUserId(requestedUserId)

                    cartResult.onSuccess { cart ->
                        if (cart != null) {
                            // Get item count for this cart
                            val itemCountResult = cartItemRepository.countByCartId(cart.id)
                            val itemCount = itemCountResult.getOrDefault(0)

                            call.respond(cart.toCartResponse(itemCount))
                        } else {
                            // Create a new cart for this user
                            val newCart = Cart(
                                userId = requestedUserId,
                                totalAmount = BigDecimal.ZERO,
                                createdAt = LocalDateTime.now(),
                                updatedAt = LocalDateTime.now()
                            )

                            val createdCartResult = cartRepository.create(newCart)

                            createdCartResult.onSuccess { createdCart ->
                                call.respond(HttpStatusCode.Created, createdCart.toCartResponse(0))
                            }.onFailure { error ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Failed to create cart: ${error.message}"
                                )
                            }
                        }
                    }.onFailure { error ->
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart: ${error.message}")
                    }
                }

                // POST /api/carts - Create a new cart
                post {
                    val request = call.receive<CreateCartRequest>()

                    // Check if user has access permission
                    val principal = call.principal<JWTPrincipal>()
                    val authenticatedUserId = principal?.getClaim("userId", Int::class) ?: 0
                    val role = principal?.getClaim("role", String::class) ?: ""

                    // Users can create carts only for themselves, admin can create for anyone
                    if (request.userId != authenticatedUserId && role != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("You don't have permission to create a cart for another user")
                        )
                        return@post
                    }

                    // Check if user already has a cart
                    val existingCartResult = cartRepository.findByUserId(request.userId)

                    existingCartResult.onSuccess { existingCart ->
                        if (existingCart != null) {
                            // User already has a cart
                            call.respond(HttpStatusCode.Conflict, "User already has a cart")
                        } else {
                            // Create a new cart
                            val newCart = Cart(
                                userId = request.userId,
                                totalAmount = BigDecimal.ZERO,
                                createdAt = LocalDateTime.now(),
                                updatedAt = LocalDateTime.now()
                            )

                            val createdCartResult = cartRepository.create(newCart)

                            createdCartResult.onSuccess { createdCart ->
                                call.respond(HttpStatusCode.Created, createdCart.toCartResponse(0))
                            }.onFailure { error ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Failed to create cart: ${error.message}"
                                )
                            }
                        }
                    }.onFailure { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Failed to check for existing cart: ${error.message}"
                        )
                    }
                }

                // PUT /api/carts/{id}/total - Update cart total
                put("/{id}/total") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid cart ID")
                    val request = call.receive<UpdateCartTotalRequest>()

                    // Check if user has permission to access this cart
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", Int::class) ?: 0
                    val role = principal?.getClaim("role", String::class) ?: ""

                    val cartResult = cartRepository.findById(id)

                    cartResult.onSuccess { cart ->
                        if (cart == null) {
                            call.respond(HttpStatusCode.NotFound, "Cart not found")
                            return@onSuccess
                        }

                        // Only the cart owner or admin can update
                        if (cart.userId != userId && role != "ADMIN") {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                MessageResponse("You don't have permission to update this cart")
                            )
                            return@onSuccess
                        }

                        val updateResult = cartRepository.updateTotalAmount(id, BigDecimal(request.totalAmount))

                        updateResult.onSuccess { updatedCart ->
                            // Get item count for this cart
                            val itemCountResult = cartItemRepository.countByCartId(updatedCart.id)
                            val itemCount = itemCountResult.getOrDefault(0)

                            call.respond(updatedCart.toCartResponse(itemCount))
                        }.onFailure { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Failed to update cart total: ${error.message}"
                            )
                        }
                    }.onFailure { error ->
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart: ${error.message}")
                    }
                }

                // DELETE /api/carts/{id} - Delete a cart
                delete("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid cart ID")

                    // Check if user has permission to access this cart
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", Int::class) ?: 0
                    val role = principal?.getClaim("role", String::class) ?: ""

                    // Önce cart'ı bul ve yetki kontrolü yap
                    val cartResult = cartRepository.findById(id)

                    cartResult.onSuccess { cart ->
                        if (cart == null) {
                            call.respond(HttpStatusCode.NotFound, "Cart not found")
                            return@onSuccess
                        }

                        // Only the cart owner or admin can delete
                        if (cart.userId != userId && role != "ADMIN") {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                MessageResponse("You don't have permission to delete this cart")
                            )
                            return@onSuccess
                        }

                        // First delete all cart items
                        val deleteItemsResult = cartItemRepository.deleteByCartId(id)

                        deleteItemsResult.onSuccess {
                            // Then delete the cart
                            val deleteCartResult = cartRepository.delete(id)

                            deleteCartResult.onSuccess { deleted ->
                                if (deleted) {
                                    call.respond(HttpStatusCode.NoContent)
                                } else {
                                    call.respond(HttpStatusCode.NotFound, "Cart not found")
                                }
                            }.onFailure { error ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Failed to delete cart: ${error.message}"
                                )
                            }
                        }.onFailure { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Failed to delete cart items: ${error.message}"
                            )
                        }
                    }.onFailure { error ->
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart: ${error.message}")
                    }
                }
            }
        }
    }
}

// Extension functions
private fun Cart.toCartResponse(itemCount: Int) = CartResponse(
    id = id,
    userId = userId,
    totalAmount = totalAmount.toString(),
    createdAt = createdAt.toEpochSecond(ZoneOffset.UTC) * 1000,
    updatedAt = updatedAt.toEpochSecond(ZoneOffset.UTC) * 1000,
    itemCount = itemCount
) 