package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.CartResponse
import demircandemir.com.application.dto.CreateCartRequest
import demircandemir.com.application.dto.UpdateCartTotalRequest
import demircandemir.com.domain.model.Cart
import demircandemir.com.domain.repository.CartItemRepository
import demircandemir.com.domain.repository.CartRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

fun Application.cartRoutes(cartRepository: CartRepository, cartItemRepository: CartItemRepository) {
    routing {
        route("/api/carts") {
            // GET /api/carts/{id} - Get cart by ID
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid cart ID")
                val cartResult = cartRepository.findById(id)

                cartResult.onSuccess { cart ->
                    if (cart != null) {
                        // Get item count for this cart
                        val itemCountResult = cartItemRepository.countByCartId(cart.id)
                        val itemCount = itemCountResult.getOrDefault(0)

                        call.respond(cart.toCartResponse(itemCount))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Cart not found")
                    }
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart: ${error.message}")
                }
            }

            // GET /api/carts/user/{userId} - Get cart by user ID
            get("/user/{userId}") {
                val userId =
                    call.parameters["userId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid user ID")
                val cartResult = cartRepository.findByUserId(userId)

                cartResult.onSuccess { cart ->
                    if (cart != null) {
                        // Get item count for this cart
                        val itemCountResult = cartItemRepository.countByCartId(cart.id)
                        val itemCount = itemCountResult.getOrDefault(0)

                        call.respond(cart.toCartResponse(itemCount))
                    } else {
                        // Create a new cart for this user
                        val newCart = Cart(
                            userId = userId,
                            totalAmount = BigDecimal.ZERO,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                        )

                        val createdCartResult = cartRepository.create(newCart)

                        createdCartResult.onSuccess { createdCart ->
                            call.respond(HttpStatusCode.Created, createdCart.toCartResponse(0))
                        }.onFailure { error ->
                            call.respond(HttpStatusCode.InternalServerError, "Failed to create cart: ${error.message}")
                        }
                    }
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart: ${error.message}")
                }
            }

            // POST /api/carts - Create a new cart
            post {
                val request = call.receive<CreateCartRequest>()

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
                            call.respond(HttpStatusCode.InternalServerError, "Failed to create cart: ${error.message}")
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

                val updateResult = cartRepository.updateTotalAmount(id, BigDecimal(request.totalAmount))

                updateResult.onSuccess { updatedCart ->
                    // Get item count for this cart
                    val itemCountResult = cartItemRepository.countByCartId(updatedCart.id)
                    val itemCount = itemCountResult.getOrDefault(0)

                    call.respond(updatedCart.toCartResponse(itemCount))
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to update cart total: ${error.message}")
                }
            }

            // DELETE /api/carts/{id} - Delete a cart
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid cart ID")

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
                        call.respond(HttpStatusCode.InternalServerError, "Failed to delete cart: ${error.message}")
                    }
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to delete cart items: ${error.message}")
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