package demircandemir.com.infrastructure.persistence

import demircandemir.com.infrastructure.persistence.tables.Addresses
import demircandemir.com.infrastructure.persistence.tables.Users
import demircandemir.com.testutils.TestData
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
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
    private lateinit var database: Database
    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setUp() {
        logger.info("Setting up test database")

        // Initialize H2 in-memory database for testing
        database = TestDatabaseFactory.initTestDatabase()

        // Create tables
        transaction(database) {
            SchemaUtils.create(Users, Addresses)
        }

        // Initialize DatabaseFactory with test database
        DatabaseFactory.initTest(database)

        userRepository = UserRepositoryImpl()

        logger.info("Test database setup completed")
    }

    @After
    fun tearDown() {
        logger.info("Cleaning up test database")

        transaction(database) {
            SchemaUtils.drop(Users, Addresses)
        }

        logger.info("Test database cleanup completed")
    }

    @Test
    fun `test create and get user`() = runBlocking {
        logger.info("Starting create and get user test")

        // Given
        val user = TestData.Users.createTestUser()

        // When
        val createdUser = userRepository.createUser(user)
        logger.info("Created user with ID: ${createdUser.id}, ${createdUser.firstName}")

        val retrievedUser = userRepository.getUserById(createdUser.id)
        logger.info("Retrieved user: $retrievedUser")

        // Then
        assertNotNull(retrievedUser, "Retrieved user should not be null")
        assertEquals(createdUser.email, retrievedUser.email)
        assertEquals(createdUser.firstName, retrievedUser.firstName)
        assertEquals(createdUser.lastName, retrievedUser.lastName)

        logger.info("Create and get user test completed successfully")
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