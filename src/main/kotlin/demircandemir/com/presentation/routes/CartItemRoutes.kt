package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.AddCartItemRequest
import demircandemir.com.application.dto.CartItemResponse
import demircandemir.com.application.dto.UpdateCartItemQuantityRequest
import demircandemir.com.domain.model.CartItem
import demircandemir.com.domain.repository.CartItemRepository
import demircandemir.com.domain.repository.CartRepository
import demircandemir.com.domain.repository.ProductRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.math.BigDecimal

fun Application.cartItemRoutes(
    cartItemRepository: CartItemRepository,
    cartRepository: CartRepository,
    productRepository: ProductRepository
) {
    routing {
        route("/api/cart-items") {
            // GET /api/cart-items/{id} - Get cart item by ID
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid cart item ID")
                val cartItemResult = cartItemRepository.findById(id)

                cartItemResult.onSuccess { cartItem ->
                    if (cartItem != null) {
                        call.respond(cartItem.toCartItemResponse())
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Cart item not found")
                    }
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart item: ${error.message}")
                }
            }

            // GET /api/cart-items/cart/{cartId} - Get all items in a cart
            get("/cart/{cartId}") {
                val cartId =
                    call.parameters["cartId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid cart ID")
                val cartItemsResult = cartItemRepository.findByCartId(cartId)

                cartItemsResult.onSuccess { cartItems ->
                    call.respond(cartItems.map { it.toCartItemResponse() })
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart items: ${error.message}")
                }
            }

            // POST /api/cart-items - Add item to cart
            post {
                val request = call.receive<AddCartItemRequest>()

                // Check if the product exists
                val product = productRepository.getProductById(request.productId)
                if (product == null) {
                    call.respond(HttpStatusCode.NotFound, "Product not found")
                    return@post
                }

                // Check if cart exists
                val cartResult = cartRepository.findById(request.cartId)

                cartResult.onSuccess cartSuccess@{ cart ->
                    if (cart == null) {
                        call.respond(HttpStatusCode.NotFound, "Cart not found")
                        return@cartSuccess
                    }

                    // Check if the item is already in the cart
                    val existingItemResult = cartItemRepository.findByCartIdAndProductId(
                        request.cartId,
                        request.productId,
                        request.sizeId,
                        request.colorId
                    )

                    existingItemResult.onSuccess itemSuccess@{ existingItem ->
                        if (existingItem != null) {
                            // Update quantity of existing item
                            val newQuantity = existingItem.quantity + request.quantity
                            val updateResult = cartItemRepository.updateQuantity(existingItem.id, newQuantity)

                            updateResult.onSuccess { updatedItem ->
                                // Update cart total
                                val newTotalAmount = cart.totalAmount.add(
                                    product.price.multiply(BigDecimal(request.quantity))
                                )
                                cartRepository.updateTotalAmount(cart.id, newTotalAmount)

                                call.respond(updatedItem.toCartItemResponse())
                            }.onFailure { error ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Failed to update cart item: ${error.message}"
                                )
                            }
                        } else {
                            // Add new item to cart
                            val newCartItem = CartItem(
                                cartId = request.cartId,
                                productId = request.productId,
                                quantity = request.quantity,
                                sizeId = request.sizeId,
                                colorId = request.colorId,
                                unitPrice = product.price
                            )

                            val createResult = cartItemRepository.create(newCartItem)

                            createResult.onSuccess { createdItem ->
                                // Update cart total
                                val newTotalAmount = cart.totalAmount.add(
                                    product.price.multiply(BigDecimal(request.quantity))
                                )
                                cartRepository.updateTotalAmount(cart.id, newTotalAmount)

                                call.respond(HttpStatusCode.Created, createdItem.toCartItemResponse())
                            }.onFailure { error ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Failed to create cart item: ${error.message}"
                                )
                            }
                        }
                    }.onFailure { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Failed to check for existing cart item: ${error.message}"
                        )
                    }
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart: ${error.message}")
                }
            }

            // PUT /api/cart-items/{id}/quantity - Update cart item quantity
            put("/{id}/quantity") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid cart item ID")
                val request = call.receive<UpdateCartItemQuantityRequest>()

                // Validate quantity is positive
                if (request.quantity <= 0) {
                    call.respond(HttpStatusCode.BadRequest, "Quantity must be positive")
                    return@put
                }

                // First get the cart item
                val cartItemResult = cartItemRepository.findById(id)

                cartItemResult.onSuccess itemSuccess@{ cartItem ->
                    if (cartItem == null) {
                        call.respond(HttpStatusCode.NotFound, "Cart item not found")
                        return@itemSuccess
                    }

                    // Get the cart
                    val cartResult = cartRepository.findById(cartItem.cartId)

                    cartResult.onSuccess cartSuccess@{ cart ->
                        if (cart == null) {
                            call.respond(HttpStatusCode.NotFound, "Cart not found")
                            return@cartSuccess
                        }

                        // Calculate difference for cart total update
                        val quantityDifference = request.quantity - cartItem.quantity
                        val totalDifference =
                            cartItem.unitPrice.multiply(BigDecimal.valueOf(quantityDifference.toLong()))

                        // Update cart item quantity
                        val updateResult = cartItemRepository.updateQuantity(id, request.quantity)

                        updateResult.onSuccess { updatedItem ->
                            // Update cart total
                            val newTotalAmount = cart.totalAmount.add(totalDifference)
                            cartRepository.updateTotalAmount(cart.id, newTotalAmount)

                            call.respond(updatedItem.toCartItemResponse())
                        }.onFailure { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Failed to update cart item: ${error.message}"
                            )
                        }
                    }.onFailure { error ->
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart: ${error.message}")
                    }
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart item: ${error.message}")
                }
            }

            // DELETE /api/cart-items/{id} - Remove item from cart
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid cart item ID")

                // First get the cart item
                val cartItemResult = cartItemRepository.findById(id)

                cartItemResult.onSuccess itemSuccess@{ cartItem ->
                    if (cartItem == null) {
                        call.respond(HttpStatusCode.NotFound, "Cart item not found")
                        return@itemSuccess
                    }

                    // Get the cart
                    val cartResult = cartRepository.findById(cartItem.cartId)

                    cartResult.onSuccess cartSuccess@{ cart ->
                        if (cart == null) {
                            call.respond(HttpStatusCode.NotFound, "Cart not found")
                            return@cartSuccess
                        }

                        // Calculate amount to subtract from cart total
                        val subtractAmount = cartItem.unitPrice.multiply(BigDecimal.valueOf(cartItem.quantity.toLong()))

                        // Delete the cart item
                        val deleteResult = cartItemRepository.delete(id)

                        deleteResult.onSuccess { deleted ->
                            if (deleted) {
                                // Update cart total
                                val newTotalAmount = cart.totalAmount.subtract(subtractAmount)
                                cartRepository.updateTotalAmount(cart.id, newTotalAmount.max(BigDecimal.ZERO))

                                call.respond(HttpStatusCode.NoContent)
                            } else {
                                call.respond(HttpStatusCode.NotFound, "Cart item not found")
                            }
                        }.onFailure { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Failed to delete cart item: ${error.message}"
                            )
                        }
                    }.onFailure { error ->
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart: ${error.message}")
                    }
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve cart item: ${error.message}")
                }
            }
        }
    }
}

// Extension functions
private fun CartItem.toCartItemResponse() = CartItemResponse(
    id = id,
    cartId = cartId,
    productId = productId,
    quantity = quantity,
    unitPrice = unitPrice.toString(),
    sizeId = sizeId,
    colorId = colorId,
    subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity.toLong())).toString()
) 