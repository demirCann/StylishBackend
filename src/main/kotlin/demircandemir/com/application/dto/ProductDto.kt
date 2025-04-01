package demircandemir.com.application.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductRequest(
    val productName: String,
    val description: String? = null,
    val price: Double,
    val stockQuantity: Int = 0,
    val categoryId: Int? = null,
    val brand: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class ProductResponse(
    val id: Int,
    val productName: String,
    val description: String? = null,
    val price: Double,
    val stockQuantity: Int,
    val categoryId: Int? = null,
    val brand: String? = null,
    val dateAdded: String,
    val isActive: Boolean
)

@Serializable
data class ProductDetailRequest(
    val productId: Int,
    val color: String? = null,
    val size: String? = null,
    val material: String? = null,
    val madeIn: String? = null,
    val careInstructions: String? = null,
    val gender: String,
    val season: String? = null
)

@Serializable
data class ProductImageRequest(
    val productId: Int,
    val imageUrl: String,
    val isPrimary: Boolean = false,
    val displayOrder: Int = 0
)