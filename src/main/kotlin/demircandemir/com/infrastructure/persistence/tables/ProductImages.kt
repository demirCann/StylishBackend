package demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object ProductImages : IntIdTable("PRODUCT_IMAGES") {
    val productId = reference("product_id", Products)
    val imageUrl = varchar("image_url", 255)
    val isPrimary = bool("is_primary").default(false)
    val displayOrder = integer("display_order").default(0)

    init {
        index(false, productId)
    }
}