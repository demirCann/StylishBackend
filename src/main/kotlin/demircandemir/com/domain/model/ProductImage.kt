package demircandemir.com.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductImage(
    val id: Int = 0,
    val productId: Int,
    val imageUrl: String,
    val isPrimary: Boolean = false,
    val displayOrder: Int = 0
) 