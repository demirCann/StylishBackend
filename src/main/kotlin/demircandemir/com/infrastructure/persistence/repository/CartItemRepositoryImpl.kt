package demircandemir.com.infrastructure.persistence.repository

import demircandemir.com.domain.model.CartItem
import demircandemir.com.domain.repository.CartItemRepository
import demircandemir.com.infrastructure.persistence.DatabaseFactory.dbQuery
import demircandemir.com.infrastructure.persistence.tables.CartItems
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class CartItemRepositoryImpl : CartItemRepository {

    private fun ResultRow.toCartItem(): CartItem {
        return CartItem(
            id = this[CartItems.id].value,
            cartId = this[CartItems.cartId].value,
            productId = this[CartItems.productId].value,
            quantity = this[CartItems.quantity],
            unitPrice = this[CartItems.unitPrice],
            sizeId = this[CartItems.sizeId]?.value,
            colorId = this[CartItems.colorId]?.value
        )
    }

    override suspend fun create(cartItem: CartItem): Result<CartItem> = dbQuery {
        try {
            val insertStatement = CartItems.insert {
                it[cartId] = cartItem.cartId
                it[productId] = cartItem.productId
                it[quantity] = cartItem.quantity
                it[unitPrice] = cartItem.unitPrice
                it[sizeId] = cartItem.sizeId
                it[colorId] = cartItem.colorId
            }

            val resultRow = insertStatement.resultedValues?.first()
                ?: return@dbQuery Result.failure(Exception("Failed to insert cart item"))

            Result.success(resultRow.toCartItem())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findById(id: Int): Result<CartItem?> = dbQuery {
        try {
            val cartItem = CartItems
                .selectAll()
                .where { CartItems.id eq id }
                .map { it.toCartItem() }
                .singleOrNull()

            Result.success(cartItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findByCartId(cartId: Int): Result<List<CartItem>> = dbQuery {
        try {
            val cartItems = CartItems
                .selectAll()
                .where { CartItems.cartId eq cartId }
                .map { it.toCartItem() }

            Result.success(cartItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findByCartIdAndProductId(
        cartId: Int,
        productId: Int,
        sizeId: Int?,
        colorId: Int?
    ): Result<CartItem?> = dbQuery {
        try {
            val query = CartItems.selectAll()
                .where {
                    (CartItems.cartId eq cartId) and (CartItems.productId eq productId)
                }

            // Add size filter if provided
            val queryWithSize = if (sizeId != null) {
                query.andWhere { CartItems.sizeId eq sizeId }
            } else {
                query.andWhere { CartItems.sizeId.isNull() }
            }

            // Add color filter if provided
            val finalQuery = if (colorId != null) {
                queryWithSize.andWhere { CartItems.colorId eq colorId }
            } else {
                queryWithSize.andWhere { CartItems.colorId.isNull() }
            }

            val cartItem = finalQuery
                .map { it.toCartItem() }
                .singleOrNull()

            Result.success(cartItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun update(cartItem: CartItem): Result<CartItem> = dbQuery {
        try {
            CartItems.update({ CartItems.id eq cartItem.id }) {
                it[cartId] = cartItem.cartId
                it[productId] = cartItem.productId
                it[quantity] = cartItem.quantity
                it[unitPrice] = cartItem.unitPrice
                it[sizeId] = cartItem.sizeId
                it[colorId] = cartItem.colorId
            }

            val updatedCartItem = CartItems
                .selectAll()
                .where { CartItems.id eq cartItem.id }
                .map { it.toCartItem() }
                .singleOrNull()
                ?: return@dbQuery Result.failure(Exception("Failed to retrieve updated cart item"))

            Result.success(updatedCartItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateQuantity(id: Int, newQuantity: Int): Result<CartItem> = dbQuery {
        try {
            require(newQuantity > 0) { "New quantity must be positive" }

            CartItems.update({ CartItems.id eq id }) {
                it[quantity] = newQuantity
            }

            val updatedCartItem = CartItems
                .selectAll()
                .where { CartItems.id eq id }
                .map { it.toCartItem() }
                .singleOrNull()
                ?: return@dbQuery Result.failure(Exception("Failed to retrieve updated cart item"))

            Result.success(updatedCartItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(id: Int): Result<Boolean> = dbQuery {
        try {
            val deletedRows = CartItems.deleteWhere { CartItems.id eq id }
            Result.success(deletedRows > 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteByCartId(cartId: Int): Result<Int> = dbQuery {
        try {
            val deletedRows = CartItems.deleteWhere { CartItems.cartId eq cartId }
            Result.success(deletedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun countByCartId(cartId: Int): Result<Int> = dbQuery {
        try {
            val count = CartItems
                .selectAll()
                .where { CartItems.cartId eq cartId }
                .count()
                .toInt()

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 