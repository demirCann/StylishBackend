package demircandemir.com.infrastructure.persistence

import demircandemir.com.domain.model.AccountStatus
import demircandemir.com.domain.model.Cart
import demircandemir.com.domain.model.CartItem
import demircandemir.com.infrastructure.persistence.repository.CartItemRepositoryImpl
import demircandemir.com.infrastructure.persistence.repository.CartRepositoryImpl
import demircandemir.com.infrastructure.persistence.tables.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CartItemRepositoryTest : BaseRepositoryTest() {
    private lateinit var repository: CartItemRepositoryImpl
    private lateinit var cartRepository: CartRepositoryImpl
    private lateinit var testCartItem: CartItem
    private var testCartId: Int = 0
    private var testProductId: Int = 0
    private var testUserId: Int = 0
    private var testColorId: Int = 0
    private var testSizeId: Int = 0

    @BeforeEach
    fun setupTestRepositoriesAndData() {
        super.logger.info("Setting up repository and test data for CartItemRepositoryTest")
        repository = CartItemRepositoryImpl()
        cartRepository = CartRepositoryImpl()

        transaction {
            CartItems.deleteAll()
            Carts.deleteAll()
        }

        transaction {
            val userId = Users.insert {
                it[firstName] = "CartItem"
                it[lastName] = "User"
                it[email] = "cartItem.user.${System.currentTimeMillis()}@example.com"
                it[passwordHash] = "password"
                it[registrationDate] = LocalDateTime.now()
                it[status] = AccountStatus.ACTIVE
            } get Users.id
            testUserId = userId.value

            val categoryEntityId = Categories.insert {
                it[categoryName] = "Test Category"
                it[description] = "Test Description"
                it[parentCategoryId] = null
            } get Categories.id

            val productId = Products.insert {
                it[productName] = "Test Product"
                it[description] = "Test Product Description"
                it[price] = BigDecimal("50.00")
                it[stockQuantity] = 100
                it[categoryId] = categoryEntityId.value
                it[brand] = "Test Brand"
                it[dateAdded] = LocalDateTime.now()
                it[isActive] = true
            } get Products.id
            testProductId = productId.value

            val colorId = Colors.insert {
                it[colorName] = "Test Color"
                it[hexCode] = "#FFFFFF"
            } get Colors.id
            testColorId = colorId.value

            val sizeId = Sizes.insert {
                it[sizeName] = "Test Size"
                it[sizeType] = "Test Size Type"
            } get Sizes.id
            testSizeId = sizeId.value
        }

        runBlocking {
            val cart = Cart(
                userId = testUserId,
                totalAmount = BigDecimal.ZERO,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            testCartId = cartRepository.create(cart).getOrThrow().id
        }

        testCartItem = CartItem(
            cartId = testCartId,
            productId = testProductId,
            quantity = 2,
            sizeId = testSizeId,
            colorId = testColorId,
            unitPrice = BigDecimal("50.00")
        )
    }

    @Test
    fun `create should add item to cart`(): Unit = runBlocking {
        val result = repository.create(testCartItem)
        result.onSuccess { createdItem ->
            assertNotNull(createdItem)
            assertEquals(testCartId, createdItem.cartId)
            assertEquals(testProductId, createdItem.productId)
            assertEquals(2, createdItem.quantity)
            assertEquals(testSizeId, createdItem.sizeId)
            assertEquals(testColorId, createdItem.colorId)
            assertEquals(BigDecimal("50.00"), createdItem.unitPrice)
            assertTrue(createdItem.id > 0)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `findById should return cart item when exists`(): Unit = runBlocking {
        val itemId = repository.create(testCartItem).getOrThrow().id

        val result = repository.findById(itemId)
        result.onSuccess { item ->
            assertNotNull(item)
            assertEquals(itemId, item.id)
            assertEquals(testCartId, item.cartId)
            assertEquals(testProductId, item.productId)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `findById should return null when cart item does not exist`(): Unit = runBlocking {
        val result = repository.findById(999)
        result.onSuccess { item ->
            assertNull(item)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `findByCartId should return all items in a cart`(): Unit = runBlocking {
        repository.create(testCartItem).getOrThrow()
        repository.create(testCartItem.copy(quantity = 3)).getOrThrow()

        val result = repository.findByCartId(testCartId)
        result.onSuccess { items ->
            assertEquals(2, items.size)
            assertTrue(items.all { it.cartId == testCartId })
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `findByCartIdAndProductId should return item when exists`(): Unit = runBlocking {
        repository.create(testCartItem).getOrThrow()

        val result = repository.findByCartIdAndProductId(
            cartId = testCartId,
            productId = testProductId,
            sizeId = testSizeId,
            colorId = testColorId
        )

        result.onSuccess { item ->
            assertNotNull(item)
            assertEquals(testCartId, item.cartId)
            assertEquals(testProductId, item.productId)
            assertEquals(testSizeId, item.sizeId)
            assertEquals(testColorId, item.colorId)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `findByCartIdAndProductId should return null when no matching item exists`(): Unit = runBlocking {
        val result = repository.findByCartIdAndProductId(
            cartId = testCartId,
            productId = 999,
            sizeId = testSizeId,
            colorId = testColorId
        )

        result.onSuccess { item ->
            assertNull(item)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `update should update existing cart item`(): Unit = runBlocking {
        val createdItem = repository.create(testCartItem).getOrThrow()

        val newSizeId = transaction {
            Sizes.insert { it[sizeName] = "New Size"; it[sizeType] = "Update" } get Sizes.id
        }.value

        val updatedItemData = createdItem.copy(
            quantity = 5,
            sizeId = newSizeId
        )

        val updateResult = repository.update(updatedItemData)
        updateResult.onSuccess { item ->
            assertEquals(5, item.quantity)
            assertEquals(newSizeId, item.sizeId)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `updateQuantity should change cart item quantity`(): Unit = runBlocking {
        val itemId = repository.create(testCartItem).getOrThrow().id

        val result = repository.updateQuantity(itemId, 10)
        result.onSuccess { updatedItem ->
            assertNotNull(updatedItem)
            assertEquals(10, updatedItem.quantity)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `delete should remove a cart item`(): Unit = runBlocking {
        val itemId = repository.create(testCartItem).getOrThrow().id

        val result = repository.delete(itemId)
        result.onSuccess { success ->
            assertTrue(success)

            val deletedItemResult = repository.findById(itemId)
            deletedItemResult.onSuccess { item ->
                assertNull(item)
            }.onFailure {
                throw AssertionError("Failed to check deleted item: ${it.message}", it)
            }
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `deleteByCartId should remove all items in a cart`(): Unit = runBlocking {
        repository.create(testCartItem).getOrThrow()
        repository.create(testCartItem.copy(quantity = 1)).getOrThrow()

        val result = repository.deleteByCartId(testCartId)
        result.onSuccess { deletedCount ->
            assertEquals(2, deletedCount)

            val remainingItemsResult = repository.findByCartId(testCartId)
            remainingItemsResult.onSuccess { items ->
                assertTrue(items.isEmpty())
            }.onFailure {
                throw AssertionError("Failed to check remaining items: ${it.message}", it)
            }
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `countByCartId should return correct item count`(): Unit = runBlocking {
        repository.create(testCartItem).getOrThrow()
        repository.create(testCartItem.copy(quantity = 1)).getOrThrow()
        repository.create(testCartItem.copy(quantity = 5)).getOrThrow()

        val result = repository.countByCartId(testCartId)
        result.onSuccess { count ->
            assertEquals(3, count)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }
} 