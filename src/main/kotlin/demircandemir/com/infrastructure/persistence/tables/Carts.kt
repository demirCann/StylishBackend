package demircandemir.com.demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Carts : IntIdTable("CART") {
    val userId = reference("user_id", Users)
    val totalAmount = decimal("total_amount", 10, 2).default(0.toBigDecimal())
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())

    init {
        index(false, userId)
    }
}