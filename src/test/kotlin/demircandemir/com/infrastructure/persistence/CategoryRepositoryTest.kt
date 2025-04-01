package demircandemir.com.infrastructure.persistence

import demircandemir.com.domain.model.Category
import demircandemir.com.infrastructure.persistence.tables.Categories
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CategoryRepositoryTest {
    private lateinit var repository: CategoryRepositoryImpl
    private lateinit var testCategory: Category
    private lateinit var testSubCategory: Category

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            TestDatabaseFactory.initTestDatabase()
            transaction {
                SchemaUtils.create(Categories)
            }
        }
    }

    @BeforeEach
    fun setupTestData() {
        repository = CategoryRepositoryImpl()
        transaction {
            SchemaUtils.drop(Categories)
            SchemaUtils.create(Categories)
        }

        testCategory = Category(
            categoryName = "Test Category",
            description = "Test Description"
        )

        testSubCategory = Category(
            categoryName = "Test Sub Category",
            description = "Test Sub Description",
            parentCategoryId = 1
        )
    }

    @Test
    fun `createCategory should create a new category`() = runBlocking {
        val createdCategory = repository.createCategory(testCategory)
        assertEquals(testCategory.categoryName, createdCategory.categoryName)
        assertEquals(testCategory.description, createdCategory.description)
        assertTrue(createdCategory.id > 0)
    }

    @Test
    fun `getCategoryById should return category when exists`() = runBlocking {
        val createdCategory = repository.createCategory(testCategory)
        val retrievedCategory = repository.getCategoryById(createdCategory.id)
        assertEquals(createdCategory.id, retrievedCategory?.id)
        assertEquals(createdCategory.categoryName, retrievedCategory?.categoryName)
    }

    @Test
    fun `getCategoryById should return null when category does not exist`() = runBlocking {
        val retrievedCategory = repository.getCategoryById(999)
        assertNull(retrievedCategory)
    }

    @Test
    fun `updateCategory should update existing category`() = runBlocking {
        val createdCategory = repository.createCategory(testCategory)
        val updatedCategory = createdCategory.copy(
            categoryName = "Updated Category",
            description = "Updated Description"
        )
        val result = repository.updateCategory(updatedCategory)
        assertEquals("Updated Category", result.categoryName)
        assertEquals("Updated Description", result.description)
    }

    @Test
    fun `deleteCategory should delete existing category`() = runBlocking {
        val createdCategory = repository.createCategory(testCategory)
        repository.deleteCategory(createdCategory.id)
        val deletedCategory = repository.getCategoryById(createdCategory.id)
        assertNull(deletedCategory)
    }

    @Test
    fun `getAllCategories should return all categories`() = runBlocking {
        val category1 = repository.createCategory(testCategory)
        val category2 = repository.createCategory(testCategory.copy(categoryName = "Second Category"))
        val categories = repository.getAllCategories()
        assertEquals(2, categories.size)
        assertTrue(categories.any { it.id == category1.id })
        assertTrue(categories.any { it.id == category2.id })
    }

    @Test
    fun `getRootCategories should return only categories without parent`() = runBlocking {
        val rootCategory = repository.createCategory(testCategory)
        val subCategory = repository.createCategory(testSubCategory)
        val rootCategories = repository.getRootCategories()
        assertEquals(1, rootCategories.size)
        assertEquals(rootCategory.id, rootCategories[0].id)
    }

    @Test
    fun `getSubcategories should return child categories`() = runBlocking {
        val parentCategory = repository.createCategory(testCategory)
        val subCategory = repository.createCategory(testSubCategory.copy(parentCategoryId = parentCategory.id))
        val subCategories = repository.getSubcategories(parentCategory.id)
        assertEquals(1, subCategories.size)
        assertEquals(subCategory.id, subCategories[0].id)
    }

    @Test
    fun `getCategoryTreeStructure should return complete category hierarchy`() = runBlocking {
        val rootCategory = repository.createCategory(testCategory)
        val subCategory = repository.createCategory(testSubCategory.copy(parentCategoryId = rootCategory.id))
        val subSubCategory = repository.createCategory(
            testSubCategory.copy(
                categoryName = "Sub Sub Category",
                parentCategoryId = subCategory.id
            )
        )

        val categoryTree = repository.getCategoryTreeStructure()
        assertEquals(1, categoryTree.size) // Only root categories
        assertEquals(1, categoryTree[0].subCategories.size) // One subcategory
        assertEquals(1, categoryTree[0].subCategories[0].subCategories.size) // One sub-subcategory
    }

    @Test
    fun `getCategoryByIdWithSubcategories should return category with its hierarchy`() = runBlocking {
        val parentCategory = repository.createCategory(testCategory)
        val subCategory = repository.createCategory(testSubCategory.copy(parentCategoryId = parentCategory.id))

        val categoryWithSubcategories = repository.getCategoryByIdWithSubcategories(parentCategory.id)
        assertTrue(categoryWithSubcategories != null)
        assertEquals(1, categoryWithSubcategories.subCategories.size)
        assertEquals(subCategory.id, categoryWithSubcategories.subCategories[0].id)
    }

    @Test
    fun `deleteCategory should handle subcategories correctly`() = runBlocking {
        val parentCategory = repository.createCategory(testCategory)
        val subCategory = repository.createCategory(testSubCategory.copy(parentCategoryId = parentCategory.id))

        repository.deleteCategory(parentCategory.id)

        // Subcategory should still exist but with null parent
        val updatedSubCategory = repository.getCategoryById(subCategory.id)
        assertTrue(updatedSubCategory != null)
        assertNull(updatedSubCategory.parentCategoryId)
    }
} 