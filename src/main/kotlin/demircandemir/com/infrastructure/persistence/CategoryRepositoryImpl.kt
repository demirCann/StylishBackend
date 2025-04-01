package demircandemir.com.infrastructure.persistence

import demircandemir.com.domain.model.Category
import demircandemir.com.domain.repository.CategoryRepository
import demircandemir.com.infrastructure.persistence.tables.Categories
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory

class CategoryRepositoryImpl : CategoryRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun createCategory(category: Category): Category = DatabaseFactory.dbQuery {
        logger.info("Creating category: ${category.categoryName}")

        val insertStatement = Categories.insert {
            it[categoryName] = category.categoryName
            it[description] = category.description
            it[parentCategoryId] = category.parentCategoryId
        }

        val categoryId = insertStatement[Categories.id].value
        logger.info("Created category with ID: $categoryId")

        category.copy(id = categoryId)
    }

    override suspend fun getCategoryById(id: Int): Category? = DatabaseFactory.dbQuery {
        Categories.selectAll()
            .where { Categories.id eq id }
            .map { it.toCategory() }
            .singleOrNull()
    }

    override suspend fun getCategoryByIdWithSubcategories(id: Int): Category? = DatabaseFactory.dbQuery {
        val category = Categories.selectAll()
            .where { Categories.id eq id }
            .map { it.toCategory() }
            .singleOrNull() ?: return@dbQuery null

        // Get subcategories
        val subcategories = Categories.selectAll()
            .where { Categories.parentCategoryId eq id }
            .map { it.toCategory() }

        // Get parent category if exists
        val parentCategory = category.parentCategoryId?.let { parentId ->
            Categories.selectAll()
                .where { Categories.id eq parentId }
                .map { it.toCategory() }
                .singleOrNull()
        }

        category.copy(parentCategory = parentCategory, subCategories = subcategories)
    }

    override suspend fun updateCategory(category: Category): Category = DatabaseFactory.dbQuery {
        Categories.update({ Categories.id eq category.id }) {
            it[categoryName] = category.categoryName
            it[description] = category.description
            it[parentCategoryId] = category.parentCategoryId
        }
        category
    }

    override suspend fun deleteCategory(id: Int): Unit = DatabaseFactory.dbQuery {
        // First update any subcategories to null parent
        Categories.update({ Categories.parentCategoryId eq id }) {
            it[parentCategoryId] = null
        }

        // Then delete the category
        Categories.deleteWhere { Categories.id eq id }
    }

    override suspend fun getAllCategories(): List<Category> = DatabaseFactory.dbQuery {
        Categories.selectAll()
            .orderBy(Categories.categoryName to SortOrder.ASC)
            .map { it.toCategory() }
    }

    override suspend fun getRootCategories(): List<Category> = DatabaseFactory.dbQuery {
        Categories.selectAll()
            .where { Categories.parentCategoryId.isNull() }
            .orderBy(Categories.categoryName to SortOrder.ASC)
            .map { it.toCategory() }
    }

    override suspend fun getSubcategories(parentCategoryId: Int): List<Category> = DatabaseFactory.dbQuery {
        Categories.selectAll()
            .where { Categories.parentCategoryId eq parentCategoryId }
            .orderBy(Categories.categoryName to SortOrder.ASC)
            .map { it.toCategory() }
    }

    override suspend fun getCategoryTreeStructure(): List<Category> = DatabaseFactory.dbQuery {
        // First get all root categories
        val rootCategories = Categories.selectAll()
            .where { Categories.parentCategoryId.isNull() }
            .orderBy(Categories.categoryName to SortOrder.ASC)
            .map { it.toCategory() }
            .toMutableList()

        // For each root category, recursively build the tree
        rootCategories.map { buildCategoryTree(it) }
    }

    // Helper function to recursively build category tree
    private fun buildCategoryTree(category: Category): Category {
        val subcategories = Categories.selectAll()
            .where { Categories.parentCategoryId eq category.id }
            .orderBy(Categories.categoryName to SortOrder.ASC)
            .map { it.toCategory() }
            .toMutableList()

        // Recursively build tree for each subcategory
        val subcategoriesWithChildren = subcategories.map { buildCategoryTree(it) }

        return category.copy(subCategories = subcategoriesWithChildren)
    }

    // Mapping function
    private fun ResultRow.toCategory() = Category(
        id = this[Categories.id].value,
        categoryName = this[Categories.categoryName],
        description = this[Categories.description],
        parentCategoryId = this[Categories.parentCategoryId]?.value
    )
} 