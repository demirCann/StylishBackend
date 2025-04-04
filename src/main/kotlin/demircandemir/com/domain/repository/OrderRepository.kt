package demircandemir.com.domain.repository

import demircandemir.com.domain.model.Order
import demircandemir.com.infrastructure.persistence.tables.OrderStatus

interface OrderRepository {
    suspend fun create(order: Order): Result<Order>
    suspend fun findById(id: Int): Result<Order?>
    suspend fun findByUserId(userId: Int): Result<List<Order>>
    suspend fun update(order: Order): Result<Order>
    suspend fun updateStatus(orderId: Int, status: OrderStatus): Result<Boolean>
    suspend fun delete(id: Int): Result<Boolean>
    suspend fun findByStatus(status: OrderStatus): Result<List<Order>>
    suspend fun findByTrackingNumber(trackingNumber: String): Result<Order?>
} 