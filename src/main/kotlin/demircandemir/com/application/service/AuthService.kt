package demircandemir.com.application.service

import demircandemir.com.application.dto.*
import demircandemir.com.domain.model.AccountStatus
import demircandemir.com.domain.model.User
import demircandemir.com.domain.model.UserRole
import demircandemir.com.domain.repository.RefreshTokenRepository
import demircandemir.com.domain.repository.UserRepository
import demircandemir.com.infrastructure.security.JwtConfig
import io.ktor.server.plugins.*
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*

class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtConfig: JwtConfig,
    private val emailService: EmailService
) {
    companion object {
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val ACCOUNT_LOCK_TIME_MINUTES = 15L
    }

    /**
     * Register a new user
     */
    suspend fun registerUser(request: CreateUserRequest): Result<User> {
        return try {
            // Check if email is already in use
            val existingUser = userRepository.getUserByEmail(request.email)
            if (existingUser != null) {
                return Result.failure(BadRequestException("Email is already in use"))
            }

            // Hash the password
            val passwordHash = hashPassword(request.password)

            // Generate verification token
            val verificationToken = generateRandomToken()

            // Create user with pending verification status
            // Role is a constant since it's not in CreateUserRequest
            val role = UserRole.CUSTOMER

            val user = User(
                id = 0,
                email = request.email,
                passwordHash = passwordHash,
                firstName = request.firstName,
                lastName = request.lastName,
                phoneNumber = request.phoneNumber,
                role = role,
                status = AccountStatus.PENDING_VERIFICATION,
                verificationToken = verificationToken,
                registrationDate = LocalDateTime.now()
            )

            val createdUser = userRepository.createUser(user)

            // Send verification email
            emailService.sendVerificationEmail(user.email, verificationToken)

            Result.success(createdUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login user and return JWT token
     */
    suspend fun loginUser(request: LoginRequest): Result<LoginResponse> {
        return try {
            val user = userRepository.getUserByEmail(request.email)
                ?: return Result.failure(BadRequestException("Invalid email or password"))

            // Check if account is locked
            if (user.status == AccountStatus.LOCKED) {
                val lockTime = user.lastFailedLoginAttempt
                if (lockTime != null &&
                    LocalDateTime.now().isBefore(lockTime.plusMinutes(ACCOUNT_LOCK_TIME_MINUTES))
                ) {
                    return Result.failure(BadRequestException("Account is locked due to too many failed attempts. Try again later."))
                }
                // If lock time has expired, proceed with login but reset the account status if successful
            }

            // Verify account is active
            if (user.status != AccountStatus.ACTIVE && user.status != AccountStatus.LOCKED) {
                return when (user.status) {
                    AccountStatus.PENDING_VERIFICATION ->
                        Result.failure(BadRequestException("Please verify your email to activate your account"))

                    AccountStatus.INACTIVE ->
                        Result.failure(BadRequestException("Account is disabled"))

                    else ->
                        Result.failure(BadRequestException("Account status does not allow login"))
                }
            }

            // Verify password
            if (!verifyPassword(request.password, user.passwordHash)) {
                // Increment failed login attempts
                val failedAttempts = user.failedLoginAttempts + 1
                val newStatus = if (failedAttempts >= MAX_FAILED_ATTEMPTS)
                    AccountStatus.LOCKED else user.status

                userRepository.updateLoginStats(user.id, true)
                if (newStatus == AccountStatus.LOCKED) {
                    userRepository.updateUserStatus(user.id, AccountStatus.LOCKED)
                }

                return Result.failure(BadRequestException("Invalid email or password"))
            }

            // Generate tokens
            val accessToken = jwtConfig.generateAccessToken(user)
            val refreshToken = jwtConfig.generateRefreshToken(user)

            // Store refresh token
            val expiryDate = LocalDateTime.now().plusDays(7)
            refreshTokenRepository.createToken(user.id, refreshToken, expiryDate)

            // Reset failed login attempts if account was previously locked but now unlocked
            if (user.status == AccountStatus.LOCKED) {
                userRepository.updateUserStatus(user.id, AccountStatus.ACTIVE)
            }

            // Update login stats
            userRepository.updateLoginStats(user.id, false)

            // Build response
            val userResponse = UserResponse(
                id = user.id,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                phoneNumber = user.phoneNumber,
                role = user.role.name,
                status = user.status.name,
                createdAt = user.registrationDate.toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
                updatedAt = user.registrationDate.toEpochSecond(java.time.ZoneOffset.UTC) * 1000
            )

            val response = LoginResponse(
                token = accessToken,
                user = userResponse
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh token and generate a new access token
     */
    suspend fun refreshToken(request: RefreshTokenRequest): Result<RefreshTokenResponse> {
        return try {
            val storedToken = refreshTokenRepository.findByToken(request.refreshToken)
                ?: return Result.failure(BadRequestException("Invalid refresh token"))

            // Check if token is expired or revoked
            if (LocalDateTime.now().isAfter(storedToken.expiresAt) || storedToken.isRevoked) {
                return Result.failure(BadRequestException("Token expired or revoked"))
            }

            // Get user
            val user = userRepository.getUserById(storedToken.userId)
                ?: return Result.failure(BadRequestException("User not found"))

            // Check if user is active
            if (user.status != AccountStatus.ACTIVE) {
                return Result.failure(BadRequestException("User account is not active"))
            }

            // Generate new tokens
            val accessToken = jwtConfig.generateAccessToken(user)
            val newRefreshToken = jwtConfig.generateRefreshToken(user)

            // Revoke old token and store new one
            refreshTokenRepository.revokeToken(request.refreshToken)

            val expiryDate = LocalDateTime.now().plusDays(7)
            refreshTokenRepository.createToken(user.id, newRefreshToken, expiryDate)

            Result.success(
                RefreshTokenResponse(
                    token = accessToken,
                    refreshToken = newRefreshToken
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout user by revoking refresh token
     */
    suspend fun logout(token: String): Result<Boolean> {
        return try {
            val revoked = refreshTokenRepository.revokeToken(token)
            Result.success(revoked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verify email with token
     */
    suspend fun verifyEmail(request: VerifyEmailRequest): Result<Boolean> {
        return try {
            val verified = userRepository.verifyEmail(request.email, request.token)
            if (verified) {
                // Get user and update status
                val user = userRepository.getUserByEmail(request.email)
                if (user != null) {
                    userRepository.updateUserStatus(user.id, AccountStatus.ACTIVE)
                    return Result.success(true)
                }
            }
            Result.failure(BadRequestException("Invalid or expired verification token"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Request password reset
     */
    suspend fun requestPasswordReset(request: RequestPasswordResetRequest): Result<Boolean> {
        return try {
            // Generate token
            val token = generateRandomToken()
            val expiryDate = LocalDateTime.now().plusHours(1)

            // Store token
            val stored = userRepository.createPasswordResetToken(request.email, token, expiryDate)

            if (stored) {
                // Send email
                emailService.sendPasswordResetEmail(request.email, token)
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reset password with token
     */
    suspend fun resetPassword(request: ResetPasswordRequest): Result<Boolean> {
        return try {
            // Validate token
            val valid = userRepository.validatePasswordResetToken(request.email, request.token)
            if (!valid) {
                return Result.failure(BadRequestException("Invalid or expired reset token"))
            }

            // Get user
            val user = userRepository.getUserByEmail(request.email)
                ?: return Result.failure(BadRequestException("User not found"))

            // Hash new password
            val passwordHash = hashPassword(request.newPassword)

            // Update password
            val updated = userRepository.updatePassword(user.id, passwordHash)

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Hash password using SHA-256
     * Note: In a production environment, use a more secure hashing algorithm like BCrypt
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }

    /**
     * Verify password against hash
     */
    private fun verifyPassword(password: String, hash: String): Boolean {
        val calculatedHash = hashPassword(password)
        return calculatedHash == hash
    }

    /**
     * Generate a random token for verification or password reset
     */
    private fun generateRandomToken(): String {
        return UUID.randomUUID().toString()
    }
} 