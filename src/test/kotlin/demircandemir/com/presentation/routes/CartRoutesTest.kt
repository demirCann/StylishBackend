package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.CreateCartRequest
import demircandemir.com.application.dto.UpdateCartTotalRequest
import demircandemir.com.application.serialization.appSerializersModule
import demircandemir.com.domain.model.Cart
import demircandemir.com.domain.repository.CartItemRepository
import demircandemir.com.domain.repository.CartRepository
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CartRoutesTest {
    private val mockCartRepository: CartRepository = mockk()
    private val mockCartItemRepository: CartItemRepository = mockk()

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        serializersModule = appSerializersModule
    }

    private val testCart = Cart(
        id = 1,
        userId = 1,
        totalAmount = BigDecimal("100.00"),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private fun Application.testModule() {
        install(ContentNegotiation) {
            json(json)
        }

        routing {
            cartRoutes(mockCartRepository, mockCartItemRepository)
        }
    }

    @Test
    fun `GET _api_carts_{id} should return cart when exists`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockCartRepository.findById(1) } returns Result.success(testCart)
            coEvery { mockCartItemRepository.countByCartId(1) } returns Result.success(5)

            val response = client.get("/api/carts/1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("100.00"))
            assertTrue(response.bodyAsText().contains("\"itemCount\": 5"))
        }
    }

    @Test
    fun `GET _api_carts_{id} should return 404 when cart does not exist`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockCartRepository.findById(999) } returns Result.success(null)

            val response = client.get("/api/carts/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `GET _api_carts_user_{userId} should return user cart when exists`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockCartRepository.findByUserId(1) } returns Result.success(testCart)
            coEvery { mockCartItemRepository.countByCartId(1) } returns Result.success(3)

            val response = client.get("/api/carts/user/1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("100.00"))
            assertTrue(response.bodyAsText().contains("\"itemCount\": 3"))
        }
    }

    @Test
    fun `GET _api_carts_user_{userId} should create new cart when user has none`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockCartRepository.findByUserId(1) } returns Result.success(null)
            coEvery { mockCartRepository.create(any()) } returns Result.success(
                Cart(
                    id = 1,
                    userId = 1,
                    totalAmount = BigDecimal.ZERO,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )

            val response = client.get("/api/carts/user/1")
            assertEquals(HttpStatusCode.Created, response.status)
            assertTrue(response.bodyAsText().contains("0"))
            assertTrue(response.bodyAsText().contains("\"itemCount\": 0"))
        }
    }

    @Test
    fun `POST _api_carts should create a new cart`() {
        testApplication {
            application {
                testModule()
            }

            val cartRequest = CreateCartRequest(
                userId = 2
            )

            coEvery { mockCartRepository.findByUserId(2) } returns Result.success(null)
            coEvery { mockCartRepository.create(any()) } returns Result.success(
                Cart(
                    id = 2,
                    userId = 2,
                    totalAmount = BigDecimal.ZERO,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )

            val response = client.post("/api/carts") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(cartRequest))
            }

            assertEquals(HttpStatusCode.Created, response.status)
            assertTrue(response.bodyAsText().contains("\"userId\": 2"))
        }
    }

    @Test
    fun `POST _api_carts should return conflict when user already has a cart`() {
        testApplication {
            application {
                testModule()
            }

            val cartRequest = CreateCartRequest(
                userId = 1
            )

            coEvery { mockCartRepository.findByUserId(1) } returns Result.success(testCart)

            val response = client.post("/api/carts") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(cartRequest))
            }

            assertEquals(HttpStatusCode.Conflict, response.status)
        }
    }

    @Test
    fun `PUT _api_carts_{id}_total should update cart total`() {
        testApplication {
            application {
                testModule()
            }

            val request = UpdateCartTotalRequest(
                totalAmount = "150.00"
            )

            coEvery { mockCartRepository.updateTotalAmount(1, BigDecimal("150.00")) } returns Result.success(
                testCart.copy(totalAmount = BigDecimal("150.00"))
            )
            coEvery { mockCartItemRepository.countByCartId(1) } returns Result.success(2)

            val response = client.put("/api/carts/1/total") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("150.00"))
        }
    }

    @Test
    fun `DELETE _api_carts_{id} should delete a cart and its items`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockCartItemRepository.deleteByCartId(1) } returns Result.success(3)
            coEvery { mockCartRepository.delete(1) } returns Result.success(true)

            val response = client.delete("/api/carts/1")
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `DELETE _api_carts_{id} should return 404 when cart does not exist`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockCartItemRepository.deleteByCartId(999) } returns Result.success(0)
            coEvery { mockCartRepository.delete(999) } returns Result.success(false)

            val response = client.delete("/api/carts/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }
} 