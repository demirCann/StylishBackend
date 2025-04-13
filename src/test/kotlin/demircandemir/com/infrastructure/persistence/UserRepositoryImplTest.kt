package demircandemir.com.infrastructure.persistence

import demircandemir.com.domain.model.AccountStatus
import demircandemir.com.infrastructure.persistence.repository.RefreshTokenRepositoryImpl
import demircandemir.com.infrastructure.persistence.repository.UserRepositoryImpl
import demircandemir.com.testutils.TestData
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.util.*
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryImplTest : BaseRepositoryTest() {
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var refreshTokenRepository: RefreshTokenRepositoryImpl

    @BeforeEach
    fun setupTestRepositories() {
        logger.info("Setting up repositories for UserRepositoryImplTest")
        userRepository = UserRepositoryImpl()
        refreshTokenRepository = RefreshTokenRepositoryImpl()
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

    // Authentication-related tests moved from UserRepositoryTest

    @Test
    fun `test update last login date`(): Unit = runBlocking {
        // Given
        val user = TestData.Users.createTestUser()
        val createdUser = userRepository.createUser(user)

        // When
        val result = userRepository.updateLoginStats(createdUser.id, false)

        // Then
        assertTrue(result)
        val updatedUser = userRepository.getUserById(createdUser.id)
        assertNotNull(updatedUser)
        assertNotNull(updatedUser.lastLoginDate)
    }

    @Test
    fun `test update failed login attempts`(): Unit = runBlocking {
        // Given
        val user = TestData.Users.createTestUser()
        val createdUser = userRepository.createUser(user)

        // When
        val result = userRepository.updateLoginStats(
            createdUser.id,
            true
        )

        // Then
        assertTrue(result)
        val updatedUser = userRepository.getUserById(createdUser.id)
        assertNotNull(updatedUser)
        assertEquals(1, updatedUser.failedLoginAttempts)
        assertNotNull(updatedUser.lastFailedLoginAttempt)
    }

    @Test
    fun `test create and validate refresh token`() = runBlocking {
        // Given
        val user = TestData.Users.createTestUser()
        val createdUser = userRepository.createUser(user)
        val refreshToken = UUID.randomUUID().toString()
        val expiresAt = LocalDateTime.now().plusDays(7)

        // When
        val createdToken = refreshTokenRepository.createToken(createdUser.id, refreshToken, expiresAt)
        val foundToken = refreshTokenRepository.findByToken(refreshToken)

        // Then
        assertNotNull(createdToken)
        assertNotNull(foundToken)
        assertEquals(createdUser.id, foundToken.userId)
        assertEquals(refreshToken, foundToken.token)
        assertFalse(foundToken.isRevoked)
    }

    @Test
    fun `test revoke refresh token`() = runBlocking {
        // Given
        val user = TestData.Users.createTestUser()
        val createdUser = userRepository.createUser(user)
        val refreshToken = UUID.randomUUID().toString()
        val expiresAt = LocalDateTime.now().plusDays(7)
        refreshTokenRepository.createToken(createdUser.id, refreshToken, expiresAt)

        // When
        val result = refreshTokenRepository.revokeToken(refreshToken)

        // Then
        assertTrue(result)
        val foundToken = refreshTokenRepository.findByToken(refreshToken)
        assertNotNull(foundToken)
        assertTrue(foundToken.isRevoked)
    }

    @Test
    fun `test update password reset token`() = runBlocking {
        // Given
        val user = TestData.Users.createTestUser()
        val createdUser = userRepository.createUser(user)
        val resetToken = UUID.randomUUID().toString()
        val expiryDate = LocalDateTime.now().plusHours(1)

        // When
        val result = userRepository.createPasswordResetToken(createdUser.email, resetToken, expiryDate)

        // Then
        assertTrue(result)
        val updatedUser = userRepository.getUserById(createdUser.id)
        assertNotNull(updatedUser)
        assertEquals(resetToken, updatedUser.passwordResetToken)
        assertEquals(expiryDate, updatedUser.passwordResetTokenExpiry)
    }

    @Test
    fun `test update verification token`() = runBlocking {
        // Given
        val user = TestData.Users.createTestUser()
        val createdUser = userRepository.createUser(user)
        val verificationToken = UUID.randomUUID().toString()

        // When
        // Directly update the user with the verification token
        val updatedUser = userRepository.updateUser(
            createdUser.copy(verificationToken = verificationToken)
        )

        // Then
        assertEquals(verificationToken, updatedUser.verificationToken)
        val retrievedUser = userRepository.getUserById(createdUser.id)
        assertNotNull(retrievedUser)
        assertEquals(verificationToken, retrievedUser.verificationToken)
    }

    @Test
    fun `test update account status`() = runBlocking {
        // Given
        val user = TestData.Users.createTestUser()
        val createdUser = userRepository.createUser(user)
        val newStatus = AccountStatus.LOCKED

        // When
        val result = userRepository.updateUserStatus(createdUser.id, newStatus)

        // Then
        assertTrue(result)
        val updatedUser = userRepository.getUserById(createdUser.id)
        assertNotNull(updatedUser)
        assertEquals(newStatus, updatedUser.status)
    }
} 