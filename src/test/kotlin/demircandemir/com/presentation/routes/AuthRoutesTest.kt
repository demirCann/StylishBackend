package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.*
import demircandemir.com.application.serialization.appSerializersModule
import demircandemir.com.application.service.AuthService
import demircandemir.com.application.service.EmailService
import demircandemir.com.domain.model.AccountStatus
import demircandemir.com.domain.model.User
import demircandemir.com.domain.model.UserRole
import demircandemir.com.domain.repository.RefreshTokenRepository
import demircandemir.com.domain.repository.UserRepository
import demircandemir.com.infrastructure.security.JwtConfig
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRoutesTest {
    private val mockUserRepository: UserRepository = mockk()
    private val mockRefreshTokenRepository: RefreshTokenRepository = mockk()
    private val mockEmailService: EmailService = mockk(relaxed = true)
    private val mockJwtConfig: JwtConfig = mockk(relaxed = true)
    private val mockAuthService: AuthService = mockk()

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        serializersModule = appSerializersModule
    }

    // Application extension function to set up test module
    private fun Application.testModule() {
        install(ContentNegotiation) {
            json(json)
        }

        // Test için authentication yapılandırması
        install(Authentication) {
            basic("auth-jwt") {
                validate { credentials ->
                    // Test için her zaman başarılı doğrulama
                    UserIdPrincipal("test-user")
                }
            }
        }

        routing {
            authRoutes(mockAuthService)
        }
    }

    // Test helper function to create test user instances
    private fun createTestUser(
        id: Int = 1,
        email: String = "test@example.com",
        firstName: String = "Test",
        lastName: String = "User",
        role: UserRole = UserRole.CUSTOMER,
        status: AccountStatus = AccountStatus.ACTIVE,
        passwordHash: String = "hashed_password_123",
        phoneNumber: String? = null,
        verificationToken: String? = "verify-token",
        passwordResetToken: String? = "reset-token",
        failedLoginAttempts: Int = 0,
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
            lastLoginDate = null,
            failedLoginAttempts = failedLoginAttempts,
            lastFailedLoginAttempt = lastFailedLoginAttempt,
            verificationToken = verificationToken,
            passwordResetToken = passwordResetToken,
            passwordResetTokenExpiry = null
        )
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            mockUserRepository,
            mockRefreshTokenRepository,
            mockEmailService,
            mockAuthService,
            mockJwtConfig,
            answers = false
        )
    }

    // --- Login Tests ---
    @Test
    fun `test successful login`() {
        testApplication {
            application {
                testModule()
            }

            val password = "password123"
            val user = createTestUser(passwordHash = "correct_hash_for_password123")
            val loginRequest = LoginRequest(email = user.email, password = password)

            val userResponse = UserResponse(
                id = user.id,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                phoneNumber = user.phoneNumber,
                role = user.role.name,
                status = user.status.name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val accessToken = "generated_access_token"
            // Based on the original LoginResponse class structure
            val loginResponse = LoginResponse(
                token = accessToken,
                user = userResponse
            )

            // Mock AuthService loginUser interaction
            coEvery { mockAuthService.loginUser(any()) } returns Result.success(loginResponse)

            // Configure client with ContentNegotiation
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When
            val response = client.post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(loginRequest)
            }

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains(accessToken))
            assertTrue(responseBody.contains(user.email))

            coVerify {
                mockAuthService.loginUser(match { req ->
                    req.email == loginRequest.email && req.password == loginRequest.password
                })
            }
        }
    }

    @Test
    fun `test login with invalid credentials`() {
        testApplication {
            application {
                testModule()
            }

            val loginRequest = LoginRequest(email = "user@example.com", password = "wrongPassword")

            // Mock AuthService failure for wrong password
            coEvery { mockAuthService.loginUser(any()) } returns Result.failure(IllegalArgumentException("Invalid email or password"))

            // Configure client
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When
            val response = client.post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(loginRequest)
            }

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)

            coVerify {
                mockAuthService.loginUser(match { req ->
                    req.email == loginRequest.email && req.password == loginRequest.password
                })
            }
        }
    }

    // --- Registration Tests ---
    @Test
    fun `test successful registration`() {
        testApplication {
            application {
                testModule()
            }

            // Create registration request
            val request = CreateUserRequest(
                email = "newuser@example.com",
                password = "password123",
                firstName = "New",
                lastName = "User",
                phoneNumber = null
            )

            val registeredUser = createTestUser(
                id = 5,
                email = request.email,
                firstName = request.firstName,
                lastName = request.lastName,
                status = AccountStatus.PENDING_VERIFICATION
            )

            // Mock AuthService registerUser interaction
            coEvery { mockAuthService.registerUser(any()) } returns Result.success(registeredUser)

            // Configure client with ContentNegotiation
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When
            val response = client.post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Then
            assertEquals(HttpStatusCode.Created, response.status)

            coVerify {
                mockAuthService.registerUser(match { req ->
                    req.email == request.email &&
                            req.password == request.password &&
                            req.firstName == request.firstName &&
                            req.lastName == request.lastName
                })
            }
        }
    }

    @Test
    fun `test registration with existing email`() {
        testApplication {
            application {
                testModule()
            }

            val request = CreateUserRequest(
                email = "existing@example.com",
                password = "password123",
                firstName = "Existing",
                lastName = "User",
                phoneNumber = null
            )

            // Mock AuthService failure for existing email
            coEvery { mockAuthService.registerUser(any()) } returns Result.failure(IllegalArgumentException("Email is already in use"))

            // Configure client
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When
            val response = client.post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)

            coVerify {
                mockAuthService.registerUser(match { req ->
                    req.email == request.email &&
                            req.password == request.password
                })
            }
        }
    }

    // --- Token Refresh Tests ---
    @Test
    fun `test successful token refresh`() {
        testApplication {
            application {
                testModule()
            }

            // Create request
            val request = RefreshTokenRequest(refreshToken = "valid_refresh_token")
            val newAccessToken = "refreshed_access_token"
            val newRefreshToken = "refreshed_refresh_token"

            // Create expected response
            val tokenResponse = RefreshTokenResponse(
                token = newAccessToken,
                refreshToken = newRefreshToken
            )

            // Mock AuthService refreshToken interaction
            coEvery { mockAuthService.refreshToken(any()) } returns Result.success(tokenResponse)

            // Configure client with ContentNegotiation
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When
            val httpResponse = client.post("/api/auth/refresh-token") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Then
            assertEquals(HttpStatusCode.OK, httpResponse.status)
            val responseBody = httpResponse.bodyAsText()
            assertTrue(responseBody.contains(newAccessToken))
            assertTrue(responseBody.contains(newRefreshToken))

            coVerify {
                mockAuthService.refreshToken(match { req ->
                    req.refreshToken == request.refreshToken
                })
            }
        }
    }

    @Test
    fun `test token refresh with invalid token`() {
        testApplication {
            application {
                testModule()
            }

            val request = RefreshTokenRequest(refreshToken = "invalid_refresh_token")

            // Mock AuthService failure
            coEvery { mockAuthService.refreshToken(any()) } returns Result.failure(IllegalArgumentException("Invalid refresh token"))

            // Configure client
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When
            val response = client.post("/api/auth/refresh-token") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)

            coVerify {
                mockAuthService.refreshToken(match { req ->
                    req.refreshToken == request.refreshToken
                })
            }
        }
    }

    // --- Logout Tests ---
    @Test
    fun `test successful logout`() {
        testApplication {
            application {
                testModule()
            }

            val tokenValue = "valid_token_to_logout"
            val logoutRequest = RefreshTokenRequest(refreshToken = tokenValue)

            // Mock AuthService logout interaction
            coEvery { mockAuthService.logout(tokenValue) } returns Result.success(true)

            // Configure client with ContentNegotiation
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When - Basic Auth kullanıyoruz
            val response = client.post("/api/auth/logout") {
                contentType(ContentType.Application.Json)
                basicAuth("test-user", "test-password") // JWT header yerine basic auth
                setBody(logoutRequest)
            }

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { mockAuthService.logout(tokenValue) }
        }
    }

    @Test
    fun `test logout with invalid token`() {
        testApplication {
            application {
                testModule()
            }

            val tokenValue = "invalid_token"
            val logoutRequest = RefreshTokenRequest(refreshToken = tokenValue)

            // Mock AuthService failure
            coEvery { mockAuthService.logout(tokenValue) } returns Result.failure(IllegalArgumentException("Invalid refresh token"))

            // Configure client
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When - Basic Auth kullanıyoruz
            val response = client.post("/api/auth/logout") {
                contentType(ContentType.Application.Json)
                header(
                    HttpHeaders.Authorization,
                    "Basic " + Base64.getEncoder().encodeToString("test-user:test-password".toByteArray())
                )
                setBody(logoutRequest)
            }

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)
            coVerify { mockAuthService.logout(tokenValue) }
        }
    }

    // Yardımcı extension function
    private fun HttpRequestBuilder.basicAuth(username: String, password: String) {
        header(
            HttpHeaders.Authorization,
            "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        )
    }

    @Test
    fun `test me endpoint`() {
        testApplication {
            application {
                testModule()
            }

            // Configure client with ContentNegotiation
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When - Basic Auth kullanıyoruz
            val response = client.get("/api/auth/me") {
                basicAuth("test-user", "test-password") // JWT header yerine basic auth
            }

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `test verify email with valid token`() {
        testApplication {
            application {
                testModule()
            }

            val email = "user@example.com"
            val token = "valid_verification_token"

            // Mock AuthService verifyEmail interaction
            coEvery {
                mockAuthService.verifyEmail(VerifyEmailRequest(token = token, email = email))
            } returns Result.success(true)

            // Configure client
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When
            val response = client.get("/api/auth/verify-email") {
                url {
                    parameters.append("token", token)
                    parameters.append("email", email)
                }
            }

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { mockAuthService.verifyEmail(VerifyEmailRequest(token = token, email = email)) }
        }
    }

    @Test
    fun `test verify email with invalid token`() {
        testApplication {
            application {
                testModule()
            }

            val email = "user@example.com"
            val token = "invalid_token"

            // Mock AuthService verifyEmail failure
            coEvery {
                mockAuthService.verifyEmail(VerifyEmailRequest(token = token, email = email))
            } returns Result.failure(BadRequestException("Invalid or expired verification token"))

            // Configure client
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When
            val response = client.get("/api/auth/verify-email") {
                url {
                    parameters.append("token", token)
                    parameters.append("email", email)
                }
            }

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)
            coVerify { mockAuthService.verifyEmail(VerifyEmailRequest(token = token, email = email)) }
        }
    }

    @Test
    fun `test request password reset successfully`() {
        testApplication {
            application {
                testModule()
            }

            val email = "user@example.com"
            val request = RequestPasswordResetRequest(email = email)

            // Mock AuthService requestPasswordReset interaction
            coEvery { mockAuthService.requestPasswordReset(request) } returns Result.success(true)

            // Configure client
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When
            val response = client.post("/api/auth/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { mockAuthService.requestPasswordReset(request) }
        }
    }

    @Test
    fun `test request password reset for non-existing user`() {
        testApplication {
            application {
                testModule()
            }

            val email = "nonexisting@example.com"
            val request = RequestPasswordResetRequest(email = email)

            // Mock AuthService requestPasswordReset failure
            coEvery { mockAuthService.requestPasswordReset(request) } returns Result.success(true) // Always return success for security

            // Configure client
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When
            val response = client.post("/api/auth/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Then
            assertEquals(HttpStatusCode.OK, response.status) // Always return OK for security
            coVerify { mockAuthService.requestPasswordReset(request) }
        }
    }

    @Test
    fun `test reset password successfully`() {
        testApplication {
            application {
                testModule()
            }

            val request = ResetPasswordRequest(
                email = "user@example.com",
                token = "valid_token",
                newPassword = "NewPassword123"
            )

            // Mock AuthService resetPassword interaction
            coEvery { mockAuthService.resetPassword(request) } returns Result.success(true)

            // Configure client
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When
            val response = client.post("/api/auth/reset-password") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { mockAuthService.resetPassword(request) }
        }
    }

    @Test
    fun `test reset password with invalid token`() {
        testApplication {
            application {
                testModule()
            }

            val request = ResetPasswordRequest(
                email = "user@example.com",
                token = "invalid_token",
                newPassword = "NewPassword123"
            )

            // Mock AuthService resetPassword failure
            coEvery { mockAuthService.resetPassword(request) } returns Result.failure(BadRequestException("Invalid or expired token"))

            // Configure client
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(json)
                }
            }

            // When
            val response = client.post("/api/auth/reset-password") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)
            coVerify { mockAuthService.resetPassword(request) }
        }
    }
}

// TODO: Ensure Koin modules in test setup correctly override production modules.
// TODO: Define proper Request/Response DTOs instead of inline data classes if not already done. 