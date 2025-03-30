package demircandemir.com.presentation.routes

import demircandemir.com.application.serialization.appSerializersModule
import demircandemir.com.domain.repository.UserRepository
import demircandemir.com.testutils.TestData
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.assertEquals

class AddressRoutesTest : KoinTest {
    private val userRepository = mockk<UserRepository>()
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        serializersModule = appSerializersModule
    }

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(module {
                single { userRepository }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun Application.testModule() {
        // Content negotiation
        install(ContentNegotiation) {
            json(json)
        }

        // Routes
        routing {
            addressRoutes(userRepository)
        }
    }

    @Test
    fun `test get address by id`() = testApplication {
        // Configure test application
        application {
            testModule()
        }

        // Given
        val address = TestData.Addresses.createTestAddress(id = 1)
        coEvery { userRepository.getAddressById(1) } returns address

        // When
        val response = client.get("/addresses/1")

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        // Add JSON response validation here
    }

    @Test
    fun `test get addresses by user id`() = testApplication {
        // Configure test application
        application {
            testModule()
        }

        // Given
        val addresses = TestData.Addresses.createTestAddresses(userId = 1)
        coEvery { userRepository.getAddressesByUserId(1) } returns addresses

        // When
        val response = client.get("/addresses/user/1")

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        // Add JSON response validation here
    }

    @Test
    fun `test create address`() = testApplication {
        // Configure test application
        application {
            testModule()
        }

        // Given
        val request = TestData.Addresses.createTestAddressRequest()
        val createdAddress = TestData.Addresses.createTestAddress(
            id = 1,
            userId = request.userId,
            addressTitle = request.addressTitle,
            address = request.address,
            city = request.city,
            district = request.district,
            postalCode = request.postalCode ?: "",
            country = request.country
        )
        coEvery {
            userRepository.createAddress(any())
        } returns createdAddress

        // When
        val response = client.post("/addresses") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        // Then
        assertEquals(HttpStatusCode.Created, response.status)
        // Add JSON response validation here
    }

    @Test
    fun `test update address`() = testApplication {
        // Configure test application
        application {
            testModule()
        }

        // Given
        val existingAddress = TestData.Addresses.createTestAddress(id = 1)
        val request = TestData.Addresses.createTestUpdateAddressRequest()
        val updatedAddress = existingAddress.copy(
            addressTitle = request.addressTitle ?: existingAddress.addressTitle
        )

        coEvery { userRepository.getAddressById(1) } returns existingAddress
        coEvery { userRepository.updateAddress(any()) } returns updatedAddress

        // When
        val response = client.put("/addresses/1") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        // Add JSON response validation here
    }

    @Test
    fun `test delete address`() = testApplication {
        // Configure test application
        application {
            testModule()
        }

        // Given
        coEvery { userRepository.deleteAddress(1) } returns Unit

        // When
        val response = client.delete("/addresses/1")

        // Then
        assertEquals(HttpStatusCode.NoContent, response.status)
    }
} 