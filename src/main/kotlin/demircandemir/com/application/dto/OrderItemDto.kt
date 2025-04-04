package demircandemir.com.application.dto

import kotlinx.serialization.Serializable

@Serializable
data class OrderItemResponse(
    val id: Int,
    val orderId: Int,
    val productId: Int,
    val quantity: Int,
    val unitPrice: String,
    val sizeId: Int?,
    val colorId: Int?,
    val subtotal: String
)

@Serializable
data class UpdateOrderItemQuantityRequest(
    val quantity: Int
) 