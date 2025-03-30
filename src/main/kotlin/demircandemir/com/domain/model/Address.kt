package demircandemir.com.domain.model

data class Address(
    val id: Int,
    val userId: Int,
    val addressTitle: String,
    val address: String,
    val city: String,
    val district: String,
    val postalCode: String?,
    val country: String
) 