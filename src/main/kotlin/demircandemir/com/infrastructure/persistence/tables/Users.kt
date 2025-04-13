package demircandemir.com.infrastructure.persistence.tables

import demircandemir.com.domain.model.AccountStatus
import demircandemir.com.domain.model.UserRole
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Users : IntIdTable("USERS") {
    val firstName = varchar("first_name", 50)
    val lastName = varchar("last_name", 50)
    val email = varchar("email", 100).uniqueIndex("idx_user_email_unique")
    val passwordHash = varchar("password_hash", 255)
    val phoneNumber = varchar("phone_number", 20).nullable()
    val role = enumerationByName("role", 20, UserRole::class).default(UserRole.CUSTOMER)
    val status = enumerationByName("status", 30, AccountStatus::class).default(AccountStatus.ACTIVE)
    val verificationToken = varchar("verification_token", 128).nullable()
    val passwordResetToken = varchar("password_reset_token", 128).nullable()
    val passwordResetTokenExpiry = datetime("password_reset_token_expiry").nullable()
    val failedLoginAttempts = integer("failed_login_attempts").default(0)
    val lastFailedLoginAttempt = datetime("last_failed_login_attempt").nullable()
    val registrationDate = datetime("registration_date").default(LocalDateTime.now())
    val lastLoginDate = datetime("last_login_date").nullable()

    init {
        index(isUnique = true, email)
    }
}