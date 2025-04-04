package demircandemir.com.domain.model

import java.math.BigDecimal

data class CartItem(
    val id: Int = 0,
    val cartId: Int,
    val productId: Int,
    val quantity: Int = 1,
    val sizeId: Int? = null,
    val colorId: Int? = null,
    val unitPrice: BigDecimal
) {
    init {
        require(quantity > 0) { "Quantity must be positive" }
        require(unitPrice >= BigDecimal.ZERO) { "Unit price cannot be negative" }
    }

    fun updateQuantity(newQuantity: Int): CartItem {
        require(newQuantity > 0) { "New quantity must be positive" }
        return this.copy(quantity = newQuantity)
    }
} 