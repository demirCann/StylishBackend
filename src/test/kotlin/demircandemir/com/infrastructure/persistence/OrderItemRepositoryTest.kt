package demircandemir.com.infrastructure.persistence

import demircandemir.com.domain.model.OrderItem
import demircandemir.com.infrastructure.persistence.repository.OrderItemRepositoryImpl
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
class OrderItemRepositoryTest {
    private lateinit var repository: OrderItemRepositoryImpl
    private lateinit var testOrderItem: OrderItem
    private var testOrderId: Int = 0
    private var testProductId: Int = 0
    private var testSizeId: Int? = null
    private var testColorId: Int? = null

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            TestDatabaseFactory.initTestDatabase()
            transaction {
                SchemaUtils.create(
                    Orders, OrderItems, Users, Addresses,
                    Products, Categories, Sizes, Colors
                )
            }
        }
    }

    @BeforeEach
    fun setupTestData() {
        repository = OrderItemRepositoryImpl()
        transaction {
            // Temizleme işlemleri
            OrderItems.deleteAll()
            Orders.deleteAll()
            Products.deleteAll()

            // Kullanıcı ve adres oluşturma
            val userId = Users.insert {
                it[firstName] = "Test"
                it[lastName] = "User"
                it[email] = "test.user.${System.currentTimeMillis()}@example.com"
                it[password] = "password"
                it[registrationDate] = LocalDateTime.now()
                it[isActive] = true
            } get Users.id

            val addressId = Addresses.insert {
                it[this.userId] = userId
                it[addressTitle] = "Test Address"
                it[address] = "123 Test St"
                it[city] = "Test City"
                it[district] = "Test District"
                it[postalCode] = "12345"
                it[country] = "Test Country"
            } get Addresses.id

            // Kategori oluşturma
            val categoryId = Categories.insert {
                it[categoryName] = "Test Category"
                it[description] = "Test Category Description"
                it[parentCategoryId] = null
            } get Categories.id

            // Ürün oluşturma
            val productId = Products.insert {
                it[productName] = "Test Product"
                it[description] = "Test Product Description"
                it[price] = BigDecimal("50.00")
                it[stockQuantity] = 100
                it[this.categoryId] = categoryId
                it[brand] = "Test Brand"
                it[dateAdded] = LocalDateTime.now()
                it[isActive] = true
            } get Products.id

            // Beden ve renk oluşturma (isteğe bağlı)
            val sizeId = Sizes.insert {
                it[sizeName] = "M"
                it[sizeType] = "Regular"
            } get Sizes.id

            val colorId = Colors.insert {
                it[colorName] = "Red"
                it[hexCode] = "#FF0000"
            } get Colors.id

            // Sipariş oluşturma
            val orderId = Orders.insert {
                it[this.userId] = userId
                it[this.addressId] = addressId
                it[totalAmount] = BigDecimal("100.00")
                it[orderDate] = LocalDateTime.now()
                it[paymentMethod] = "Credit Card"
                it[orderStatus] = OrderStatus.Pending
                it[trackingNumber] = null
                it[shippingFee] = BigDecimal("10.00")
            } get Orders.id

            testOrderId = orderId.value
            testProductId = productId.value
            testSizeId = sizeId.value
            testColorId = colorId.value
        }

        testOrderItem = OrderItem(
            orderId = testOrderId,
            productId = testProductId,
            quantity = 2,
            unitPrice = BigDecimal("50.00"),
            sizeId = testSizeId,
            colorId = testColorId
        )
    }

    @Test
    fun `create should create a new order item`(): Unit = runBlocking {
        val result = repository.create(testOrderItem)
        result.onSuccess { createdOrderItem ->
            assertNotNull(createdOrderItem)
            assertEquals(testOrderId, createdOrderItem.orderId)
            assertEquals(testProductId, createdOrderItem.productId)
            assertEquals(2, createdOrderItem.quantity)
            assertEquals(BigDecimal("50.00"), createdOrderItem.unitPrice)
            assertEquals(testSizeId, createdOrderItem.sizeId)
            assertEquals(testColorId, createdOrderItem.colorId)
            assertTrue(createdOrderItem.id > 0)
        }
    }

    @Test
    fun `createMany should create multiple order items`(): Unit = runBlocking {
        val items = listOf(
            testOrderItem,
            testOrderItem.copy(productId = testProductId, quantity = 1)
        )

        val result = repository.createMany(items)
        result.onSuccess { createdItems ->
            assertEquals(2, createdItems.size)
            assertTrue(createdItems.all { it.orderId == testOrderId })
            assertEquals(3, createdItems.sumOf { it.quantity })
        }
    }

    @Test
    fun `findById should return order item when exists`(): Unit = runBlocking {
        val createResult = repository.create(testOrderItem)
        val orderItemId = createResult.getOrNull()?.id ?: 0

        val result = repository.findById(orderItemId)
        result.onSuccess { orderItem ->
            assertNotNull(orderItem)
            assertEquals(orderItemId, orderItem.id)
            assertEquals(testOrderId, orderItem.orderId)
            assertEquals(testProductId, orderItem.productId)
        }
    }

    @Test
    fun `findById should return null when order item does not exist`(): Unit = runBlocking {
        val result = repository.findById(999)
        result.onSuccess { orderItem ->
            assertNull(orderItem)
        }
    }

    @Test
    fun `findByOrderId should return all items for an order`(): Unit = runBlocking {
        repository.create(testOrderItem)
        repository.create(testOrderItem.copy(quantity = 3))

        val result = repository.findByOrderId(testOrderId)
        result.onSuccess { items ->
            assertEquals(2, items.size)
            assertTrue(items.all { it.orderId == testOrderId })
            assertEquals(5, items.sumOf { it.quantity }) // 2 + 3
        }
    }

    @Test
    fun `update should update existing order item`(): Unit = runBlocking {
        val createResult = repository.create(testOrderItem)
        val createdItem = createResult.getOrNull() ?: throw AssertionError("Failed to create test order item")

        val updatedItem = createdItem.copy(
            quantity = 5,
            unitPrice = BigDecimal("55.00")
        )

        val updateResult = repository.update(updatedItem)
        updateResult.onSuccess { item ->
            assertEquals(5, item.quantity)
            assertEquals(BigDecimal("55.00"), item.unitPrice)
        }
    }

    @Test
    fun `updateQuantity should change item quantity`(): Unit = runBlocking {
        val createResult = repository.create(testOrderItem)
        val orderItemId = createResult.getOrNull()?.id ?: 0

        val result = repository.updateQuantity(orderItemId, 10)
        result.onSuccess { updatedItem ->
            assertEquals(10, updatedItem.quantity)
        }
    }

    @Test
    fun `delete should remove an order item`(): Unit = runBlocking {
        val createResult = repository.create(testOrderItem)
        val orderItemId = createResult.getOrNull()?.id ?: 0

        val result = repository.delete(orderItemId)
        result.onSuccess { success ->
            assertTrue(success)

            val deletedItemResult = repository.findById(orderItemId)
            deletedItemResult.onSuccess { item ->
                assertNull(item)
            }
        }
    }

    @Test
    fun `deleteByOrderId should remove all items from an order`(): Unit = runBlocking {
        repository.create(testOrderItem)
        repository.create(testOrderItem.copy(quantity = 3))

        val result = repository.deleteByOrderId(testOrderId)
        result.onSuccess { deletedCount ->
            assertEquals(2, deletedCount)

            val remainingItemsResult = repository.findByOrderId(testOrderId)
            remainingItemsResult.onSuccess { items ->
                assertTrue(items.isEmpty())
            }
        }
    }

    @Test
    fun `updateQuantity should throw exception for invalid quantity`(): Unit = runBlocking {
        val createResult = repository.create(testOrderItem)
        val orderItemId = createResult.getOrNull()?.id ?: 0

        val result = repository.updateQuantity(orderItemId, -1)
        result.onFailure { exception ->
            assertNotNull(exception)
            assertTrue(exception.message?.contains("New quantity must be positive") ?: false)
        }
    }

    @Test
    fun `updateQuantity should return failure when item does not exist`(): Unit = runBlocking {
        val result = repository.updateQuantity(999, 5)
        result.onFailure { exception ->
            assertNotNull(exception)
        }
    }
} 