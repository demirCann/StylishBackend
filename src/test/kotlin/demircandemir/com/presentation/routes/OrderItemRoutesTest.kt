package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.UpdateOrderItemQuantityRequest
import demircandemir.com.application.serialization.appSerializersModule
import demircandemir.com.domain.model.Order
import demircandemir.com.domain.model.OrderItem
import demircandemir.com.domain.repository.OrderItemRepository
import demircandemir.com.domain.repository.OrderRepository
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

class OrderItemRoutesTest {
    private val mockOrderItemRepository: OrderItemRepository = mockk()
    private val mockOrderRepository: OrderRepository = mockk()

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
            orderItemRoutes(mockOrderItemRepository, mockOrderRepository)
        }
    }

    @Test
    fun `GET _api_order-items_{id} should return order item when exists`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockOrderItemRepository.findById(1) } returns Result.success(testOrderItem)

            val response = client.get("/api/order-items/1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("50.00"))
        }
    }

    @Test
    fun `GET _api_order-items_{id} should return 404 when order item does not exist`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockOrderItemRepository.findById(999) } returns Result.success(null)

            val response = client.get("/api/order-items/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `GET _api_order-items_order_{orderId} should return order items for an order`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockOrderRepository.findById(1) } returns Result.success(testOrder)
            coEvery { mockOrderItemRepository.findByOrderId(1) } returns Result.success(listOf(testOrderItem))

            val response = client.get("/api/order-items/order/1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("50.00"))
        }
    }

    @Test
    fun `GET _api_order-items_order_{orderId} should return 404 when order does not exist`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockOrderRepository.findById(999) } returns Result.success(null)

            val response = client.get("/api/order-items/order/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `PUT _api_order-items_{id}_quantity should update order item quantity`() {
        testApplication {
            application {
                testModule()
            }

            val request = UpdateOrderItemQuantityRequest(3)

            coEvery { mockOrderItemRepository.findById(1) } returns Result.success(testOrderItem)
            coEvery { mockOrderRepository.findById(1) } returns Result.success(testOrder)
            coEvery { mockOrderRepository.update(any()) } returns Result.success(testOrder)
            coEvery { mockOrderItemRepository.updateQuantity(1, 3) } returns
                    Result.success(testOrderItem.copy(quantity = 3))

            val response = client.put("/api/order-items/1/quantity") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            println("Response body: ${response.bodyAsText()}")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"quantity\": 3"))
        }
    }

    @Test
    fun `PUT _api_order-items_{id}_quantity should return 404 when order item does not exist`() {
        testApplication {
            application {
                testModule()
            }

            val request = UpdateOrderItemQuantityRequest(3)

            coEvery { mockOrderItemRepository.findById(999) } returns Result.success(null)

            val response = client.put("/api/order-items/999/quantity") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `PUT _api_order-items_{id}_quantity should validate quantity is positive`() {
        testApplication {
            application {
                testModule()
            }

            val request = UpdateOrderItemQuantityRequest(0)

            val response = client.put("/api/order-items/1/quantity") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Quantity must be positive"))
        }
    }

    @Test
    fun `DELETE _api_order-items_{id} should delete an order item`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockOrderItemRepository.findById(1) } returns Result.success(testOrderItem)
            coEvery { mockOrderRepository.findById(1) } returns Result.success(testOrder)
            coEvery { mockOrderRepository.update(any()) } returns Result.success(testOrder)
            coEvery { mockOrderItemRepository.delete(1) } returns Result.success(true)

            val response = client.delete("/api/order-items/1")
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `DELETE _api_order-items_{id} should return 404 when order item does not exist`() {
        testApplication {
            application {
                testModule()
            }

            coEvery { mockOrderItemRepository.findById(999) } returns Result.success(null)

            val response = client.delete("/api/order-items/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }
} 