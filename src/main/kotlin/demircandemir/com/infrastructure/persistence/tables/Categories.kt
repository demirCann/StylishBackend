package demircandemir.com.demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object Categories : IntIdTable("CATEGORY") {
    val categoryName = varchar("category_name", 100)
    val description = text("description").nullable()
    val parentCategoryId = reference("parent_category_id", Categories).nullable()

    init {
        index(false, parentCategoryId)
    }
}