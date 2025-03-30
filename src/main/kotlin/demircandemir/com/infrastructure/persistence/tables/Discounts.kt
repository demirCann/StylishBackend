package demircandemir.com.demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Discounts : IntIdTable("DISCOUNTS") {
    val discountCode = varchar("discount_code", 50).uniqueIndex()
    val discountRate = decimal("discount_rate", 5, 2)
    val minCartAmount = decimal("min_cart_amount", 10, 2).default(0.toBigDecimal())
    val startDate = datetime("start_date").default(LocalDateTime.now())
    val endDate = datetime("end_date").nullable()
    val usageLimit = integer("usage_limit").nullable()
    val usageCount = integer("usage_count").default(0)

    init {
        index(false, discountCode)
        index(false, startDate, endDate)
    }
}