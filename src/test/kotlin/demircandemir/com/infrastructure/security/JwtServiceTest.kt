package demircandemir.com.infrastructure.security

import demircandemir.com.domain.model.AccountStatus
import demircandemir.com.domain.model.User
import demircandemir.com.domain.model.UserRole
import io.ktor.server.config.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtServiceTest {
    private lateinit var jwtConfig: JwtConfig

    @BeforeEach
    fun setup() {
        val config = ApplicationConfig("application-test.conf")
        jwtConfig = JwtConfig(config)
    }

    private fun createTestUser(
        id: Int = 1,
        email: String = "test@example.com",
        firstName: String = "Test",
        lastName: String = "User",
        role: UserRole = UserRole.CUSTOMER,
        status: AccountStatus = AccountStatus.ACTIVE,
        phoneNumber: String? = null
    ): User {
        return User(
            id = id,
            email = email,
            passwordHash = "hashed_password", // Not used by JWT generation/validation directly
            firstName = firstName,
            lastName = lastName,
            role = role,
            status = status,
            registrationDate = LocalDateTime.now(),
            phoneNumber = phoneNumber
        )
    }

    @Test
    fun `generateAccessToken should create a non-blank token`() {
        // Given
        val user = createTestUser()

        // When
        val token = jwtConfig.generateAccessToken(user)

        // Then
        assertNotNull(token)
        assertTrue(token.isNotBlank(), "Generated access token should not be blank")
    }

    @Test
    fun `validateToken should return correct claims for a valid access token`() {
        // Given
        val user = createTestUser(id = 5, email = "customer@shop.com", role = UserRole.CUSTOMER)
        val token = jwtConfig.generateAccessToken(user)

        // When
        val claims = jwtConfig.validateToken(token)

        // Then
        assertNotNull(claims, "Claims should not be null for a valid token")
        assertTrue(claims.isNotEmpty(), "Claims should not be empty for a valid token")
        assertEquals(user.id, claims["userId"], "User ID claim mismatch")
        assertEquals(user.email, claims["email"], "Email claim mismatch")
        assertEquals(user.role.name, claims["role"], "Role claim mismatch")
    }

    @Test
    fun `validateToken should return correct claims for an ADMIN user access token`() {
        // Given
        val adminUser = createTestUser(id = 10, email = "admin@shop.com", role = UserRole.ADMIN)
        val token = jwtConfig.generateAccessToken(adminUser)

        // When
        val claims = jwtConfig.validateToken(token)

        // Then
        assertNotNull(claims, "Claims should not be null for a valid admin token")
        assertTrue(claims.isNotEmpty(), "Claims should not be empty for a valid admin token")
        assertEquals(adminUser.id, claims["userId"], "Admin User ID claim mismatch")
        assertEquals(adminUser.email, claims["email"], "Admin Email claim mismatch")
        assertEquals(adminUser.role.name, claims["role"], "Admin Role claim mismatch")
    }

    @Test
    fun `validateToken should return empty claims for a token with invalid signature`() {
        // Given
        // A token generated with the correct structure but signed with a different secret
        val validTokenStructure = jwtConfig.generateAccessToken(createTestUser())
        val parts = validTokenStructure.split('.')
        // An invalid signature (just an example, could be anything that's not the correct signature)
        val invalidSignature = "invalid_signature_string_that_is_long_enough"
        val invalidToken = "${parts[0]}.${parts[1]}.$invalidSignature"

        assertNotEquals(validTokenStructure, invalidToken, "Tokens should differ")

        // When
        val claims = jwtConfig.validateToken(invalidToken)

        // Then
        assertTrue(claims.isEmpty(), "Claims should be empty for a token with an invalid signature")
    }

    @Test
    fun `generateRefreshToken should create a non-blank token`() {
        // Given
        val user = createTestUser()

        // When
        val token = jwtConfig.generateRefreshToken(user)

        // Then
        assertNotNull(token)
        assertTrue(token.isNotBlank(), "Generated refresh token should not be blank")
    }

    @Test
    fun `validateToken should return correct claims for a valid refresh token`() {
        // Given
        val user = createTestUser(id = 7)
        val token = jwtConfig.generateRefreshToken(user)

        // When
        val claims = jwtConfig.validateToken(token)

        // Then
        assertNotNull(claims, "Claims should not be null for a valid refresh token")
        assertTrue(claims.isNotEmpty(), "Claims should not be empty for a valid refresh token")
        assertEquals(user.id, claims["userId"], "User ID claim mismatch in refresh token")
    }

    @Test
    fun `validateToken should return empty claims for an invalid refresh token string`() {
        // Given
        val invalidToken = "this-is-not-a-jwt-refresh-token"

        // When
        val claims = jwtConfig.validateToken(invalidToken)

        // Then
        assertTrue(claims.isEmpty(), "Claims should be empty for a completely invalid refresh token string")
    }
} 