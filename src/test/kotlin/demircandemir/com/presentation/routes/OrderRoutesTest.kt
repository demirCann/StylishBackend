package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.CreateOrderItemRequest
import demircandemir.com.application.dto.CreateOrderRequest
import demircandemir.com.application.dto.UpdateOrderStatusRequest
import demircandemir.com.application.dto.UpdateTrackingNumberRequest
import demircandemir.com.application.serialization.appSerializersModule
import demircandemir.com.domain.model.Order
import demircandemir.com.domain.model.OrderItem
import demircandemir.com.domain.repository.OrderItemRepository
import demircandemir.com.domain.repository.OrderRepository
import demircandemir.com.domain.repository.ProductRepository
import demircandemir.com.domain.repository.UserRepository
import demircandemir.com.infrastructure.persistence.tables.OrderStatus
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

class OrderRoutesTest {
    private val mockOrderRepository: OrderRepository = mockk()
    private val mockOrderItemRepository: OrderItemRepository = mockk()
    private val mockProductRepository: ProductRepository = mockk()
    private val mockUserRepository: UserRepository = mockk()

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        serializersModule = appSerializersModule
    }

    private val testOrder = Order(
        id = 1,
        userId = 1,
        addressId = 1,
        totalAmount = BigDecimal("100.00"),
        orderDate = LocalDateTime.now(),
        paymentMethod = "Credit Card",
        orderStatus = OrderStatus.Pending,
        trackingNumber = null,
        shippingFee = BigDecimal("10.00")
    )

    private val testOrderItem = OrderItem(
        id = 1,
        orderId = 1,
        productId = 1,
        quantity = 2,
        unitPrice = BigDecimal("50.00"),
        sizeId = null,
        colorId = null
    )

    private fun Application.testModule() {
        install(ContentNegotiation) {
            json(json)
        }

        routing {
            orderRoutes(mockOrderRepository, mockOrderItemRepository, mockProductRepository, mockUserRepository)
        }
    }

    @Test
    fun `GET _api_orders_{id} should return order when exists`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockOrderRepository.findById(1) } returns Result.success(testOrder)
            coEvery { mockOrderItemRepository.findByOrderId(1) } returns Result.success(listOf(testOrderItem))

            try {
                val response = client.get("/api/orders/1")
                println("Response status: ${response.status}")
                println("Response body: ${response.bodyAsText()}")
                assertEquals(HttpStatusCode.OK, response.status)
                assertTrue(response.bodyAsText().contains("Credit Card"))
            } catch (e: Exception) {
                println("Test hata: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }

    @Test
    fun `GET _api_orders_{id} should return 404 when order does not exist`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockOrderRepository.findById(999) } returns Result.success(null)

            val response = client.get("/api/orders/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `GET _api_orders_user_{userId} should return user orders`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockOrderRepository.findByUserId(1) } returns Result.success(listOf(testOrder))

            val response = client.get("/api/orders/user/1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Credit Card"))
        }
    }

    @Test
    fun `GET _api_orders_status_{status} should return orders with matching status`() {
        testApplication {
            application {
                testModule()
            }

            // Tüm OrderStatus değerleri için yanıt döndürmeyi ayarlayalım
            for (statusEnum in OrderStatus.entries) {
                coEvery { mockOrderRepository.findByStatus(statusEnum) } returns
                        Result.success(listOf(testOrder))
            }

            // Client tarafından gönderilen status değeri genellikle küçük harfle olur
            val requestStatus = "pending" // Enum değeri: Pending
            println("İstek ile gönderilen status: $requestStatus")

            val response = client.get("/api/orders/status/$requestStatus")
            println("API Yanıtı: ${response.status} - ${response.bodyAsText()}") // Debug bilgisi

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Credit Card"))
        }
    }

    @Test
    fun `POST _api_orders should create a new order`() {
        testApplication {
            application {
                testModule()
            }

            val orderRequest = CreateOrderRequest(
                userId = 1,
                addressId = 1,
                paymentMethod = "Credit Card",
                items = listOf(
                    CreateOrderItemRequest(
                        productId = 1,
                        quantity = 2
                    )
                ),
                shippingFee = "10.00"
            )

            coEvery { mockUserRepository.getUserById(1) } returns mockk(relaxed = true)
            coEvery { mockUserRepository.getAddressById(1) } returns mockk(relaxed = true)
            coEvery { mockProductRepository.getProductById(1) } returns mockk {
                coEvery { price } returns BigDecimal("50.00")
            }
            coEvery { mockOrderRepository.create(any()) } returns Result.success(testOrder)
            coEvery { mockOrderItemRepository.createMany(any()) } returns
                    Result.success(listOf(testOrderItem))

            val response = client.post("/api/orders") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(orderRequest))
            }

            assertEquals(HttpStatusCode.Created, response.status)
            assertTrue(response.bodyAsText().contains("Credit Card"))
        }
    }

    @Test
    fun `PUT _api_orders_{id}_status should update order status`() {
        testApplication {
            application {
                testModule()
            }

            val request = UpdateOrderStatusRequest(OrderStatus.Shipped)

            coEvery { mockOrderRepository.updateStatus(1, OrderStatus.Shipped) } returns Result.success(true)
            coEvery { mockOrderRepository.findById(1) } returns Result.success(testOrder.copy(orderStatus = OrderStatus.Shipped))
            coEvery { mockOrderItemRepository.findByOrderId(1) } returns Result.success(listOf(testOrderItem))

            val response = client.put("/api/orders/1/status") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Shipped"))
        }
    }

    @Test
    fun `PUT _api_orders_{id}_tracking should update tracking number`() {
        testApplication {
            application {
                testModule()
            }

            val request = UpdateTrackingNumberRequest("TRACK123")

            coEvery { mockOrderRepository.findById(1) } returns Result.success(testOrder)
            coEvery { mockOrderRepository.update(any()) } returns
                    Result.success(testOrder.copy(trackingNumber = "TRACK123"))
            coEvery { mockOrderItemRepository.findByOrderId(1) } returns Result.success(listOf(testOrderItem))

            val response = client.put("/api/orders/1/tracking") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("TRACK123"))
        }
    }

    @Test
    fun `DELETE _api_orders_{id} should delete an order`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockOrderItemRepository.deleteByOrderId(1) } returns Result.success(1)
            coEvery { mockOrderRepository.delete(1) } returns Result.success(true)

            val response = client.delete("/api/orders/1")
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `POST _api_orders should validate product exists`() {
        testApplication {
            application {
                testModule()
            }

            val orderRequest = CreateOrderRequest(
                userId = 1,
                addressId = 1,
                paymentMethod = "Credit Card",
                items = listOf(
                    CreateOrderItemRequest(
                        productId = 999,
                        quantity = 2
                    )
                ),
                shippingFee = "10.00"
            )

            coEvery { mockUserRepository.getUserById(1) } returns mockk(relaxed = true)
            coEvery { mockUserRepository.getAddressById(1) } returns mockk(relaxed = true)
            coEvery { mockProductRepository.getProductById(999) } returns null

            val response = client.post("/api/orders") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(orderRequest))
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Product not found"))
        }
    }

    @Test
    fun `POST _api_orders should validate user exists`() {
        testApplication {
            application {
                testModule()
            }

            val orderRequest = CreateOrderRequest(
                userId = 999,
                addressId = 1,
                paymentMethod = "Credit Card",
                items = listOf(
                    CreateOrderItemRequest(
                        productId = 1,
                        quantity = 2
                    )
                ),
                shippingFee = "10.00"
            )

            coEvery { mockUserRepository.getUserById(999) } returns null

            val response = client.post("/api/orders") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(orderRequest))
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("User not found"))
        }
    }
} 