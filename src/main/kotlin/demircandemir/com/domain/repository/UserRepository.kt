package demircandemir.com.domain.repository

import demircandemir.com.domain.model.User
import java.util.UUID

interface UserRepository {
    suspend fun createUser(user: User): User
    suspend fun getUserById(id: Int): User?
    suspend fun getUserByEmail(email: String): User?
    suspend fun updateUser(user: User): User
    suspend fun deleteUser(id: Int)
    suspend fun getAllUsers(): List<User>
} 