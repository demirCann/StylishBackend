package demircandemir.com.domain.model

import java.time.LocalDateTime

enum class UserRole {
    CUSTOMER, ADMIN
}

enum class AccountStatus {
    ACTIVE, INACTIVE, LOCKED, PENDING_VERIFICATION
}

data class User(
    val id: Int,
    val email: String,
    val passwordHash: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?,
    val role: UserRole = UserRole.CUSTOMER,
    val status: AccountStatus = AccountStatus.ACTIVE,
    val verificationToken: String? = null,
    val passwordResetToken: String? = null,
    val passwordResetTokenExpiry: LocalDateTime? = null,
    val failedLoginAttempts: Int = 0,
    val lastFailedLoginAttempt: LocalDateTime? = null,
    val registrationDate: LocalDateTime,
    val lastLoginDate: LocalDateTime? = null
) 