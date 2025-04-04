package demircandemir.com.domain.repository

import demircandemir.com.domain.model.Cart
import java.math.BigDecimal

interface CartRepository {
    suspend fun create(cart: Cart): Result<Cart>
    suspend fun findById(id: Int): Result<Cart?>
    suspend fun findByUserId(userId: Int): Result<Cart?>
    suspend fun update(cart: Cart): Result<Cart>
    suspend fun updateTotalAmount(cartId: Int, newAmount: BigDecimal): Result<Cart>
    suspend fun touch(cartId: Int): Result<Boolean> // Updates the updatedAt timestamp
    suspend fun delete(id: Int): Result<Boolean>
    suspend fun deleteByUserId(userId: Int): Result<Boolean>
} 