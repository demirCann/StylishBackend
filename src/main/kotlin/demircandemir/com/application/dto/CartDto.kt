package demircandemir.com.application.dto

import kotlinx.serialization.Serializable

@Serializable
data class CartResponse(
    val id: Int,
    val userId: Int,
    val totalAmount: String,
    val createdAt: Long,
    val updatedAt: Long,
    val itemCount: Int
)

@Serializable
data class CreateCartRequest(
    val userId: Int
)

@Serializable
data class UpdateCartTotalRequest(
    val totalAmount: String
) 