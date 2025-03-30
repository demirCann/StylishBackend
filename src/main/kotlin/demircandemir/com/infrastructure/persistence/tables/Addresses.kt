package demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object Addresses : IntIdTable("ADDRESSES") {
    val userId = reference("user_id", Users)
    val addressTitle = varchar("address_title", 50)
    val address = varchar("address", 255)
    val city = varchar("city", 50)
    val district = varchar("district", 50)
    val postalCode = varchar("postal_code", 20).nullable()
    val country = varchar("country", 50).default("Turkey")

    init {
        index(false, userId)
    }
}