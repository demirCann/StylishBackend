package demircandemir.com.application.dto

import kotlinx.serialization.Serializable

@Serializable
data class CartItemResponse(
    val id: Int,
    val cartId: Int,
    val productId: Int,
    val quantity: Int,
    val unitPrice: String,
    val sizeId: Int?,
    val colorId: Int?,
    val subtotal: String
)

@Serializable
data class AddCartItemRequest(
    val cartId: Int,
    val productId: Int,
    val quantity: Int = 1,
    val sizeId: Int? = null,
    val colorId: Int? = null
)

@Serializable
data class UpdateCartItemQuantityRequest(
    val quantity: Int
) 