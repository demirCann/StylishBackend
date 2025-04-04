package demircandemir.com.infrastructure.persistence

import demircandemir.com.infrastructure.persistence.repository.UserRepositoryImpl
import demircandemir.com.infrastructure.persistence.tables.Addresses
import demircandemir.com.infrastructure.persistence.tables.Users
import demircandemir.com.testutils.TestData
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserRepositoryImplTest {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setUp() {
        // Initialize H2 in-memory database for testing
        TestDatabaseFactory.initTestDatabase()

        // Create tables
        transaction {
            SchemaUtils.create(Users, Addresses)
        }

        userRepository = UserRepositoryImpl()
    }

    @After
    fun tearDown() {
        logger.info("Cleaning up test database")

        transaction {
            SchemaUtils.drop(Users, Addresses)
        }
    }

    @Test
    fun `test create and get user`() = runBlocking {
        // Given
        val user = TestData.Users.createTestUser()

        // When
        val createdUser = userRepository.createUser(user)
        val retrievedUser = userRepository.getUserById(createdUser.id)

        // Then
        assertNotNull(retrievedUser, "Retrieved user should not be null")
        assertEquals(createdUser.email, retrievedUser.email)
        assertEquals(createdUser.firstName, retrievedUser.firstName)
        assertEquals(createdUser.lastName, retrievedUser.lastName)
    }

    @Test
    fun `test update user`() = runBlocking {
        // Given
        val user = TestData.Users.createTestUser()
        val createdUser = userRepository.createUser(user)

        // When
        val updatedUser = userRepository.updateUser(
            createdUser.copy(firstName = "Updated", lastName = "Name")
        )
        val retrievedUser = userRepository.getUserById(updatedUser.id)

        // Then
        assertNotNull(retrievedUser)
        assertEquals("Updated", retrievedUser.firstName)
        assertEquals("Name", retrievedUser.lastName)
    }

    @Test
    fun `test delete user`() = runBlocking {
        // Given
        val user = TestData.Users.createTestUser()
        val createdUser = userRepository.createUser(user)

        // When
        userRepository.deleteUser(createdUser.id)
        val retrievedUser = userRepository.getUserById(createdUser.id)

        // Then
        assertNull(retrievedUser)
    }

    @Test
    fun `test create and get address`() = runBlocking {
        // Given
        val user = TestData.Users.createTestUser()
        val createdUser = userRepository.createUser(user)

        val address = TestData.Addresses.createTestAddress(userId = createdUser.id)

        // When
        val createdAddress = userRepository.createAddress(address)
        val retrievedAddress = userRepository.getAddressById(createdAddress.id)

        // Then
        assertNotNull(retrievedAddress)
        assertEquals(createdAddress.addressTitle, retrievedAddress.addressTitle)
        assertEquals(createdAddress.address, retrievedAddress.address)
        assertEquals(createdAddress.city, retrievedAddress.city)
    }

    @Test
    fun `test get addresses by user id`() = runBlocking {
        // Given
        val user = TestData.Users.createTestUser()
        val createdUser = userRepository.createUser(user)

        val addresses = TestData.Addresses.createTestAddresses(userId = createdUser.id)
        addresses.forEach { address -> userRepository.createAddress(address) }

        // When
        val retrievedAddresses = userRepository.getAddressesByUserId(createdUser.id)

        // Then
        assertEquals(2, retrievedAddresses.size)
    }
} 