package demircandemir.com.infrastructure.persistence

import demircandemir.com.demircandemir.com.infrastructure.persistence.tables.Users
import demircandemir.com.domain.model.User
import demircandemir.com.domain.repository.UserRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

class UserRepositoryImpl : UserRepository {
    override suspend fun createUser(user: User): User = DatabaseFactory.dbQuery {
        Users.insert {
            it[email] = user.email
            it[password] = user.password
            it[firstName] = user.firstName
            it[lastName] = user.lastName
            it[phoneNumber] = user.phoneNumber
        }
        user
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
} 