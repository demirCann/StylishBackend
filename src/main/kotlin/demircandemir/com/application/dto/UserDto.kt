package demircandemir.com.application.dto

import kotlinx.serialization.Serializable

// --- User Registration and Profile ---
@Serializable
data class CreateUserRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String? = null
)

@Serializable
data class UpdateUserRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null
)

@Serializable
data class UserResponse(
    val id: Int,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?,
    val role: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)

// --- Authentication ---
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserResponse
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    val token: String,
    val refreshToken: String
)

// --- Password Reset ---
@Serializable
data class RequestPasswordResetRequest(
    val email: String
)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val email: String,
    val newPassword: String
)

// --- Email Verification ---
@Serializable
data class VerifyEmailRequest(
    val token: String,
    val email: String
)

// --- Two-Factor Authentication ---
@Serializable
data class Setup2FARequest(
    val enable: Boolean
)

@Serializable
data class Verify2FARequest(
    val code: String
)

@Serializable
data class Generate2FAResponse(
    val secretKey: String,
    val qrCodeUrl: String
)

// --- Generic Responses ---
@Serializable
data class MessageResponse(
    val message: String
) 