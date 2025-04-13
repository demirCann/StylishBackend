package demircandemir.com.infrastructure.persistence

import demircandemir.com.domain.model.AccountStatus
import demircandemir.com.domain.model.Cart
import demircandemir.com.infrastructure.persistence.repository.CartRepositoryImpl
import demircandemir.com.infrastructure.persistence.tables.Carts
import demircandemir.com.infrastructure.persistence.tables.Users
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
class CartRepositoryTest : BaseRepositoryTest() {
    private lateinit var repository: CartRepositoryImpl
    private lateinit var testCart: Cart
    private var testUserId: Int = 0

    @BeforeEach
    fun setupTestRepositoriesAndData() {
        super.logger.info("Setting up repository and test data for CartRepositoryTest")
        repository = CartRepositoryImpl()

        transaction {
            Carts.deleteAll()
        }

        transaction {
            val userId = Users.insert {
                it[firstName] = "Cart"
                it[lastName] = "User"
                it[email] = "cart.user.${System.currentTimeMillis()}@example.com"
                it[passwordHash] = "password"
                it[registrationDate] = LocalDateTime.now()
                it[status] = AccountStatus.ACTIVE
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
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `findById should return cart when exists`(): Unit = runBlocking {
        val createResult = repository.create(testCart)
        val cartId = createResult.getOrThrow().id

        val result = repository.findById(cartId)
        result.onSuccess { cart ->
            assertNotNull(cart)
            assertEquals(cartId, cart.id)
            assertEquals(testUserId, cart.userId)
            assertEquals(BigDecimal("150.00"), cart.totalAmount)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `findById should return null when cart does not exist`(): Unit = runBlocking {
        val result = repository.findById(999)
        result.onSuccess { cart ->
            assertNull(cart)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `findByUserId should return cart for a user`(): Unit = runBlocking {
        repository.create(testCart).getOrThrow()

        val result = repository.findByUserId(testUserId)
        result.onSuccess { cart ->
            assertNotNull(cart)
            assertEquals(testUserId, cart.userId)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `findByUserId should return null when user has no cart`(): Unit = runBlocking {
        val result = repository.findByUserId(999)
        result.onSuccess { cart ->
            assertNull(cart)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `update should update existing cart`(): Unit = runBlocking {
        val createdCart = repository.create(testCart).getOrThrow()

        val originalUpdatedAt = createdCart.updatedAt
        Thread.sleep(10) // Ensure time difference for updatedAt

        val updatedCartData = createdCart.copy(
            totalAmount = BigDecimal("200.00"),
            updatedAt = LocalDateTime.now()
        )

        val updateResult = repository.update(updatedCartData)
        updateResult.onSuccess { cart ->
            assertEquals(BigDecimal("200.00"), cart.totalAmount)
            assertTrue(cart.updatedAt.isAfter(originalUpdatedAt))
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `updateTotalAmount should change cart total`(): Unit = runBlocking {
        val cartId = repository.create(testCart).getOrThrow().id

        val result = repository.updateTotalAmount(cartId, BigDecimal("250.00"))
        result.onSuccess { updatedCart ->
            assertNotNull(updatedCart)
            assertEquals(BigDecimal("250.00"), updatedCart.totalAmount)
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `touch should update the updatedAt timestamp`(): Unit = runBlocking {
        val cartId = repository.create(testCart).getOrThrow().id
        val originalCart = repository.findById(cartId).getOrThrow()

        Thread.sleep(10) // Ensure time difference for updatedAt

        val result = repository.touch(cartId)
        result.onSuccess { success ->
            assertTrue(success)

            val updatedCartResult = repository.findById(cartId)
            updatedCartResult.onSuccess { cart ->
                assertNotNull(cart)
                if (originalCart != null) {
                    assertTrue(cart.updatedAt.isAfter(originalCart.updatedAt))
                }
            }.onFailure {
                throw AssertionError("Failed to retrieve updated cart: ${it.message}", it)
            }
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `delete should remove a cart`(): Unit = runBlocking {
        val cartId = repository.create(testCart).getOrThrow().id

        val result = repository.delete(cartId)
        result.onSuccess { success ->
            assertTrue(success)

            val deletedCartResult = repository.findById(cartId)
            deletedCartResult.onSuccess { cart ->
                assertNull(cart)
            }.onFailure {
                throw AssertionError("Failed to check deleted cart: ${it.message}", it)
            }
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }

    @Test
    fun `deleteByUserId should remove a user's cart`(): Unit = runBlocking {
        repository.create(testCart).getOrThrow()

        val result = repository.deleteByUserId(testUserId)
        result.onSuccess { success ->
            assertTrue(success)

            val deletedCartResult = repository.findByUserId(testUserId)
            deletedCartResult.onSuccess { cart ->
                assertNull(cart)
            }.onFailure {
                throw AssertionError("Failed to check deleted cart by user ID: ${it.message}", it)
            }
        }.onFailure {
            throw AssertionError("Test failed: ${it.message}", it)
        }
    }
} 