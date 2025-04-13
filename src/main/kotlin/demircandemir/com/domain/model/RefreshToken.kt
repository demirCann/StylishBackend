package demircandemir.com.domain.model

import java.time.LocalDateTime

data class RefreshToken(
    val id: Int = 0,
    val userId: Int,
    val token: String,
    val expiresAt: LocalDateTime,
    val isRevoked: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
) 