package demircandemir.com.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Int = 0,
    val categoryName: String,
    val description: String? = null,
    val parentCategoryId: Int? = null,
    val parentCategory: Category? = null,
    val subCategories: List<Category> = emptyList()
) 