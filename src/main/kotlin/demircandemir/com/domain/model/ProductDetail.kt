package demircandemir.com.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductDetail(
    val id: Int = 0,
    val productId: Int,
    val color: String? = null,
    val size: String? = null,
    val material: String? = null,
    val madeIn: String? = null,
    val careInstructions: String? = null,
    val gender: Gender,
    val season: String? = null,
)

@Serializable
enum class Gender {
    Male, Female, Unisex
}