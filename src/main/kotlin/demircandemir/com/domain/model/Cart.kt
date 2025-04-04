package demircandemir.com.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Cart(
    val id: Int = 0,
    val userId: Int,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)