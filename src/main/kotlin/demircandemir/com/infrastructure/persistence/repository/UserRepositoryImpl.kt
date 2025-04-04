package demircandemir.com.infrastructure.persistence.repository

import demircandemir.com.domain.model.Address
import demircandemir.com.domain.model.User
import demircandemir.com.domain.repository.UserRepository
import demircandemir.com.infrastructure.persistence.DatabaseFactory
import demircandemir.com.infrastructure.persistence.tables.Addresses
import demircandemir.com.infrastructure.persistence.tables.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory

class UserRepositoryImpl : UserRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun createUser(user: User): User = DatabaseFactory.dbQuery {
        logger.info("Creating user: $user")

        val insertStatement = Users.insert {
            it[email] = user.email
            it[password] = user.password
            it[firstName] = user.firstName
            it[lastName] = user.lastName
            it[phoneNumber] = user.phoneNumber
            it[registrationDate] = user.registrationDate
            it[isActive] = user.isActive
        }

        val userId = insertStatement[Users.id].value
        logger.info("Created user with ID: $userId")

        user.copy(id = userId)
    }

    override suspend fun getUserById(id: Int): User? = DatabaseFactory.dbQuery {
        Users.selectAll()
            .where { Users.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }

    override suspend fun getUserByEmail(email: String): User? = DatabaseFactory.dbQuery {
        Users.selectAll()
            .where { Users.email eq email }
            .map { it.toUser() }
            .singleOrNull()
    }

    override suspend fun updateUser(user: User): User = DatabaseFactory.dbQuery {
        Users.update({ Users.id eq user.id }) {
            it[firstName] = user.firstName
            it[lastName] = user.lastName
            it[email] = user.email
            it[password] = user.password
            it[phoneNumber] = user.phoneNumber
        }
        user
    }

    override suspend fun deleteUser(id: Int): Unit = DatabaseFactory.dbQuery {
        Users.deleteWhere { Users.id eq id }
    }

    override suspend fun getAllUsers(): List<User> = DatabaseFactory.dbQuery {
        Users.selectAll().map { it.toUser() }
    }

    override suspend fun createAddress(address: Address): Address = DatabaseFactory.dbQuery {
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

    override suspend fun getAddressById(id: Int): Address? = DatabaseFactory.dbQuery {
        Addresses.selectAll()
            .where { Addresses.id eq id }
            .map { it.toAddress() }
            .singleOrNull()
    }

    override suspend fun getAddressesByUserId(userId: Int): List<Address> = DatabaseFactory.dbQuery {
        Addresses.selectAll()
            .where { Addresses.userId eq userId }
            .map { it.toAddress() }
    }

    override suspend fun updateAddress(address: Address): Address = DatabaseFactory.dbQuery {
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

    override suspend fun deleteAddress(id: Int): Unit = DatabaseFactory.dbQuery {
        Addresses.deleteWhere { Addresses.id eq id }
    }

    private fun ResultRow.toUser() = User(
        id = this[Users.id].value,
        email = this[Users.email],
        password = this[Users.password],
        firstName = this[Users.firstName],
        lastName = this[Users.lastName],
        phoneNumber = this[Users.phoneNumber],
        registrationDate = this[Users.registrationDate],
        isActive = this[Users.isActive]
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