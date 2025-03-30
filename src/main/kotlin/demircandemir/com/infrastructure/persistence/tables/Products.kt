package demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Products : IntIdTable("PRODUCTS") {
    val productName = varchar("product_name", 255)
    val description = text("description").nullable()
    val price = decimal("price", 10, 2)
    val stockQuantity = integer("stock_quantity").default(0)
    val categoryId = reference("category_id", Categories).nullable()
    val brand = varchar("brand", 100).nullable()
    val dateAdded = datetime("date_added").default(LocalDateTime.now())
    val isActive = bool("is_active").default(true)

    init {
        index(false, categoryId)
        index(false, productName)
        index(false, isActive)
    }
}