package demircandemir.com.domain.model

import java.math.BigDecimal

data class OrderItem(
    val id: Int = 0,
    val orderId: Int,
    val productId: Int,
    val quantity: Int = 1,
    val unitPrice: BigDecimal,
    val sizeId: Int? = null,
    val colorId: Int? = null
) {
    init {
        require(quantity > 0) { "Quantity must be positive" }
        require(unitPrice >= BigDecimal.ZERO) { "Price cannot be negative" }
    }
} 