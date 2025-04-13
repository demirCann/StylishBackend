package demircandemir.com.domain.repository

import demircandemir.com.domain.model.RefreshToken
import java.time.LocalDateTime

interface RefreshTokenRepository {
    suspend fun createToken(userId: Int, token: String, expiresAt: LocalDateTime): RefreshToken
    suspend fun findByToken(token: String): RefreshToken?
    suspend fun revokeToken(token: String): Boolean
    suspend fun revokeAllUserTokens(userId: Int): Boolean
    suspend fun deleteExpiredTokens(): Int
} 