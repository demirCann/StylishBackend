package demircandemir.com.application.dto

import kotlinx.serialization.Serializable

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
    val createdAt: Long,
    val updatedAt: Long
) 