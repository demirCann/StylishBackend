package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.AddressResponse
import demircandemir.com.application.dto.CreateAddressRequest
import demircandemir.com.application.dto.UpdateAddressRequest
import demircandemir.com.domain.model.Address
import demircandemir.com.domain.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.addressRoutes(userRepository: UserRepository) {
    routing {
        route("/addresses") {
            // Get address by ID
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
                val address = userRepository.getAddressById(id)
                if (address != null) {
                    call.respond(address.toAddressResponse())
                } else {
                    call.respond(HttpStatusCode.NotFound, "Address not found")
                }
            }

            // Get addresses by user ID
            get("/user/{userId}") {
                val userId =
                    call.parameters["userId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid user ID")
                val addresses = userRepository.getAddressesByUserId(userId)
                call.respond(addresses.map { it.toAddressResponse() })
            }

            // Create new address
            post {
                val request = call.receive<CreateAddressRequest>()
                val address = Address(
                    id = 0,
                    userId = request.userId,
                    addressTitle = request.addressTitle,
                    address = request.address,
                    city = request.city,
                    district = request.district,
                    postalCode = request.postalCode,
                    country = request.country
                )
                val createdAddress = userRepository.createAddress(address)
                call.respond(HttpStatusCode.Created, createdAddress.toAddressResponse())
            }

            // Update address
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
                val request = call.receive<UpdateAddressRequest>()
                val existingAddress = userRepository.getAddressById(id)
                    ?: throw IllegalArgumentException("Address not found")

                val updatedAddress = existingAddress.copy(
                    addressTitle = request.addressTitle ?: existingAddress.addressTitle,
                    address = request.address ?: existingAddress.address,
                    city = request.city ?: existingAddress.city,
                    district = request.district ?: existingAddress.district,
                    postalCode = request.postalCode ?: existingAddress.postalCode,
                    country = request.country ?: existingAddress.country
                )
                val savedAddress = userRepository.updateAddress(updatedAddress)
                call.respond(savedAddress.toAddressResponse())
            }

            // Delete address
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
                userRepository.deleteAddress(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun Address.toAddressResponse() = AddressResponse(
    id = id,
    userId = userId,
    addressTitle = addressTitle,
    address = address,
    city = city,
    district = district,
    postalCode = postalCode,
    country = country
) 