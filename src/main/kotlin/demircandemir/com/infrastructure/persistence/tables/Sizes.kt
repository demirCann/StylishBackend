package demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object Sizes : IntIdTable("SIZES") {
    val sizeName = varchar("size_name", 20)
    val sizeType = varchar("size_type", 50)
}