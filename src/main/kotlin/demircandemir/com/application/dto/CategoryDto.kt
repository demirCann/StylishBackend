package demircandemir.com.application.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryRequest(
    val categoryName: String,
    val description: String? = null,
    val parentCategoryId: Int? = null
) 