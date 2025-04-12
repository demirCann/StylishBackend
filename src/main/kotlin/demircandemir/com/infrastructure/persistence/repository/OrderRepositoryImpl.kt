package demircandemir.com.infrastructure.persistence.repository

import demircandemir.com.domain.model.Order
import demircandemir.com.domain.repository.OrderRepository
import demircandemir.com.infrastructure.persistence.DatabaseFactory.dbQuery
import demircandemir.com.infrastructure.persistence.tables.OrderStatus
import demircandemir.com.infrastructure.persistence.tables.Orders
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class OrderRepositoryImpl : OrderRepository {

    private fun ResultRow.toOrder(): Order {
        return Order(
            id = this[Orders.id].value,
            userId = this[Orders.userId].value,
            addressId = this[Orders.addressId].value,
            totalAmount = this[Orders.totalAmount],
            orderDate = this[Orders.orderDate],
            paymentMethod = this[Orders.paymentMethod],
            orderStatus = this[Orders.orderStatus],
            trackingNumber = this[Orders.trackingNumber],
            shippingProvider = this[Orders.shippingProvider],
            shippingFee = this[Orders.shippingFee]
        )
    }

    override suspend fun create(order: Order): Result<Order> = dbQuery {
        try {
            val insertStatement = Orders.insert {
                it[userId] = order.userId
                it[addressId] = order.addressId
                it[totalAmount] = order.totalAmount
                it[orderDate] = order.orderDate
                it[paymentMethod] = order.paymentMethod
                it[orderStatus] = order.orderStatus
                it[trackingNumber] = order.trackingNumber
                it[shippingProvider] = order.shippingProvider
                it[shippingFee] = order.shippingFee
            }

            val resultRow = insertStatement.resultedValues?.first()
                ?: return@dbQuery Result.failure(Exception("Failed to insert order"))

            Result.success(resultRow.toOrder())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findById(id: Int): Result<Order?> = dbQuery {
        try {
            val order = Orders
                .selectAll()
                .where { Orders.id eq id }
                .map { it.toOrder() }
                .singleOrNull()

            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findByUserId(userId: Int): Result<List<Order>> = dbQuery {
        try {
            val orders = Orders
                .selectAll()
                .where { Orders.userId eq userId }
                .map { it.toOrder() }

            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun update(order: Order): Result<Order> = dbQuery {
        try {
            Orders.update({ Orders.id eq order.id }) {
                it[userId] = order.userId
                it[addressId] = order.addressId
                it[totalAmount] = order.totalAmount
                it[orderDate] = order.orderDate
                it[paymentMethod] = order.paymentMethod
                it[orderStatus] = order.orderStatus
                it[trackingNumber] = order.trackingNumber
                it[shippingProvider] = order.shippingProvider
                it[shippingFee] = order.shippingFee
            }

            val updatedOrder = Orders
                .selectAll()
                .where { Orders.id eq order.id }
                .map { it.toOrder() }
                .singleOrNull()
                ?: return@dbQuery Result.failure(Exception("Failed to retrieve updated order"))

            Result.success(updatedOrder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStatus(orderId: Int, status: OrderStatus): Result<Boolean> = dbQuery {
        try {
            val updatedRows = Orders.update({ Orders.id eq orderId }) {
                it[orderStatus] = status
            }

            Result.success(updatedRows > 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTracking(
        orderId: Int,
        trackingNumber: String,
        shippingProvider: String
    ): Result<Boolean> = dbQuery {
        try {
            val updatedRows = Orders.update({ Orders.id eq orderId }) {
                it[Orders.trackingNumber] = trackingNumber
                it[Orders.shippingProvider] = shippingProvider
            }

            Result.success(updatedRows > 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(id: Int): Result<Boolean> = dbQuery {
        try {
            val deletedRows = Orders.deleteWhere { Orders.id eq id }
            Result.success(deletedRows > 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findByStatus(status: OrderStatus): Result<List<Order>> = dbQuery {
        try {
            val orders = Orders
                .selectAll()
                .where { Orders.orderStatus eq status }
                .map { it.toOrder() }

            Result.success(orders)
        } catch (e: Exception) {
            println("Failed to retrieve order inside repository: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun findByTrackingNumber(trackingNumber: String): Result<Order?> = dbQuery {
        try {
            val order = Orders
                .selectAll()
                .where { Orders.trackingNumber eq trackingNumber }
                .map { it.toOrder() }
                .singleOrNull()

            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 