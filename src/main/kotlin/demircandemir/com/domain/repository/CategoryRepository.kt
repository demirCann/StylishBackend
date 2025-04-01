package demircandemir.com.domain.repository

import demircandemir.com.domain.model.Category

interface CategoryRepository {
    suspend fun createCategory(category: Category): Category
    suspend fun getCategoryById(id: Int): Category?
    suspend fun getCategoryByIdWithSubcategories(id: Int): Category?
    suspend fun updateCategory(category: Category): Category
    suspend fun deleteCategory(id: Int)
    suspend fun getAllCategories(): List<Category>
    suspend fun getRootCategories(): List<Category>
    suspend fun getSubcategories(parentCategoryId: Int): List<Category>
    suspend fun getCategoryTreeStructure(): List<Category>
} 