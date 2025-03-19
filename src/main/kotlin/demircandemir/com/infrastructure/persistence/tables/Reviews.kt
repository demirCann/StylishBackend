package demircandemir.com.demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Reviews : IntIdTable("REVIEW") {
    val productId = reference("product_id", Products)
    val userId = reference("user_id", Users)
    val rating = integer("rating").check { it.between(1, 5) }
    val reviewText = text("review_text").nullable()
    val reviewDate = datetime("review_date").default(LocalDateTime.now())
    val isApproved = bool("is_approved").default(false)

    init {
        index(false, productId)
        index(false, userId)
        index(false, isApproved)
    }
}