package demircandemir.com.application.dto

import demircandemir.com.infrastructure.persistence.tables.OrderStatus
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    val id: Int,
    val userId: Int,
    val addressId: Int,
    val totalAmount: String,
    val orderDate: Long, // Epoch milliseconds
    val paymentMethod: String,
    val orderStatus: OrderStatus,
    val trackingNumber: String?,
    val shippingProvider: String?,
    val shippingFee: String,
    val items: List<OrderItemResponse>? = null
)

@Serializable
data class CreateOrderRequest(
    val userId: Int,
    val addressId: Int,
    val paymentMethod: String,
    val items: List<CreateOrderItemRequest>,
    val shippingFee: String = "0"
)

@Serializable
data class CreateOrderItemRequest(
    val productId: Int,
    val quantity: Int = 1,
    val sizeId: Int? = null,
    val colorId: Int? = null
)

@Serializable
data class UpdateOrderStatusRequest(
    val orderStatus: OrderStatus
)

@Serializable
data class UpdateTrackingRequest(
    val trackingNumber: String,
    val shippingProvider: String
) 