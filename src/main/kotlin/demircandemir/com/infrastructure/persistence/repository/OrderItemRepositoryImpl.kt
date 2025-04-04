package demircandemir.com.infrastructure.persistence.repository

import demircandemir.com.domain.model.OrderItem
import demircandemir.com.domain.repository.OrderItemRepository
import demircandemir.com.infrastructure.persistence.DatabaseFactory.dbQuery
import demircandemir.com.infrastructure.persistence.tables.OrderItems
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class OrderItemRepositoryImpl : OrderItemRepository {

    private fun ResultRow.toOrderItem(): OrderItem {
        return OrderItem(
            id = this[OrderItems.id].value,
            orderId = this[OrderItems.orderId].value,
            productId = this[OrderItems.productId].value,
            quantity = this[OrderItems.quantity],
            unitPrice = this[OrderItems.unitPrice],
            sizeId = this[OrderItems.sizeId]?.value,
            colorId = this[OrderItems.colorId]?.value
        )
    }

    override suspend fun create(orderItem: OrderItem): Result<OrderItem> = dbQuery {
        try {
            val insertStatement = OrderItems.insert {
                it[orderId] = orderItem.orderId
                it[productId] = orderItem.productId
                it[quantity] = orderItem.quantity
                it[unitPrice] = orderItem.unitPrice
                it[sizeId] = orderItem.sizeId
                it[colorId] = orderItem.colorId
            }

            val resultRow = insertStatement.resultedValues?.first()
                ?: return@dbQuery Result.failure(Exception("Failed to insert order item"))

            Result.success(resultRow.toOrderItem())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createMany(orderItems: List<OrderItem>): Result<List<OrderItem>> = dbQuery {
        try {
            val insertedItems = mutableListOf<OrderItem>()

            transaction {
                orderItems.forEach { orderItem ->
                    val insertStatement = OrderItems.insert {
                        it[orderId] = orderItem.orderId
                        it[productId] = orderItem.productId
                        it[quantity] = orderItem.quantity
                        it[unitPrice] = orderItem.unitPrice
                        it[sizeId] = orderItem.sizeId
                        it[colorId] = orderItem.colorId
                    }

                    insertStatement.resultedValues?.first()?.let { row ->
                        insertedItems.add(row.toOrderItem())
                    }
                }
            }

            if (insertedItems.size != orderItems.size) {
                return@dbQuery Result.failure(Exception("Failed to insert all order items"))
            }

            Result.success(insertedItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findById(id: Int): Result<OrderItem?> = dbQuery {
        try {
            val orderItem = OrderItems
                .selectAll()
                .where { OrderItems.id eq id }
                .map { it.toOrderItem() }
                .singleOrNull()

            Result.success(orderItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findByOrderId(orderId: Int): Result<List<OrderItem>> = dbQuery {
        try {
            val orderItems = OrderItems
                .selectAll()
                .where { OrderItems.orderId eq orderId }
                .map { it.toOrderItem() }

            Result.success(orderItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun update(orderItem: OrderItem): Result<OrderItem> = dbQuery {
        try {
            OrderItems.update({ OrderItems.id eq orderItem.id }) {
                it[orderId] = orderItem.orderId
                it[productId] = orderItem.productId
                it[quantity] = orderItem.quantity
                it[unitPrice] = orderItem.unitPrice
                it[sizeId] = orderItem.sizeId
                it[colorId] = orderItem.colorId
            }

            val updatedOrderItem = OrderItems
                .selectAll()
                .where { OrderItems.id eq orderItem.id }
                .map { it.toOrderItem() }
                .singleOrNull()
                ?: return@dbQuery Result.failure(Exception("Failed to retrieve updated order item"))

            Result.success(updatedOrderItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateQuantity(id: Int, newQuantity: Int): Result<OrderItem> = dbQuery {
        try {
            require(newQuantity > 0) { "New quantity must be positive" }

            OrderItems.update({ OrderItems.id eq id }) {
                it[quantity] = newQuantity
            }

            val updatedOrderItem = OrderItems
                .selectAll()
                .where { OrderItems.id eq id }
                .map { it.toOrderItem() }
                .singleOrNull()
                ?: return@dbQuery Result.failure(Exception("Failed to retrieve updated order item"))

            Result.success(updatedOrderItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(id: Int): Result<Boolean> = dbQuery {
        try {
            val deletedRows = OrderItems.deleteWhere { OrderItems.id eq id }
            Result.success(deletedRows > 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteByOrderId(orderId: Int): Result<Int> = dbQuery {
        try {
            val deletedRows = OrderItems.deleteWhere { OrderItems.orderId eq orderId }
            Result.success(deletedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 