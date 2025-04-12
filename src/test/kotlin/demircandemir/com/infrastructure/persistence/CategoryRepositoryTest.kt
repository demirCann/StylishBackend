package demircandemir.com.infrastructure.persistence

import demircandemir.com.domain.model.Category
import demircandemir.com.infrastructure.persistence.repository.CategoryRepositoryImpl
import demircandemir.com.infrastructure.persistence.tables.Categories
import demircandemir.com.testutils.TestData
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CategoryRepositoryTest : BaseRepositoryTest() {
    private lateinit var repository: CategoryRepositoryImpl

    // Test data objects will be created within @BeforeEach
    private var testParentCategoryId: Int = 0
    private var testSubCategoryId: Int = 0
    private lateinit var testParentCategory: Category
    private lateinit var testSubCategory: Category

    // @BeforeAll inherited from BaseRepositoryTest

    @BeforeEach
    fun setupTestRepositoriesAndData() {
        // BaseRepositoryTest's @BeforeEach (cleanDataBeforeTest) runs first
        super.logger.info("Setting up repository and test data for CategoryRepositoryTest")
        repository = CategoryRepositoryImpl()

        // Data cleaning is handled by the base class @BeforeEach

        // Create a standard parent and subcategory for use in multiple tests
        transaction {
            val parentCategoryData = TestData.Categories.createTestCategory(categoryName = "Parent Category")
            val parentResult = Categories.insert {
                it[categoryName] = parentCategoryData.categoryName
                it[description] = parentCategoryData.description
            }
            testParentCategoryId = parentResult[Categories.id].value
            testParentCategory = parentCategoryData.copy(id = testParentCategoryId)

            val subCategoryData = TestData.Categories.createTestCategory(
                categoryName = "Sub Category",
                parentCategoryId = testParentCategoryId
            )
            val subResult = Categories.insert {
                it[categoryName] = subCategoryData.categoryName
                it[description] = subCategoryData.description
                it[parentCategoryId] = subCategoryData.parentCategoryId
            }
            testSubCategoryId = subResult[Categories.id].value
            testSubCategory = subCategoryData.copy(id = testSubCategoryId)
        }
    }

    // @AfterAll inherited from BaseRepositoryTest

    @Test
    fun `createCategory should create a new category`() = runBlocking {
        val newCategoryData = TestData.Categories.createTestCategory(categoryName = "New Unique Category")
        val createdCategory = repository.createCategory(newCategoryData)
        assertEquals(newCategoryData.categoryName, createdCategory.categoryName)
        assertEquals(newCategoryData.description, createdCategory.description)
        assertTrue(createdCategory.id > 0)
        assertNotEquals(testParentCategoryId, createdCategory.id)
        assertNotEquals(testSubCategoryId, createdCategory.id)
    }

    @Test
    fun `getCategoryById should return category when exists`() = runBlocking {
        val retrievedParent = repository.getCategoryById(testParentCategoryId)
        assertNotNull(retrievedParent)
        assertEquals(testParentCategoryId, retrievedParent.id)
        assertEquals(testParentCategory.categoryName, retrievedParent.categoryName)

        val retrievedSub = repository.getCategoryById(testSubCategoryId)
        assertNotNull(retrievedSub)
        assertEquals(testSubCategoryId, retrievedSub.id)
        assertEquals(testSubCategory.categoryName, retrievedSub.categoryName)
        assertEquals(testParentCategoryId, retrievedSub.parentCategoryId)
    }

    @Test
    fun `getCategoryById should return null when category does not exist`() = runBlocking {
        val retrievedCategory = repository.getCategoryById(99999)
        assertNull(retrievedCategory)
    }

    @Test
    fun `updateCategory should update existing category`() = runBlocking {
        val retrievedParent = repository.getCategoryById(testParentCategoryId)
        assertNotNull(retrievedParent)

        val updatedCategoryData = retrievedParent.copy(
            categoryName = "Updated Parent Category",
            description = "Updated Description"
        )
        val result = repository.updateCategory(updatedCategoryData)
        assertEquals("Updated Parent Category", result.categoryName)
        assertEquals("Updated Description", result.description)
        assertEquals(testParentCategoryId, result.id) // ID should not change
    }

    @Test
    fun `deleteCategory should delete existing category`() = runBlocking {
        // Create a separate category to delete to avoid interfering with other tests using the default ones
        val categoryToDeleteData = TestData.Categories.createTestCategory(categoryName = "To Be Deleted")
        val createdCategory = repository.createCategory(categoryToDeleteData)
        val categoryIdToDelete = createdCategory.id

        assertNotNull(repository.getCategoryById(categoryIdToDelete)) // Verify it exists
        repository.deleteCategory(categoryIdToDelete)
        val deletedCategory = repository.getCategoryById(categoryIdToDelete)
        assertNull(deletedCategory, "Category should be null after deletion")
    }

    @Test
    fun `getAllCategories should return all categories`() = runBlocking {
        // We start with 2 categories created in @BeforeEach
        val categories = repository.getAllCategories()
        assertEquals(2, categories.size, "Should have 2 categories from setup")
        assertTrue(categories.any { it.id == testParentCategoryId }, "Parent category missing")
        assertTrue(categories.any { it.id == testSubCategoryId }, "Sub category missing")
    }

    @Test
    fun `getRootCategories should return only categories without parent`() = runBlocking {
        // We start with 1 root (parent) and 1 sub category
        val rootCategories = repository.getRootCategories()
        assertEquals(1, rootCategories.size, "Should only find 1 root category")
        assertEquals(testParentCategoryId, rootCategories[0].id)
    }

    @Test
    fun `getSubcategories should return child categories`() = runBlocking {
        // We check the subcategories of the parent created in setup
        val subCategories = repository.getSubcategories(testParentCategoryId)
        assertEquals(1, subCategories.size, "Parent should have 1 subcategory")
        assertEquals(testSubCategoryId, subCategories[0].id)
    }

    @Test
    fun `getCategoryTreeStructure should return complete category hierarchy`() = runBlocking {
        // Create one more level
        val subSubCategoryData = TestData.Categories.createTestCategory(
            categoryName = "Sub Sub Category",
            parentCategoryId = testSubCategoryId
        )
        val createdSubSub = repository.createCategory(subSubCategoryData)

        val categoryTree = repository.getCategoryTreeStructure()

        assertEquals(1, categoryTree.size, "Should only be 1 root category in the tree")
        val rootNode = categoryTree[0]
        assertEquals(testParentCategoryId, rootNode.id)

        assertEquals(1, rootNode.subCategories.size, "Root should have 1 subcategory")
        val subNode = rootNode.subCategories[0]
        assertEquals(testSubCategoryId, subNode.id)

        assertEquals(1, subNode.subCategories.size, "Subcategory should have 1 sub-subcategory")
        val subSubNode = subNode.subCategories[0]
        assertEquals(createdSubSub.id, subSubNode.id)
        assertTrue(subSubNode.subCategories.isEmpty(), "Sub-subcategory should have no children")
    }

    @Test
    fun `getCategoryByIdWithSubcategories should return category with its hierarchy`() = runBlocking {
        // Uses the parent and subcategory created in setup
        val categoryWithSubcategories = repository.getCategoryByIdWithSubcategories(testParentCategoryId)
        assertNotNull(categoryWithSubcategories, "Parent category should be found")
        assertEquals(1, categoryWithSubcategories.subCategories.size, "Parent should have 1 subcategory fetched")
        assertEquals(testSubCategoryId, categoryWithSubcategories.subCategories[0].id)
    }

    @Test
    fun `deleteCategory should handle subcategories correctly by setting parent to null`() = runBlocking {
        // Uses the parent and subcategory created in setup
        assertNotNull(repository.getCategoryById(testParentCategoryId))
        assertNotNull(repository.getCategoryById(testSubCategoryId))

        repository.deleteCategory(testParentCategoryId)

        // Parent should be gone
        assertNull(repository.getCategoryById(testParentCategoryId), "Parent category should be deleted")
        
        // Subcategory should still exist but with null parent
        val updatedSubCategory = repository.getCategoryById(testSubCategoryId)
        assertNotNull(updatedSubCategory, "Subcategory should still exist after parent deletion")
        assertNull(updatedSubCategory.parentCategoryId, "Subcategory's parent ID should be null after parent deletion")
    }
} 