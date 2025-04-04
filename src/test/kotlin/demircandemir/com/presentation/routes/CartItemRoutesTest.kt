package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.AddCartItemRequest
import demircandemir.com.application.dto.UpdateCartItemQuantityRequest
import demircandemir.com.application.serialization.appSerializersModule
import demircandemir.com.domain.model.Cart
import demircandemir.com.domain.model.CartItem
import demircandemir.com.domain.model.Product
import demircandemir.com.domain.repository.CartItemRepository
import demircandemir.com.domain.repository.CartRepository
import demircandemir.com.domain.repository.ProductRepository
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

class CartItemRoutesTest {
    private val mockCartItemRepository: CartItemRepository = mockk()
    private val mockCartRepository: CartRepository = mockk()
    private val mockProductRepository: ProductRepository = mockk()

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

    private val testCartItem = CartItem(
        id = 1,
        cartId = 1,
        productId = 1,
        quantity = 2,
        unitPrice = BigDecimal("50.00"),
        sizeId = 1,
        colorId = 2
    )

    private val testProduct = mockk<Product> {
        coEvery { id } returns 1
        coEvery { price } returns BigDecimal("50.00")
    }

    private fun Application.testModule() {
        install(ContentNegotiation) {
            json(json)
        }

        routing {
            cartItemRoutes(mockCartItemRepository, mockCartRepository, mockProductRepository)
        }
    }

    @Test
    fun `GET _api_cart-items_{id} should return cart item when exists`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockCartItemRepository.findById(1) } returns Result.success(testCartItem)

            val response = client.get("/api/cart-items/1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"unitPrice\": \"50.00\""))
            assertTrue(response.bodyAsText().contains("\"quantity\": 2"))
        }
    }

    @Test
    fun `GET _api_cart-items_{id} should return 404 when cart item does not exist`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockCartItemRepository.findById(999) } returns Result.success(null)

            val response = client.get("/api/cart-items/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `GET _api_cart-items_cart_{cartId} should return all items in a cart`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockCartRepository.findById(1) } returns Result.success(testCart)
            coEvery { mockCartItemRepository.findByCartId(1) } returns Result.success(listOf(testCartItem))

            val response = client.get("/api/cart-items/cart/1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"unitPrice\": \"50.00\""))
            assertTrue(response.bodyAsText().contains("\"quantity\": 2"))
        }
    }

    @Test
    fun `POST _api_cart-items should add new item to cart`() {
        testApplication {
            application {
                testModule()
            }

            val request = AddCartItemRequest(
                cartId = 1,
                productId = 1,
                quantity = 2,
                sizeId = 1,
                colorId = 2
            )

            coEvery { mockProductRepository.getProductById(1) } returns testProduct
            coEvery { mockCartRepository.findById(1) } returns Result.success(testCart)
            coEvery { mockCartItemRepository.findByCartIdAndProductId(1, 1, 1, 2) } returns Result.success(null)
            coEvery { mockCartItemRepository.create(any()) } returns Result.success(testCartItem)
            coEvery { mockCartRepository.updateTotalAmount(1, any()) } returns Result.success(testCart)

            val response = client.post("/api/cart-items") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            assertEquals(HttpStatusCode.Created, response.status)
            assertTrue(response.bodyAsText().contains("\"unitPrice\": \"50.00\""))
        }
    }

    @Test
    fun `POST _api_cart-items should update quantity when item already exists`() {
        testApplication {
            application {
                testModule()
            }

            val request = AddCartItemRequest(
                cartId = 1,
                productId = 1,
                quantity = 3,
                sizeId = 1,
                colorId = 2
            )

            coEvery { mockProductRepository.getProductById(1) } returns testProduct
            coEvery { mockCartRepository.findById(1) } returns Result.success(testCart)
            coEvery { mockCartItemRepository.findByCartIdAndProductId(1, 1, 1, 2) } returns Result.success(testCartItem)
            coEvery {
                mockCartItemRepository.updateQuantity(
                    1,
                    5
                )
            } returns Result.success(testCartItem.copy(quantity = 5))
            coEvery { mockCartRepository.updateTotalAmount(1, any()) } returns Result.success(testCart)

            val response = client.post("/api/cart-items") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `POST _api_cart-items should return 404 when product not found`() {
        testApplication {
            application {
                testModule()
            }

            val request = AddCartItemRequest(
                cartId = 1,
                productId = 999,
                quantity = 2
            )

            coEvery { mockProductRepository.getProductById(999) } returns null

            val response = client.post("/api/cart-items") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `POST _api_cart-items should return 404 when cart not found`() {
        testApplication {
            application {
                testModule()
            }

            val request = AddCartItemRequest(
                cartId = 999,
                productId = 1,
                quantity = 2
            )

            coEvery { mockProductRepository.getProductById(1) } returns testProduct
            coEvery { mockCartRepository.findById(999) } returns Result.success(null)

            val response = client.post("/api/cart-items") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `PUT _api_cart-items_{id}_quantity should update cart item quantity`() {
        testApplication {
            application {
                testModule()
            }

            val request = UpdateCartItemQuantityRequest(quantity = 5)

            coEvery { mockCartItemRepository.findById(1) } returns Result.success(testCartItem)
            coEvery { mockCartRepository.findById(1) } returns Result.success(testCart)
            coEvery {
                mockCartItemRepository.updateQuantity(
                    1,
                    5
                )
            } returns Result.success(testCartItem.copy(quantity = 5))
            coEvery { mockCartRepository.updateTotalAmount(1, any()) } returns Result.success(testCart)

            val response = client.put("/api/cart-items/1/quantity") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"quantity\": 5"))
        }
    }

    @Test
    fun `PUT _api_cart-items_{id}_quantity should return 404 when cart item not found`() {
        testApplication {
            application {
                testModule()
            }

            val request = UpdateCartItemQuantityRequest(quantity = 5)

            coEvery { mockCartItemRepository.findById(999) } returns Result.success(null)

            val response = client.put("/api/cart-items/999/quantity") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `PUT _api_cart-items_{id}_quantity should validate quantity is positive`() {
        testApplication {
            application {
                testModule()
            }

            val request = UpdateCartItemQuantityRequest(quantity = 0)

            coEvery { mockCartItemRepository.findById(1) } returns Result.success(testCartItem)

            val response = client.put("/api/cart-items/1/quantity") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            println("Response Body: ${response.bodyAsText()}")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Quantity must be positive"))
        }
    }

    @Test
    fun `DELETE _api_cart-items_{id} should remove item from cart`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockCartItemRepository.findById(1) } returns Result.success(testCartItem)
            coEvery { mockCartRepository.findById(1) } returns Result.success(testCart)
            coEvery { mockCartItemRepository.delete(1) } returns Result.success(true)
            coEvery { mockCartRepository.updateTotalAmount(1, any()) } returns Result.success(testCart)

            val response = client.delete("/api/cart-items/1")
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `DELETE _api_cart-items_{id} should return 404 when cart item not found`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockCartItemRepository.findById(999) } returns Result.success(null)

            val response = client.delete("/api/cart-items/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }
} 