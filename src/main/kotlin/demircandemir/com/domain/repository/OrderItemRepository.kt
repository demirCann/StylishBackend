package demircandemir.com.domain.repository

import demircandemir.com.domain.model.OrderItem

interface OrderItemRepository {
    suspend fun create(orderItem: OrderItem): Result<OrderItem>
    suspend fun createMany(orderItems: List<OrderItem>): Result<List<OrderItem>>
    suspend fun findById(id: Int): Result<OrderItem?>
    suspend fun findByOrderId(orderId: Int): Result<List<OrderItem>>
    suspend fun update(orderItem: OrderItem): Result<OrderItem>
    suspend fun updateQuantity(id: Int, newQuantity: Int): Result<OrderItem>
    suspend fun delete(id: Int): Result<Boolean>
    suspend fun deleteByOrderId(orderId: Int): Result<Int> // Returns number of deleted items
} 