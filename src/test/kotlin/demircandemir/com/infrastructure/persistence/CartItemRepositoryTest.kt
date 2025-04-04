package demircandemir.com.infrastructure.persistence

import demircandemir.com.domain.model.Cart
import demircandemir.com.domain.model.CartItem
import demircandemir.com.infrastructure.persistence.repository.CartItemRepositoryImpl
import demircandemir.com.infrastructure.persistence.repository.CartRepositoryImpl
import demircandemir.com.infrastructure.persistence.tables.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
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
class CartItemRepositoryTest {
    private lateinit var repository: CartItemRepositoryImpl
    private lateinit var cartRepository: CartRepositoryImpl
    private lateinit var testCartItem: CartItem
    private var testCartId: Int = 0
    private var testProductId: Int = 0
    private var testUserId: Int = 0
    private var testColorId: Int = 0
    private var testSizeId: Int = 0

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            TestDatabaseFactory.initTestDatabase()
            transaction {
                SchemaUtils.create(CartItems, Carts, Products, Users, Categories, Colors, Sizes)
            }
        }
    }

    @BeforeEach
    fun setupTestData() {
        repository = CartItemRepositoryImpl()
        cartRepository = CartRepositoryImpl()

        transaction {
            // Cleaning
            CartItems.deleteAll()
            Carts.deleteAll()
            Products.deleteAll()
            Colors.deleteAll()
            Sizes.deleteAll()

            // Creating user for tests
            val userId = Users.insert {
                it[firstName] = "CartItem"
                it[lastName] = "User"
                it[email] = "cartItem.user.${System.currentTimeMillis()}@example.com"
                it[password] = "password"
                it[registrationDate] = LocalDateTime.now()
                it[isActive] = true
            } get Users.id

            testUserId = userId.value

            // Create a category for product
            val categoryEntityId = Categories.insert {
                it[categoryName] = "Test Category"
                it[description] = "Test Description"
                it[parentCategoryId] = null
            } get Categories.id

            // Create a product
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

            // Create color and size
            val colorId = Colors.insert {
                it[colorName] = "Test Color"
                it[hexCode] = "#FFFFFF"
            } get Colors.id

            val sizeId = Sizes.insert {
                it[sizeName] = "Test Size"
                it[sizeType] = "Test Size Type"
            } get Sizes.id

            testColorId = colorId.value
            testSizeId = sizeId.value
        }

        // Create cart
        runBlocking {
            val cart = Cart(
                userId = testUserId,
                totalAmount = BigDecimal.ZERO,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val createCartResult = cartRepository.create(cart)
            testCartId = createCartResult.getOrNull()?.id ?: throw AssertionError("Failed to create test cart")
        }

        // Prepare cart item
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
        }
    }

    @Test
    fun `findById should return cart item when exists`(): Unit = runBlocking {
        val createResult = repository.create(testCartItem)
        val itemId = createResult.getOrNull()?.id ?: 0

        val result = repository.findById(itemId)
        result.onSuccess { item ->
            assertNotNull(item)
            assertEquals(itemId, item.id)
            assertEquals(testCartId, item.cartId)
            assertEquals(testProductId, item.productId)
        }
    }

    @Test
    fun `findById should return null when cart item does not exist`(): Unit = runBlocking {
        val result = repository.findById(999)
        result.onSuccess { item ->
            assertNull(item)
        }
    }

    @Test
    fun `findByCartId should return all items in a cart`(): Unit = runBlocking {
        repository.create(testCartItem)
        repository.create(testCartItem.copy(productId = testProductId))

        val result = repository.findByCartId(testCartId)
        result.onSuccess { items ->
            assertEquals(2, items.size)
            assertTrue(items.all { it.cartId == testCartId })
        }
    }

    @Test
    fun `findByCartIdAndProductId should return item when exists`(): Unit = runBlocking {
        repository.create(testCartItem)

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
        }
    }

    @Test
    fun `update should update existing cart item`(): Unit = runBlocking {
        val createResult = repository.create(testCartItem)
        val createdItem = createResult.getOrNull() ?: throw AssertionError("Failed to create test cart item")

        val updatedItem = createdItem.copy(
            quantity = 5,
            sizeId = testSizeId + 1
        )

        val updateResult = repository.update(updatedItem)
        updateResult.onSuccess { item ->
            assertEquals(5, item.quantity)
            assertEquals(testSizeId + 1, item.sizeId)
        }
    }

    @Test
    fun `updateQuantity should change cart item quantity`(): Unit = runBlocking {
        val createResult = repository.create(testCartItem)
        val itemId = createResult.getOrNull()?.id ?: 0

        val result = repository.updateQuantity(itemId, 10)
        result.onSuccess { updatedItem ->
            assertNotNull(updatedItem)
            assertEquals(10, updatedItem.quantity)
        }
    }

    @Test
    fun `delete should remove a cart item`(): Unit = runBlocking {
        val createResult = repository.create(testCartItem)
        val itemId = createResult.getOrNull()?.id ?: 0

        val result = repository.delete(itemId)
        result.onSuccess { success ->
            assertTrue(success)

            val deletedItemResult = repository.findById(itemId)
            deletedItemResult.onSuccess { item ->
                assertNull(item)
            }
        }
    }

    @Test
    fun `deleteByCartId should remove all items in a cart`(): Unit = runBlocking {
        repository.create(testCartItem)
        repository.create(testCartItem.copy(productId = testProductId))

        val result = repository.deleteByCartId(testCartId)
        result.onSuccess { deletedCount ->
            assertEquals(2, deletedCount)

            val remainingItemsResult = repository.findByCartId(testCartId)
            remainingItemsResult.onSuccess { items ->
                assertTrue(items.isEmpty())
            }
        }
    }

    @Test
    fun `countByCartId should return correct item count`(): Unit = runBlocking {
        repository.create(testCartItem)
        repository.create(testCartItem.copy(productId = testProductId))
        repository.create(testCartItem.copy(productId = testProductId))

        val result = repository.countByCartId(testCartId)
        result.onSuccess { count ->
            assertEquals(3, count)
        }
    }
} 