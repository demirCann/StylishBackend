package demircandemir.com.domain.repository

import demircandemir.com.domain.model.CartItem

interface CartItemRepository {
    suspend fun create(cartItem: CartItem): Result<CartItem>
    suspend fun findById(id: Int): Result<CartItem?>
    suspend fun findByCartId(cartId: Int): Result<List<CartItem>>
    suspend fun findByCartIdAndProductId(
        cartId: Int,
        productId: Int,
        sizeId: Int? = null,
        colorId: Int? = null
    ): Result<CartItem?>

    suspend fun update(cartItem: CartItem): Result<CartItem>
    suspend fun updateQuantity(id: Int, newQuantity: Int): Result<CartItem>
    suspend fun delete(id: Int): Result<Boolean>
    suspend fun deleteByCartId(cartId: Int): Result<Int> // Returns number of deleted items
    suspend fun countByCartId(cartId: Int): Result<Int> // Count items in a cart
} 