package demircandemir.com.domain.repository

import demircandemir.com.domain.model.Address
import demircandemir.com.domain.model.User

interface UserRepository {
    // User operations
    suspend fun createUser(user: User): User
    suspend fun getUserById(id: Int): User?
    suspend fun getUserByEmail(email: String): User?
    suspend fun updateUser(user: User): User
    suspend fun deleteUser(id: Int)
    suspend fun getAllUsers(): List<User>

    // Address operations
    suspend fun createAddress(address: Address): Address
    suspend fun getAddressById(id: Int): Address?
    suspend fun getAddressesByUserId(userId: Int): List<Address>
    suspend fun updateAddress(address: Address): Address
    suspend fun deleteAddress(id: Int)
} 