package demircandemir.com.application.service

import demircandemir.com.application.dto.CreateUserRequest
import demircandemir.com.application.dto.LoginRequest
import demircandemir.com.application.dto.RefreshTokenRequest
import demircandemir.com.domain.model.AccountStatus
import demircandemir.com.domain.model.RefreshToken
import demircandemir.com.domain.model.User
import demircandemir.com.domain.model.UserRole
import demircandemir.com.domain.repository.RefreshTokenRepository
import demircandemir.com.domain.repository.UserRepository
import demircandemir.com.infrastructure.security.JwtConfig
import io.ktor.server.config.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthServiceTest {
    private lateinit var authService: AuthService
    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var jwtConfig: JwtConfig
    private lateinit var emailService: EmailService

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        refreshTokenRepository = mockk()
        val config = ApplicationConfig("application-test.conf")
        jwtConfig = JwtConfig(config)
        emailService = mockk()
        authService = AuthService(userRepository, refreshTokenRepository, jwtConfig, emailService)
    }

    /**
     * Helper function to create a test user
     */
    private fun createTestUser(
        id: Int = 1,
        email: String = "test@example.com",
        firstName: String = "Test",
        lastName: String = "User",
        role: UserRole = UserRole.CUSTOMER,
        status: AccountStatus = AccountStatus.ACTIVE,
        passwordHash: String = "hashed_password_123",
        phoneNumber: String? = null,
        lastFailedLoginAttempt: LocalDateTime? = null
    ): User {
        return User(
            id = id,
            email = email,
            passwordHash = passwordHash,
            firstName = firstName,
            lastName = lastName,
            role = role,
            status = status,
            registrationDate = LocalDateTime.now(),
            phoneNumber = phoneNumber,
            lastFailedLoginAttempt = lastFailedLoginAttempt
        )
    }

    @Test
    fun `test successful login`() = runBlocking {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val hashedPassword = "hashed_password_123" // In real app, this would be properly hashed

        val user = createTestUser(
            email = email,
            passwordHash = hashedPassword
        )

        val loginRequest = LoginRequest(email, password)
        val refreshToken = "valid_refresh_token"
        val expiryDate = LocalDateTime.now().plusDays(7)

        coEvery { userRepository.getUserByEmail(email) } returns user
        coEvery { userRepository.updateLoginStats(1, false) } returns true
        coEvery { refreshTokenRepository.createToken(1, any(), any()) } returns RefreshToken(
            id = 1,
            userId = 1,
            token = refreshToken,
            expiresAt = expiryDate,
            isRevoked = false
        )

        // When
        val result = authService.loginUser(loginRequest)

        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertNotNull(response.token)
        assertNotNull(response.user)
        assertEquals(user.id, response.user.id)
        assertEquals(user.email, response.user.email)
    }

    @Test
    fun `test login with invalid credentials`() = runBlocking {
        // Given
        val email = "test@example.com"
        val password = "wrong_password"

        val loginRequest = LoginRequest(email, password)
        coEvery { userRepository.getUserByEmail(email) } returns null

        // When
        val result = authService.loginUser(loginRequest)

        // Then
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
    }

    @Test
    fun `test login with locked account`() = runBlocking {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val hashedPassword = "hashed_password_123"

        val user = createTestUser(
            email = email,
            passwordHash = hashedPassword,
            status = AccountStatus.LOCKED,
            lastFailedLoginAttempt = LocalDateTime.now().minusMinutes(5)
        )

        val loginRequest = LoginRequest(email, password)
        coEvery { userRepository.getUserByEmail(email) } returns user

        // When
        val result = authService.loginUser(loginRequest)

        // Then
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
    }

    @Test
    fun `test successful registration`() = runBlocking {
        // Given
        val email = "newuser@example.com"
        val password = "password123"
        val firstName = "New"
        val lastName = "User"

        val createUserRequest = CreateUserRequest(
            email = email,
            password = password,
            firstName = firstName,
            lastName = lastName
        )

        val createdUser = createTestUser(
            email = email,
            firstName = firstName,
            lastName = lastName,
            status = AccountStatus.PENDING_VERIFICATION
        )

        coEvery { userRepository.getUserByEmail(email) } returns null
        coEvery { userRepository.createUser(any()) } returns createdUser
        coEvery { emailService.sendVerificationEmail(any(), any()) } returns Unit

        // When
        val result = authService.registerUser(createUserRequest)

        // Then
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals(email, user.email)
        assertEquals(AccountStatus.PENDING_VERIFICATION, user.status)
        coVerify { emailService.sendVerificationEmail(email, any()) }
    }

    @Test
    fun `test registration with existing email`() = runBlocking {
        // Given
        val email = "existing@example.com"
        val password = "password123"
        val firstName = "Existing"
        val lastName = "User"

        val createUserRequest = CreateUserRequest(
            email = email,
            password = password,
            firstName = firstName,
            lastName = lastName
        )

        val existingUser = createTestUser(
            email = email,
            firstName = firstName,
            lastName = lastName,
            status = AccountStatus.ACTIVE
        )

        coEvery { userRepository.getUserByEmail(email) } returns existingUser

        // When
        val result = authService.registerUser(createUserRequest)

        // Then
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { userRepository.createUser(any()) }
        coVerify(exactly = 0) { emailService.sendVerificationEmail(any(), any()) }
    }

    @Test
    fun `test successful token refresh`(): Unit = runBlocking {
        // Given
        val refreshToken = "valid_refresh_token"
        val userId = 1

        val user = createTestUser(
            id = userId,
            email = "test@example.com"
        )

        val refreshTokenRequest = RefreshTokenRequest(refreshToken)
        val storedToken = RefreshToken(
            id = 1,
            userId = userId,
            token = refreshToken,
            expiresAt = LocalDateTime.now().plusDays(7),
            isRevoked = false
        )

        coEvery { refreshTokenRepository.findByToken(refreshToken) } returns storedToken
        coEvery { userRepository.getUserById(userId) } returns user
        coEvery { refreshTokenRepository.revokeToken(refreshToken) } returns true
        coEvery { refreshTokenRepository.createToken(any(), any(), any()) } returns RefreshToken(
            id = 2,
            userId = userId,
            token = "new_token",
            expiresAt = LocalDateTime.now().plusDays(7),
            isRevoked = false
        )

        // When
        val result = authService.refreshToken(refreshTokenRequest)

        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertNotNull(response.token)
        assertNotNull(response.refreshToken)
    }

    @Test
    fun `test token refresh with invalid token`() = runBlocking {
        // Given
        val refreshToken = "invalid_refresh_token"
        val refreshTokenRequest = RefreshTokenRequest(refreshToken)

        coEvery { refreshTokenRepository.findByToken(refreshToken) } returns null

        // When
        val result = authService.refreshToken(refreshTokenRequest)

        // Then
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
    }

    @Test
    fun `test successful logout`() = runBlocking {
        // Given
        val refreshToken = "valid_refresh_token"

        coEvery { refreshTokenRepository.revokeToken(refreshToken) } returns true

        // When
        val result = authService.logout(refreshToken)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() ?: false)
        coVerify { refreshTokenRepository.revokeToken(refreshToken) }
    }

    @Test
    fun `test logout with invalid token`() = runBlocking {
        // Given
        val refreshToken = "invalid_refresh_token"

        coEvery { refreshTokenRepository.revokeToken(refreshToken) } returns false

        // When
        val result = authService.logout(refreshToken)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }
} 