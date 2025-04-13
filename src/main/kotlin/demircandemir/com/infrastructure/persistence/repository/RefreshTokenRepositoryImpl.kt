package demircandemir.com.infrastructure.persistence.repository

import demircandemir.com.domain.model.RefreshToken
import demircandemir.com.domain.repository.RefreshTokenRepository
import demircandemir.com.infrastructure.persistence.tables.RefreshTokens
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime

class RefreshTokenRepositoryImpl : RefreshTokenRepository {

    override suspend fun createToken(userId: Int, token: String, expiresAt: LocalDateTime): RefreshToken =
        newSuspendedTransaction {
            val id = RefreshTokens.insert {
                it[RefreshTokens.userId] = userId
                it[RefreshTokens.token] = token
                it[RefreshTokens.expiresAt] = expiresAt
                it[isRevoked] = false
            } get RefreshTokens.id

            RefreshToken(
                id = id.value,
                userId = userId,
                token = token,
                expiresAt = expiresAt,
                isRevoked = false,
                createdAt = LocalDateTime.now()
            )
        }

    override suspend fun findByToken(token: String): RefreshToken? = newSuspendedTransaction {
        RefreshTokens.selectAll()
            .where { RefreshTokens.token eq token }
            .singleOrNull()
            ?.let { row ->
                RefreshToken(
                    id = row[RefreshTokens.id].value,
                    userId = row[RefreshTokens.userId],
                    token = row[RefreshTokens.token],
                    expiresAt = row[RefreshTokens.expiresAt],
                    isRevoked = row[RefreshTokens.isRevoked],
                    createdAt = row[RefreshTokens.createdAt]
                )
            }
    }

    override suspend fun revokeToken(token: String): Boolean = newSuspendedTransaction {
        RefreshTokens.update({ RefreshTokens.token eq token }) {
            it[isRevoked] = true
        } > 0
    }

    override suspend fun revokeAllUserTokens(userId: Int): Boolean = newSuspendedTransaction {
        RefreshTokens.update({ RefreshTokens.userId eq userId }) {
            it[isRevoked] = true
        } > 0
    }

    override suspend fun deleteExpiredTokens(): Int = newSuspendedTransaction {
        RefreshTokens.deleteWhere {
            (expiresAt.less(LocalDateTime.now())) or isRevoked
        }
    }
} 