package demircandemir.com.infrastructure.persistence.repository

import demircandemir.com.domain.model.AccountStatus
import demircandemir.com.domain.model.Address
import demircandemir.com.domain.model.User
import demircandemir.com.domain.repository.UserRepository
import demircandemir.com.infrastructure.persistence.DatabaseFactory.dbQuery
import demircandemir.com.infrastructure.persistence.tables.Addresses
import demircandemir.com.infrastructure.persistence.tables.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class UserRepositoryImpl : UserRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun createUser(user: User): User = dbQuery {
        logger.info("Creating user: $user")

        val insertStatement = Users.insert {
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[firstName] = user.firstName
            it[lastName] = user.lastName
            it[phoneNumber] = user.phoneNumber
            it[role] = user.role
            it[status] = user.status
            it[verificationToken] = user.verificationToken
            it[passwordResetToken] = user.passwordResetToken
            it[passwordResetTokenExpiry] = user.passwordResetTokenExpiry
            it[failedLoginAttempts] = user.failedLoginAttempts
            it[lastFailedLoginAttempt] = user.lastFailedLoginAttempt
            it[registrationDate] = user.registrationDate
            it[lastLoginDate] = user.lastLoginDate
        }

        val userId = insertStatement[Users.id].value
        logger.info("Created user with ID: $userId")

        user.copy(id = userId)
    }

    override suspend fun getUserById(id: Int): User? = dbQuery {
        Users.selectAll()
            .where { Users.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }

    override suspend fun getUserByEmail(email: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.email eq email }
            .map { it.toUser() }
            .singleOrNull()
    }

    override suspend fun updateUser(user: User): User = dbQuery {
        Users.update({ Users.id eq user.id }) {
            it[firstName] = user.firstName
            it[lastName] = user.lastName
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[phoneNumber] = user.phoneNumber
            it[role] = user.role
            it[status] = user.status
            it[verificationToken] = user.verificationToken
            it[passwordResetToken] = user.passwordResetToken
            it[passwordResetTokenExpiry] = user.passwordResetTokenExpiry
            it[failedLoginAttempts] = user.failedLoginAttempts
            it[lastFailedLoginAttempt] = user.lastFailedLoginAttempt
            it[lastLoginDate] = user.lastLoginDate
        }
        user
    }

    override suspend fun deleteUser(id: Int): Unit = dbQuery {
        Users.deleteWhere { Users.id eq id }
    }

    override suspend fun getAllUsers(): List<User> = dbQuery {
        Users.selectAll().map { it.toUser() }
    }

    override suspend fun verifyEmail(email: String, token: String): Boolean = dbQuery {
        val user = Users.selectAll()
            .where { Users.email eq email }
            .map { it.toUser() }
            .singleOrNull() ?: return@dbQuery false

        if (user.verificationToken == token) {
            Users.update({ Users.id eq user.id }) {
                it[status] = AccountStatus.ACTIVE
                it[verificationToken] = null
            }
            true
        } else {
            false
        }
    }

    override suspend fun updatePassword(userId: Int, newPasswordHash: String): Boolean = dbQuery {
        val updatedRows = Users.update({ Users.id eq userId }) {
            it[passwordHash] = newPasswordHash
            it[passwordResetToken] = null
            it[passwordResetTokenExpiry] = null
        }
        updatedRows > 0
    }

    override suspend fun createPasswordResetToken(email: String, token: String, expiryDate: LocalDateTime): Boolean =
        dbQuery {
            val user = Users.selectAll()
                .where { Users.email eq email }
                .map { it.toUser() }
                .singleOrNull() ?: return@dbQuery false

            val updatedRows = Users.update({ Users.id eq user.id }) {
                it[passwordResetToken] = token
                it[passwordResetTokenExpiry] = expiryDate
            }

            updatedRows > 0
        }

    override suspend fun validatePasswordResetToken(email: String, token: String): Boolean = dbQuery {
        val user = Users.selectAll()
            .where { Users.email eq email }
            .map { it.toUser() }
            .singleOrNull() ?: return@dbQuery false

        user.passwordResetToken == token &&
                user.passwordResetTokenExpiry != null &&
                user.passwordResetTokenExpiry.isAfter(LocalDateTime.now())
    }

    override suspend fun updateLoginStats(userId: Int, failedAttempt: Boolean): Boolean = dbQuery {
        if (failedAttempt) {
            val user = Users.selectAll()
                .where { Users.id eq userId }
                .map { it.toUser() }
                .singleOrNull() ?: return@dbQuery false

            val newFailedAttempts = user.failedLoginAttempts + 1

            val status = if (newFailedAttempts >= 5) AccountStatus.LOCKED else user.status

            Users.update({ Users.id eq userId }) {
                it[failedLoginAttempts] = newFailedAttempts
                it[lastFailedLoginAttempt] = LocalDateTime.now()
                it[Users.status] = status
            }
        } else {
            Users.update({ Users.id eq userId }) {
                it[failedLoginAttempts] = 0
                it[lastLoginDate] = LocalDateTime.now()
            }
        }
        true
    }

    override suspend fun updateUserStatus(userId: Int, status: AccountStatus): Boolean = dbQuery {
        val updatedRows = Users.update({ Users.id eq userId }) {
            it[Users.status] = status
        }
        updatedRows > 0
    }

    override suspend fun createAddress(address: Address): Address = dbQuery {
        val id = Addresses.insert {
            it[userId] = address.userId
            it[addressTitle] = address.addressTitle
            it[this.address] = address.address
            it[city] = address.city
            it[district] = address.district
            it[postalCode] = address.postalCode
            it[country] = address.country
        } get Addresses.id

        address.copy(id = id.value)
    }

    override suspend fun getAddressById(id: Int): Address? = dbQuery {
        Addresses.selectAll()
            .where { Addresses.id eq id }
            .map { it.toAddress() }
            .singleOrNull()
    }

    override suspend fun getAddressesByUserId(userId: Int): List<Address> = dbQuery {
        Addresses.selectAll()
            .where { Addresses.userId eq userId }
            .map { it.toAddress() }
    }

    override suspend fun updateAddress(address: Address): Address = dbQuery {
        Addresses.update({ Addresses.id eq address.id }) {
            it[addressTitle] = address.addressTitle
            it[this.address] = address.address
            it[city] = address.city
            it[district] = address.district
            it[postalCode] = address.postalCode
            it[country] = address.country
        }
        address
    }

    override suspend fun deleteAddress(id: Int): Unit = dbQuery {
        Addresses.deleteWhere { Addresses.id eq id }
    }

    private fun ResultRow.toUser() = User(
        id = this[Users.id].value,
        email = this[Users.email],
        passwordHash = this[Users.passwordHash],
        firstName = this[Users.firstName],
        lastName = this[Users.lastName],
        phoneNumber = this[Users.phoneNumber],
        role = this[Users.role],
        status = this[Users.status],
        verificationToken = this[Users.verificationToken],
        passwordResetToken = this[Users.passwordResetToken],
        passwordResetTokenExpiry = this[Users.passwordResetTokenExpiry],
        failedLoginAttempts = this[Users.failedLoginAttempts],
        lastFailedLoginAttempt = this[Users.lastFailedLoginAttempt],
        registrationDate = this[Users.registrationDate],
        lastLoginDate = this[Users.lastLoginDate]
    )

    private fun ResultRow.toAddress() = Address(
        id = this[Addresses.id].value,
        userId = this[Addresses.userId].value,
        addressTitle = this[Addresses.addressTitle],
        address = this[Addresses.address],
        city = this[Addresses.city],
        district = this[Addresses.district],
        postalCode = this[Addresses.postalCode],
        country = this[Addresses.country]
    )
} 