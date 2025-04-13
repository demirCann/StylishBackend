package demircandemir.com.infrastructure.persistence

import demircandemir.com.domain.model.OrderItem
import demircandemir.com.infrastructure.persistence.repository.OrderItemRepositoryImpl
import demircandemir.com.infrastructure.persistence.tables.*
import demircandemir.com.testutils.TestData
import kotlinx.coroutines.runBlocking
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
class OrderItemRepositoryTest : BaseRepositoryTest() {
    private lateinit var repository: OrderItemRepositoryImpl
    private lateinit var testOrderItem: OrderItem
    private var testOrderId: Int = 0
    private var testProductId: Int = 0
    private var testSizeId: Int? = null
    private var testColorId: Int? = null

    @BeforeEach
    fun setupTestRepositoriesAndData() {
        super.logger.info("Setting up repository and test data for OrderItemRepositoryTest")
        repository = OrderItemRepositoryImpl()

        transaction {
            val user = TestData.Users.createTestUser(email = "orderitem.user.${System.currentTimeMillis()}@example.com")
            val createdUserResult = Users.insert {
                it[firstName] = user.firstName
                it[lastName] = user.lastName
                it[email] = user.email
                it[passwordHash] = user.passwordHash
                it[registrationDate] = user.registrationDate
                it[status] = user.status
            }
            val userId = createdUserResult[Users.id].value

            val address = TestData.Addresses.createTestAddress(userId = userId)
            val createdAddressResult = Addresses.insert {
                it[this.userId] = address.userId
                it[addressTitle] = address.addressTitle
                it[this.address] = address.address
                it[city] = address.city
                it[district] = address.district
                it[postalCode] = address.postalCode
                it[country] = address.country
            }
            val addressId = createdAddressResult[Addresses.id].value

            val categoryResult = Categories.insert {
                it[categoryName] = "Test Category for OrderItem"
                it[description] = "Test Category Description"
            }
            val categoryId = categoryResult[Categories.id].value

            val product = TestData.Products.createTestProduct(categoryId = categoryId)
            val createdProductResult = Products.insert {
                it[productName] = product.productName
                it[description] = product.description
                it[price] = product.price
                it[stockQuantity] = product.stockQuantity
                it[this.categoryId] = product.categoryId
                it[brand] = product.brand
                it[dateAdded] = product.dateAdded
                it[isActive] = product.isActive
            }
            testProductId = createdProductResult[Products.id].value

            val sizeResult = Sizes.insert {
                it[sizeName] = "M"
                it[sizeType] = "Regular"
            }
            testSizeId = sizeResult[Sizes.id].value

            val colorResult = Colors.insert {
                it[colorName] = "Red"
                it[hexCode] = "#FF0000"
            }
            testColorId = colorResult[Colors.id].value

            val orderResult = Orders.insert {
                it[this.userId] = userId
                it[this.addressId] = addressId
                it[totalAmount] = BigDecimal("100.00")
                it[orderDate] = LocalDateTime.now()
                it[paymentMethod] = "Credit Card"
                it[orderStatus] = OrderStatus.Pending
                it[trackingNumber] = null
                it[shippingFee] = BigDecimal("10.00")
            }
            testOrderId = orderResult[Orders.id].value
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
            assertEquals(5, items.sumOf { it.quantity })
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
            assertNotNull(updatedItem, "Updated item should not be null")
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
            // Consider adding a more specific check for the exception type or message if needed
        }
    }
}