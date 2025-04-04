package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.OrderItemResponse
import demircandemir.com.application.dto.UpdateOrderItemQuantityRequest
import demircandemir.com.domain.model.OrderItem
import demircandemir.com.domain.repository.OrderItemRepository
import demircandemir.com.domain.repository.OrderRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.math.BigDecimal

fun Application.orderItemRoutes(
    orderItemRepository: OrderItemRepository,
    orderRepository: OrderRepository
) {
    routing {
        route("/api/order-items") {
            // GET /api/order-items/{id} - Get order item by ID
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid order item ID")
                val orderItemResult = orderItemRepository.findById(id)

                orderItemResult.onSuccess getItemSuccess@{ orderItem ->
                    if (orderItem != null) {
                        call.respond(orderItem.toOrderItemResponse())
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Order item not found")
                    }
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve order item: ${error.message}")
                }
            }

            // GET /api/order-items/order/{orderId} - Get all items for an order
            get("/order/{orderId}") {
                val orderId =
                    call.parameters["orderId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid order ID")

                // Check if order exists
                val orderResult = orderRepository.findById(orderId)

                orderResult.onSuccess checkOrderSuccess@{ order ->
                    if (order == null) {
                        call.respond(HttpStatusCode.NotFound, "Order not found")
                        return@checkOrderSuccess
                    }

                    val orderItemsResult = orderItemRepository.findByOrderId(orderId)

                    orderItemsResult.onSuccess getItemsSuccess@{ orderItems ->
                        call.respond(orderItems.map { it.toOrderItemResponse() })
                    }.onFailure { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Failed to retrieve order items: ${error.message}"
                        )
                    }
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve order: ${error.message}")
                }
            }

            // PUT /api/order-items/{id}/quantity - Update order item quantity
            put("/{id}/quantity") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid order item ID")
                val request = call.receive<UpdateOrderItemQuantityRequest>()

                if (request.quantity <= 0) {
                    call.respond(HttpStatusCode.BadRequest, "Quantity must be positive")
                    return@put
                }

                // First get the order item
                val orderItemResult = orderItemRepository.findById(id)

                orderItemResult.onSuccess getItemSuccess@{ orderItem ->
                    if (orderItem == null) {
                        call.respond(HttpStatusCode.NotFound, "Order item not found")
                        return@getItemSuccess
                    }

                    // Get the order to update total amount later
                    val orderResult = orderRepository.findById(orderItem.orderId)

                    orderResult.onSuccess getOrderSuccess@{ order ->
                        if (order == null) {
                            call.respond(HttpStatusCode.NotFound, "Order not found")
                            return@getOrderSuccess
                        }

                        // Calculate difference for order total update
                        val quantityDifference = request.quantity - orderItem.quantity
                        val priceDifference =
                            orderItem.unitPrice.multiply(BigDecimal.valueOf(quantityDifference.toLong()))

                        // Update order item quantity
                        val updateResult = orderItemRepository.updateQuantity(id, request.quantity)

                        updateResult.onSuccess updateItemSuccess@{ updatedItem ->
                            // Update order total amount
                            val newTotalAmount = order.totalAmount.add(priceDifference)
                            val updatedOrder = order.copy(totalAmount = newTotalAmount)
                            orderRepository.update(updatedOrder)

                            call.respond(updatedItem.toOrderItemResponse())
                        }.onFailure { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Failed to update order item: ${error.message}"
                            )
                        }
                    }.onFailure { error ->
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve order: ${error.message}")
                    }
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve order item: ${error.message}")
                }
            }

            // DELETE /api/order-items/{id} - Delete an order item (typically just for admin use)
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid order item ID")

                // First get the order item
                val orderItemResult = orderItemRepository.findById(id)

                orderItemResult.onSuccess getItemSuccess@{ orderItem ->
                    if (orderItem == null) {
                        call.respond(HttpStatusCode.NotFound, "Order item not found")
                        return@getItemSuccess
                    }

                    // Get the order to update total amount
                    val orderResult = orderRepository.findById(orderItem.orderId)

                    orderResult.onSuccess getOrderSuccess@{ order ->
                        if (order == null) {
                            call.respond(HttpStatusCode.NotFound, "Order not found")
                            return@getOrderSuccess
                        }

                        // Calculate amount to subtract from order total
                        val subtractAmount =
                            orderItem.unitPrice.multiply(BigDecimal.valueOf(orderItem.quantity.toLong()))

                        // Delete the order item
                        val deleteResult = orderItemRepository.delete(id)

                        deleteResult.onSuccess deleteItemSuccess@{ deleted ->
                            if (deleted) {
                                // Update order total
                                val newTotalAmount = order.totalAmount.subtract(subtractAmount)
                                val updatedOrder = order.copy(totalAmount = newTotalAmount.max(BigDecimal.ZERO))
                                orderRepository.update(updatedOrder)

                                call.respond(HttpStatusCode.NoContent)
                            } else {
                                call.respond(HttpStatusCode.NotFound, "Order item not found")
                            }
                        }.onFailure { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Failed to delete order item: ${error.message}"
                            )
                        }
                    }.onFailure { error ->
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve order: ${error.message}")
                    }
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve order item: ${error.message}")
                }
            }
        }
    }
}

// Extension function
private fun OrderItem.toOrderItemResponse() = OrderItemResponse(
    id = id,
    orderId = orderId,
    productId = productId,
    quantity = quantity,
    unitPrice = unitPrice.toString(),
    sizeId = sizeId,
    colorId = colorId,
    subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity.toLong())).toString()
) 