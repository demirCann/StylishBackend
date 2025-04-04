package demircandemir.com.infrastructure.persistence

import demircandemir.com.domain.model.Cart
import demircandemir.com.infrastructure.persistence.repository.CartRepositoryImpl
import demircandemir.com.infrastructure.persistence.tables.Carts
import demircandemir.com.infrastructure.persistence.tables.Users
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
class CartRepositoryTest {
    private lateinit var repository: CartRepositoryImpl
    private lateinit var testCart: Cart
    private var testUserId: Int = 0

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            TestDatabaseFactory.initTestDatabase()
            transaction {
                SchemaUtils.create(Carts, Users)
            }
        }
    }

    @BeforeEach
    fun setupTestData() {
        repository = CartRepositoryImpl()
        transaction {
            // Cleaning
            Carts.deleteAll()

            // Creating user for tests
            val userId = Users.insert {
                it[firstName] = "Cart"
                it[lastName] = "User"
                it[email] = "cart.user.${System.currentTimeMillis()}@example.com"
                it[password] = "password"
                it[registrationDate] = LocalDateTime.now()
                it[isActive] = true
            } get Users.id

            testUserId = userId.value
        }

        testCart = Cart(
            userId = testUserId,
            totalAmount = BigDecimal("150.00"),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `create should create a new cart`(): Unit = runBlocking {
        val result = repository.create(testCart)
        result.onSuccess { createdCart ->
            assertNotNull(createdCart)
            assertEquals(testUserId, createdCart.userId)
            assertEquals(BigDecimal("150.00"), createdCart.totalAmount)
            assertTrue(createdCart.id > 0)
        }
    }

    @Test
    fun `findById should return cart when exists`(): Unit = runBlocking {
        val createResult = repository.create(testCart)
        val cartId = createResult.getOrNull()?.id ?: 0

        val result = repository.findById(cartId)
        result.onSuccess { cart ->
            assertNotNull(cart)
            assertEquals(cartId, cart.id)
            assertEquals(testUserId, cart.userId)
            assertEquals(BigDecimal("150.00"), cart.totalAmount)
        }
    }

    @Test
    fun `findById should return null when cart does not exist`(): Unit = runBlocking {
        val result = repository.findById(999)
        result.onSuccess { cart ->
            assertNull(cart)
        }
    }

    @Test
    fun `findByUserId should return cart for a user`(): Unit = runBlocking {
        repository.create(testCart)

        val result = repository.findByUserId(testUserId)
        result.onSuccess { cart ->
            assertNotNull(cart)
            assertEquals(testUserId, cart.userId)
        }
    }

    @Test
    fun `findByUserId should return null when user has no cart`(): Unit = runBlocking {
        val result = repository.findByUserId(999)
        result.onSuccess { cart ->
            assertNull(cart)
        }
    }

    @Test
    fun `update should update existing cart`(): Unit = runBlocking {
        val createResult = repository.create(testCart)
        val createdCart = createResult.getOrNull() ?: throw AssertionError("Failed to create test cart")

        val originalUpdatedAt = createdCart.updatedAt
        Thread.sleep(10) // Ensure time difference for updatedAt

        val updatedCart = createdCart.copy(
            totalAmount = BigDecimal("200.00"),
            updatedAt = LocalDateTime.now()
        )

        val updateResult = repository.update(updatedCart)
        updateResult.onSuccess { cart ->
            assertEquals(BigDecimal("200.00"), cart.totalAmount)
            assertTrue(cart.updatedAt.isAfter(originalUpdatedAt))
        }
    }

    @Test
    fun `updateTotalAmount should change cart total`(): Unit = runBlocking {
        val createResult = repository.create(testCart)
        val cartId = createResult.getOrNull()?.id ?: 0

        val result = repository.updateTotalAmount(cartId, BigDecimal("250.00"))
        result.onSuccess { updatedCart ->
            assertNotNull(updatedCart)
            assertEquals(BigDecimal("250.00"), updatedCart.totalAmount)
        }
    }

    @Test
    fun `touch should update the updatedAt timestamp`(): Unit = runBlocking {
        val createResult = repository.create(testCart)
        val cartId = createResult.getOrNull()?.id ?: 0
        val originalCart =
            repository.findById(cartId).getOrNull() ?: throw AssertionError("Failed to retrieve test cart")

        Thread.sleep(10) // Ensure time difference for updatedAt

        val result = repository.touch(cartId)
        result.onSuccess { success ->
            assertTrue(success)

            val updatedCartResult = repository.findById(cartId)
            updatedCartResult.onSuccess { cart ->
                assertNotNull(cart)
                assertTrue(cart.updatedAt.isAfter(originalCart.updatedAt))
            }
        }
    }

    @Test
    fun `delete should remove a cart`(): Unit = runBlocking {
        val createResult = repository.create(testCart)
        val cartId = createResult.getOrNull()?.id ?: 0

        val result = repository.delete(cartId)
        result.onSuccess { success ->
            assertTrue(success)

            val deletedCartResult = repository.findById(cartId)
            deletedCartResult.onSuccess { cart ->
                assertNull(cart)
            }
        }
    }

    @Test
    fun `deleteByUserId should remove a user's cart`(): Unit = runBlocking {
        repository.create(testCart)

        val result = repository.deleteByUserId(testUserId)
        result.onSuccess { success ->
            assertTrue(success)

            val deletedCartResult = repository.findByUserId(testUserId)
            deletedCartResult.onSuccess { cart ->
                assertNull(cart)
            }
        }
    }
} 