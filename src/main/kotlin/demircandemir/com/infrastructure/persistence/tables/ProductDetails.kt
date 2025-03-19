package demircandemir.com.demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object ProductDetails : IntIdTable("PRODUCT_DETAIL") {
    val productId = reference("product_id", Products)
    val color = varchar("color", 50).nullable()
    val size = varchar("size", 20).nullable()
    val material = varchar("material", 100).nullable()
    val madeIn = varchar("made_in", 100).nullable()
    val careInstructions = text("care_instructions").nullable()
    val gender = enumerationByName("gender", 10, Gender::class)
    val season = varchar("season", 50).nullable()

    init {
        index(false, productId)
    }
}

enum class Gender {
    Male, Female, Unisex
}
