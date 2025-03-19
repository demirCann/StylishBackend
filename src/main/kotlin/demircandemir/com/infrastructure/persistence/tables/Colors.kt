package demircandemir.com.demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object Colors : IntIdTable("COLOR") {
    val colorName = varchar("color_name", 50)
    val hexCode = varchar("hex_code", 7).nullable()
}