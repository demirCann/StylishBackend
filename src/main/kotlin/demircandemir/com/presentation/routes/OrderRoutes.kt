package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.*
import demircandemir.com.domain.model.Order
import demircandemir.com.domain.model.OrderItem
import demircandemir.com.domain.repository.OrderItemRepository
import demircandemir.com.domain.repository.OrderRepository
import demircandemir.com.domain.repository.ProductRepository
import demircandemir.com.domain.repository.UserRepository
import demircandemir.com.infrastructure.persistence.tables.OrderStatus
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

fun Application.orderRoutes(
    orderRepository: OrderRepository,
    orderItemRepository: OrderItemRepository,
    productRepository: ProductRepository,
    userRepository: UserRepository
) {
    routing {
        // Authentication required for all order operations
        authenticate("auth-jwt") {
            route("/api/orders") {
                // GET /api/orders/{id} - Get order by ID with items
                get("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid order ID")

                    // Check if user has permission to access this order
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", Int::class) ?: 0
                    val role = principal?.getClaim("role", String::class) ?: ""

                    val orderResult = orderRepository.findById(id)

                    orderResult.onSuccess getOrderSuccess@{ order ->
                        if (order == null) {
                            call.respond(HttpStatusCode.NotFound, "Order not found")
                            return@getOrderSuccess
                        }

                        // Only the order owner or admin can access
                        if (order.userId != userId && role != "ADMIN") {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                MessageResponse("You don't have permission to access this order")
                            )
                            return@getOrderSuccess
                        }

                        // Get order items
                        val itemsResult = orderItemRepository.findByOrderId(order.id)

                        itemsResult.onSuccess getItemsSuccess@{ items ->
                            call.respond(order.toOrderResponse(items))
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

                // GET /api/orders/user/{userId} - Get all orders for a user
                get("/user/{userId}") {
                    val requestedUserId =
                        call.parameters["userId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid user ID")

                    // Check if user has access permission
                    val principal = call.principal<JWTPrincipal>()
                    val authenticatedUserId = principal?.getClaim("userId", Int::class) ?: 0
                    val role = principal?.getClaim("role", String::class) ?: ""

                    // Users can only access their own orders, admin can access all orders
                    if (requestedUserId != authenticatedUserId && role != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("You don't have permission to access these orders")
                        )
                        return@get
                    }

                    val ordersResult = orderRepository.findByUserId(requestedUserId)

                    ordersResult.onSuccess getUserOrdersSuccess@{ orders ->
                        // We'll return orders without items for the list view
                        call.respond(orders.map { it.toOrderResponse(emptyList()) })
                    }.onFailure { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Failed to retrieve user orders: ${error.message}"
                        )
                    }
                }

                // Admin-only: GET /api/orders/status/{status} - Get orders by status
                get("/status/{status}") {
                    // Only admin users can access this endpoint
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class) ?: ""

                    if (role != "ADMIN") {
                        call.respond(HttpStatusCode.Forbidden, MessageResponse("Admin access required"))
                        return@get
                    }

                    val statusParam =
                        call.parameters["status"] ?: throw IllegalArgumentException("Invalid order status")

                    println("status param: $statusParam")

                    try {
                        // Convert status parameter to enum
                        println("inside try block")

                        val statusEnum = when (statusParam.lowercase()) {
                            "pending" -> OrderStatus.Pending
                            "confirmed" -> OrderStatus.Confirmed
                            "processing" -> OrderStatus.Processing
                            "shipped" -> OrderStatus.Shipped
                            "delivered" -> OrderStatus.Delivered
                            "cancelled" -> OrderStatus.Cancelled
                            "returned" -> OrderStatus.Returned
                            else -> throw IllegalArgumentException("Invalid order status inside try block: $statusParam")
                        }

                        val ordersResult = orderRepository.findByStatus(statusEnum)

                        ordersResult.onSuccess getStatusOrdersSuccess@{ orders ->
                            for (item in orders) {
                                println("inside onSuccess: ${item.id}")
                            }
                            call.respond(orders.map { it.toOrderResponse(emptyList()) })
                        }.onFailure { error ->
                            println("Failed to retrieve status orders inside onFailure block: ${error.message}")
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Failed to retrieve orders: ${error.message}"
                            )
                        }
                    } catch (e: IllegalArgumentException) {
                        println("Invalid order status inside catch block: $statusParam")
                        call.respond(HttpStatusCode.BadRequest, "Invalid order status: $statusParam")
                    }
                }

                // POST /api/orders - Create a new order
                post {
                    val request = call.receive<CreateOrderRequest>()

                    // Check if user has access permission
                    val principal = call.principal<JWTPrincipal>()
                    val authenticatedUserId = principal?.getClaim("userId", Int::class) ?: 0
                    val role = principal?.getClaim("role", String::class) ?: ""

                    // Users can create orders only for themselves, admin can create for anyone
                    if (request.userId != authenticatedUserId && role != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("You don't have permission to create an order for another user")
                        )
                        return@post
                    }

                    // Validate user exists
                    val user = userRepository.getUserById(request.userId)
                    if (user == null) {
                        call.respond(HttpStatusCode.BadRequest, "User not found")
                        return@post
                    }

                    // Validate address exists
                    val address = userRepository.getAddressById(request.addressId)
                    if (address == null) {
                        call.respond(HttpStatusCode.BadRequest, "Address not found")
                        return@post
                    }

                    // Parse shipping fee from string to BigDecimal
                    val shippingFee = try {
                        BigDecimal(request.shippingFee)
                    } catch (e: NumberFormatException) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid shipping fee format")
                        return@post
                    }

                    // Calculate order total
                    var orderTotal = BigDecimal.ZERO
                    val orderItems = mutableListOf<OrderItem>()

                    for (itemRequest in request.items) {
                        val product = productRepository.getProductById(itemRequest.productId)
                        if (product == null) {
                            call.respond(HttpStatusCode.BadRequest, "Product not found: ${itemRequest.productId}")
                            return@post
                        }

                        if (itemRequest.quantity <= 0) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                "Quantity must be positive for product: ${itemRequest.productId}"
                            )
                            return@post
                        }

                        val itemTotal = product.price.multiply(BigDecimal(itemRequest.quantity))
                        orderTotal = orderTotal.add(itemTotal)

                        orderItems.add(
                            OrderItem(
                                orderId = 0, // Will be set after order creation
                                productId = itemRequest.productId,
                                quantity = itemRequest.quantity,
                                unitPrice = product.price,
                                sizeId = itemRequest.sizeId,
                                colorId = itemRequest.colorId
                            )
                        )
                    }

                    if (orderItems.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest, "Order must contain at least one item")
                        return@post
                    }

                    // Add shipping fee
                    val totalWithShipping = orderTotal.add(shippingFee)

                    // Create the order
                    val newOrder = Order(
                        userId = request.userId,
                        addressId = request.addressId,
                        totalAmount = orderTotal,
                        orderDate = LocalDateTime.now(),
                        paymentMethod = request.paymentMethod,
                        shippingFee = totalWithShipping
                    )

                    val createdOrderResult = orderRepository.create(newOrder)

                    createdOrderResult.onSuccess createOrderSuccess@{ createdOrder ->
                        // Update order ID in all items
                        val itemsWithOrderId = orderItems.map { it.copy(orderId = createdOrder.id) }

                        // Save all order items
                        val createdItemsResult = orderItemRepository.createMany(itemsWithOrderId)

                        createdItemsResult.onSuccess createItemsSuccess@{ createdItems ->
                            call.respond(HttpStatusCode.Created, createdOrder.toOrderResponse(createdItems))
                        }.onFailure { error ->
                            // If items creation fails, try to delete the order to maintain consistency
                            orderRepository.delete(createdOrder.id)
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Failed to create order items: ${error.message}"
                            )
                        }
                    }.onFailure { error ->
                        call.respond(HttpStatusCode.InternalServerError, "Failed to create order: ${error.message}")
                    }
                }

                // PUT /api/orders/{id}/status - Update order status (Admin only)
                put("/{id}/status") {
                    // Only admin users can update order status
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class) ?: ""

                    if (role != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("Admin access required to update order status")
                        )
                        return@put
                    }

                    val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid order ID")
                    val request = call.receive<UpdateOrderStatusRequest>()

                    val updateResult = orderRepository.updateStatus(id, request.orderStatus)

                    updateResult.onSuccess updateStatusSuccess@{ updated ->
                        if (updated) {
                            val orderResult = orderRepository.findById(id)

                            orderResult.onSuccess getUpdatedOrderSuccess@{ order ->
                                if (order != null) {
                                    val itemsResult = orderItemRepository.findByOrderId(order.id)

                                    itemsResult.onSuccess getItemsSuccess@{ items ->
                                        call.respond(order.toOrderResponse(items))
                                    }.onFailure { error ->
                                        call.respond(
                                            HttpStatusCode.InternalServerError,
                                            "Failed to retrieve order items: ${error.message}"
                                        )
                                    }
                                } else {
                                    call.respond(HttpStatusCode.NotFound, "Order not found")
                                }
                            }.onFailure { error ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Failed to retrieve updated order: ${error.message}"
                                )
                            }
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Order not found")
                        }
                    }.onFailure { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Failed to update order status: ${error.message}"
                        )
                    }
                }

                // PUT /api/orders/{id}/tracking - Update tracking number (Admin only)
                put("/{id}/tracking") {
                    // Only admin users can update tracking number
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class) ?: ""

                    if (role != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("Admin access required to update tracking information")
                        )
                        return@put
                    }

                    val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid order ID")
                    val request = call.receive<UpdateTrackingRequest>()

                    val updateResult =
                        orderRepository.updateTracking(id, request.trackingNumber, request.shippingProvider)

                    updateResult.onSuccess { updated ->
                        if (updated) {
                            val orderResult = orderRepository.findById(id)

                            orderResult.onSuccess getUpdatedOrderSuccess@{ order ->
                                if (order != null) {
                                    val itemsResult = orderItemRepository.findByOrderId(order.id)

                                    itemsResult.onSuccess getItemsSuccess@{ items ->
                                        call.respond(order.toOrderResponse(items))
                                    }.onFailure { error ->
                                        call.respond(
                                            HttpStatusCode.InternalServerError,
                                            "Failed to retrieve order items: ${error.message}"
                                        )
                                    }
                                } else {
                                    call.respond(HttpStatusCode.NotFound, "Order not found")
                                }
                            }.onFailure { error ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Failed to retrieve updated order: ${error.message}"
                                )
                            }
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Order not found")
                        }
                    }.onFailure { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Failed to update tracking info: ${error.message}"
                        )
                    }
                }

                // Admin only: DELETE /api/orders/{id} - Cancel an order
                delete("/{id}") {
                    // Sadece admin kullanıcılar siparişi silebilir
                    // Only admin users can delete an order
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class) ?: ""

                    if (role != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("Admin access required to delete an order")
                        )
                        return@delete
                    }

                    val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid order ID")

                    // Get the order first to see if it exists
                    val orderResult = orderRepository.findById(id)

                    orderResult.onSuccess { order ->
                        if (order == null) {
                            call.respond(HttpStatusCode.NotFound, "Order not found")
                            return@onSuccess
                        }

                        // Delete order items first
                        val deleteItemsResult = orderItemRepository.deleteByOrderId(id)

                        deleteItemsResult.onSuccess {
                            // Then delete the order
                            val deleteOrderResult = orderRepository.delete(id)

                            deleteOrderResult.onSuccess { deleted ->
                                if (deleted) {
                                    call.respond(HttpStatusCode.NoContent)
                                } else {
                                    call.respond(HttpStatusCode.NotFound, "Order not found")
                                }
                            }.onFailure { error ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Failed to delete order: ${error.message}"
                                )
                            }
                        }.onFailure { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Failed to delete order items: ${error.message}"
                            )
                        }
                    }.onFailure { error ->
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve order: ${error.message}")
                    }
                }
            }
        }
    }
}

// Extension functions
private fun Order.toOrderResponse(items: List<OrderItem>) = OrderResponse(
    id = id,
    userId = userId,
    addressId = addressId,
    totalAmount = totalAmount.toString(),
    orderDate = orderDate.toEpochSecond(ZoneOffset.UTC) * 1000,
    paymentMethod = paymentMethod,
    orderStatus = orderStatus,
    trackingNumber = trackingNumber,
    shippingProvider = shippingProvider,
    shippingFee = shippingFee.toString(),
    items = items.map { it.toOrderItemResponse() }
)

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