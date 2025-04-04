package demircandemir.com.infrastructure.persistence

import demircandemir.com.domain.model.Order
import demircandemir.com.infrastructure.persistence.repository.OrderRepositoryImpl
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
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderRepositoryTest {
    private lateinit var repository: OrderRepositoryImpl
    private lateinit var testOrder: Order
    private var testUserId: Int = 0
    private var testAddressId: Int = 0

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            TestDatabaseFactory.initTestDatabase()
            transaction {
                SchemaUtils.create(Orders, Users, Addresses)
            }
        }
    }

    @BeforeEach
    fun setupTestData() {
        repository = OrderRepositoryImpl()
        transaction {
            // Cleaning
            OrderItems.deleteAll()
            Orders.deleteAll()

            // Creating user and address for tests
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

            testUserId = userId.value
            testAddressId = addressId.value
        }

        testOrder = Order(
            userId = testUserId,
            addressId = testAddressId,
            totalAmount = BigDecimal("100.00"),
            orderDate = LocalDateTime.now(),
            paymentMethod = "Credit Card",
            orderStatus = OrderStatus.Pending,
            trackingNumber = null,
            shippingFee = BigDecimal("10.00")
        )
    }

    @Test
    fun `create should create a new order`(): Unit = runBlocking {
        val result = repository.create(testOrder)
        result.onSuccess { createdOrder ->
            assertNotNull(createdOrder)
            assertEquals(testUserId, createdOrder.userId)
            assertEquals(testAddressId, createdOrder.addressId)
            assertEquals(BigDecimal("100.00"), createdOrder.totalAmount)
            assertEquals("Credit Card", createdOrder.paymentMethod)
            assertEquals(OrderStatus.Pending, createdOrder.orderStatus)
            assertTrue(createdOrder.id > 0)
        }
    }

    @Test
    fun `findById should return order when exists`(): Unit = runBlocking {
        val createResult = repository.create(testOrder)
        val orderId = createResult.getOrNull()?.id ?: 0

        val result = repository.findById(orderId)
        result.onSuccess { order ->
            assertNotNull(order)
            assertEquals(orderId, order.id)
            assertEquals(testUserId, order.userId)
            assertEquals(testAddressId, order.addressId)
        }
    }

    @Test
    fun `findById should return null when order does not exist`(): Unit = runBlocking {
        val result = repository.findById(999)
        result.onSuccess { order ->
            assertNull(order)
        }
    }

    @Test
    fun `findByUserId should return all orders for a user`(): Unit = runBlocking {
        repository.create(testOrder)
        repository.create(testOrder.copy(paymentMethod = "PayPal"))

        val result = repository.findByUserId(testUserId)
        result.onSuccess { orders ->
            assertEquals(2, orders.size)
            assertTrue(orders.all { it.userId == testUserId })
        }
    }

    @Test
    fun `update should update existing order`(): Unit = runBlocking {
        val createResult = repository.create(testOrder)
        val createdOrder = createResult.getOrNull() ?: throw AssertionError("Failed to create test order")

        val updatedOrder = createdOrder.copy(
            totalAmount = BigDecimal("150.00"),
            orderStatus = OrderStatus.Confirmed,
            trackingNumber = "TRACK123"
        )

        val updateResult = repository.update(updatedOrder)
        updateResult.onSuccess { order ->
            assertEquals(BigDecimal("150.00"), order.totalAmount)
            assertEquals(OrderStatus.Confirmed, order.orderStatus)
            assertEquals("TRACK123", order.trackingNumber)
        }
    }

    @Test
    fun `updateStatus should change order status`(): Unit = runBlocking {
        val createResult = repository.create(testOrder)
        val orderId = createResult.getOrNull()?.id ?: 0

        val result = repository.updateStatus(orderId, OrderStatus.Shipped)
        result.onSuccess { success ->
            assertTrue(success)

            val updatedOrderResult = repository.findById(orderId)
            updatedOrderResult.onSuccess { order ->
                assertNotNull(order)
                assertEquals(OrderStatus.Shipped, order.orderStatus)
            }
        }
    }

    @Test
    fun `delete should remove an order`(): Unit = runBlocking {
        val createResult = repository.create(testOrder)
        val orderId = createResult.getOrNull()?.id ?: 0

        val result = repository.delete(orderId)
        result.onSuccess { success ->
            assertTrue(success)

            val deletedOrderResult = repository.findById(orderId)
            deletedOrderResult.onSuccess { order ->
                assertNull(order)
            }
        }
    }

    @Test
    fun `findByStatus should return orders with matching status`(): Unit = runBlocking {
        repository.create(testOrder)
        repository.create(testOrder.copy(orderStatus = OrderStatus.Confirmed))
        repository.create(testOrder.copy(orderStatus = OrderStatus.Shipped))
        repository.create(testOrder.copy(orderStatus = OrderStatus.Confirmed))

        val result = repository.findByStatus(OrderStatus.Confirmed)
        result.onSuccess { orders ->
            assertEquals(2, orders.size)
            assertTrue(orders.all { it.orderStatus == OrderStatus.Confirmed })
        }
    }

    @Test
    fun `findByTrackingNumber should return order with matching tracking number`(): Unit = runBlocking {
        val trackingNumber = "TRACK-${System.currentTimeMillis()}"
        repository.create(testOrder.copy(trackingNumber = trackingNumber))

        val result = repository.findByTrackingNumber(trackingNumber)
        result.onSuccess { order ->
            assertNotNull(order)
            assertEquals(trackingNumber, order.trackingNumber)
        }
    }

    @Test
    fun `updateStatus should return false when order does not exist`(): Unit = runBlocking {
        val result = repository.updateStatus(999, OrderStatus.Shipped)
        result.onSuccess { success ->
            assertFalse(success)
        }
    }
} 