package demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object RefreshTokens : IntIdTable("REFRESH_TOKENS") {
    val userId = integer("user_id").references(Users.id)
    val token = varchar("token", 255)
    val expiresAt = datetime("expires_at")
    val isRevoked = bool("is_revoked").default(false)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    init {
        index(isUnique = true, token)
        index(false, userId)
    }
} 