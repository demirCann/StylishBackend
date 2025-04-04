package demircandemir.com.infrastructure.persistence.repository

import demircandemir.com.domain.model.Cart
import demircandemir.com.domain.repository.CartRepository
import demircandemir.com.infrastructure.persistence.DatabaseFactory.dbQuery
import demircandemir.com.infrastructure.persistence.tables.Carts
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDateTime

class CartRepositoryImpl : CartRepository {

    private fun ResultRow.toCart(): Cart {
        return Cart(
            id = this[Carts.id].value,
            userId = this[Carts.userId].value,
            totalAmount = this[Carts.totalAmount],
            createdAt = this[Carts.createdAt],
            updatedAt = this[Carts.updatedAt]
        )
    }

    override suspend fun create(cart: Cart): Result<Cart> = dbQuery {
        try {
            val insertStatement = Carts.insert {
                it[userId] = cart.userId
                it[totalAmount] = cart.totalAmount
                it[createdAt] = cart.createdAt
                it[updatedAt] = cart.updatedAt
            }

            val resultRow = insertStatement.resultedValues?.first()
                ?: return@dbQuery Result.failure(Exception("Failed to insert cart"))

            Result.success(resultRow.toCart())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findById(id: Int): Result<Cart?> = dbQuery {
        try {
            val cart = Carts
                .selectAll()
                .where { Carts.id eq id }
                .map { it.toCart() }
                .singleOrNull()

            Result.success(cart)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findByUserId(userId: Int): Result<Cart?> = dbQuery {
        try {
            val cart = Carts
                .selectAll()
                .where { Carts.userId eq userId }
                .map { it.toCart() }
                .singleOrNull()

            Result.success(cart)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun update(cart: Cart): Result<Cart> = dbQuery {
        try {
            val now = LocalDateTime.now()

            Carts.update({ Carts.id eq cart.id }) {
                it[userId] = cart.userId
                it[totalAmount] = cart.totalAmount
                it[updatedAt] = now
            }

            val updatedCart = Carts
                .selectAll()
                .where { Carts.id eq cart.id }
                .map { it.toCart() }
                .singleOrNull()
                ?: return@dbQuery Result.failure(Exception("Failed to retrieve updated cart"))

            Result.success(updatedCart)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTotalAmount(cartId: Int, newAmount: BigDecimal): Result<Cart> = dbQuery {
        try {
            val now = LocalDateTime.now()

            Carts.update({ Carts.id eq cartId }) {
                it[totalAmount] = newAmount
                it[updatedAt] = now
            }

            val updatedCart = Carts
                .selectAll()
                .where { Carts.id eq cartId }
                .map { it.toCart() }
                .singleOrNull()
                ?: return@dbQuery Result.failure(Exception("Failed to retrieve updated cart"))

            Result.success(updatedCart)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun touch(cartId: Int): Result<Boolean> = dbQuery {
        try {
            val now = LocalDateTime.now()

            val updatedRows = Carts.update({ Carts.id eq cartId }) {
                it[updatedAt] = now
            }

            Result.success(updatedRows > 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(id: Int): Result<Boolean> = dbQuery {
        try {
            val deletedRows = Carts.deleteWhere { Carts.id eq id }
            Result.success(deletedRows > 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteByUserId(userId: Int): Result<Boolean> = dbQuery {
        try {
            val deletedRows = Carts.deleteWhere { Carts.userId eq userId }
            Result.success(deletedRows > 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 