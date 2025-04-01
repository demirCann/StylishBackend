package demircandemir.com.domain.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Serializable
data class Product(
    val id: Int = 0,
    val productName: String,
    val description: String? = null,
    @Contextual
    val price: BigDecimal,
    val stockQuantity: Int = 0,
    val categoryId: Int? = null,
    val brand: String? = null,
    @Contextual
    val dateAdded: LocalDateTime = LocalDateTime.now(),
    val isActive: Boolean = true,
    val details: ProductDetail? = null,
    val images: List<ProductImage> = emptyList(),
    val category: Category? = null
) 