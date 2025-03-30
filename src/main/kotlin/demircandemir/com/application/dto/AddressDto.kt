package demircandemir.com.application.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateAddressRequest(
    val userId: Int,
    val addressTitle: String,
    val address: String,
    val city: String,
    val district: String,
    val postalCode: String? = null,
    val country: String = "Turkey"
)

@Serializable
data class UpdateAddressRequest(
    val addressTitle: String? = null,
    val address: String? = null,
    val city: String? = null,
    val district: String? = null,
    val postalCode: String? = null,
    val country: String? = null
)

@Serializable
data class AddressResponse(
    val id: Int,
    val userId: Int,
    val addressTitle: String,
    val address: String,
    val city: String,
    val district: String,
    val postalCode: String?,
    val country: String
) 