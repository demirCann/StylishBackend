package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.ProductDetailRequest
import demircandemir.com.application.dto.ProductImageRequest
import demircandemir.com.application.dto.ProductRequest
import demircandemir.com.application.serialization.appSerializersModule
import demircandemir.com.domain.model.Gender
import demircandemir.com.domain.model.Product
import demircandemir.com.domain.model.ProductDetail
import demircandemir.com.domain.model.ProductImage
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductRoutesTest {
    private val mockRepository: ProductRepository = mockk()
    private val testProduct = Product(
        id = 1,
        productName = "Test Product",
        description = "Test Description",
        price = BigDecimal("99.99"),
        stockQuantity = 100,
        brand = "Test Brand",
        isActive = true
    )

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        serializersModule = appSerializersModule
    }

    private fun Application.testModule() {
        install(ContentNegotiation) {
            json(json)
        }

        routing {
            productRoutes(mockRepository)
        }
    }

    @Test
    fun `GET _api_products should return all products`() = testApplication {
        application {
            testModule()
        }

        val products = listOf(testProduct)
        coEvery { mockRepository.getActiveProducts(1, 10) } returns products

        val response = client.get("/api/products")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Test Product"))
    }

    @Test
    fun `GET _api_products_{id} should return product when exists`() = testApplication {
        application {
            testModule()
        }

        coEvery { mockRepository.getProductByIdWithDetails(1) } returns testProduct

        val response = client.get("/api/products/1")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Test Product"))
    }

    @Test
    fun `GET _api_products_{id} should return 404 when product does not exist`() = testApplication {
        application {
            testModule()
        }

        coEvery { mockRepository.getProductByIdWithDetails(999) } returns null

        val response = client.get("/api/products/999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST _api_products should create new product`() = testApplication {
        application {
            testModule()
        }

        val request = ProductRequest(
            productName = "New Product",
            description = "New Description",
            price = 149.99,
            stockQuantity = 50,
            categoryId = 1,
            brand = "New Brand",
            isActive = true
        )

        coEvery { mockRepository.createProduct(any()) } returns testProduct.copy(
            productName = request.productName,
            description = request.description,
            price = BigDecimal(request.price.toString()),
            stockQuantity = request.stockQuantity,
            categoryId = request.categoryId,
            brand = request.brand,
            isActive = request.isActive
        )

        val response = client.post("/api/products") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("New Product"))
    }

    @Test
    fun `PUT _api_products_{id} should update existing product`() = testApplication {
        application {
            testModule()
        }

        val request = ProductRequest(
            productName = "Updated Product",
            description = "Updated Description",
            price = 199.99,
            stockQuantity = 75,
            categoryId = 1,
            brand = "Updated Brand",
            isActive = true
        )

        coEvery { mockRepository.getProductById(1) } returns testProduct
        coEvery { mockRepository.updateProduct(any()) } returns testProduct.copy(
            productName = request.productName,
            description = request.description,
            price = BigDecimal(request.price.toString()),
            stockQuantity = request.stockQuantity,
            categoryId = request.categoryId,
            brand = request.brand,
            isActive = request.isActive
        )

        val response = client.put("/api/products/1") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Updated Product"))
    }

    @Test
    fun `DELETE _api_products_{id} should delete product`() = testApplication {
        application {
            testModule()
        }

        coEvery { mockRepository.deleteProduct(1) } returns Unit

        val response = client.delete("/api/products/1")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET _api_products_search should return matching products`() = testApplication {
        application {
            testModule()
        }

        val searchResults = listOf(testProduct)
        coEvery { mockRepository.searchProducts("Test", 1, 10) } returns searchResults

        val response = client.get("/api/products/search?q=Test")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Test Product"))
    }

    @Test
    fun `GET _api_products_category_{categoryId} should return products in category`() = testApplication {
        application {
            testModule()
        }

        val categoryProducts = listOf(testProduct)
        coEvery { mockRepository.getProductsByCategory(1, 1, 10) } returns categoryProducts

        val response = client.get("/api/products/category/1")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Test Product"))
    }

    @Test
    fun `POST _api_products_{productId}_details should create product details`() = testApplication {
        application {
            testModule()
        }

        val request = ProductDetailRequest(
            productId = 1,
            color = "Red",
            size = "M",
            material = "Cotton",
            madeIn = "Turkey",
            careInstructions = "Machine wash",
            gender = "Male",
            season = "Summer"
        )

        val details = ProductDetail(
            id = 1,
            productId = 1,
            color = request.color,
            size = request.size,
            material = request.material,
            madeIn = request.madeIn,
            careInstructions = request.careInstructions,
            gender = Gender.Male,
            season = request.season
        )

        coEvery { mockRepository.getProductDetails(1) } returns null
        coEvery { mockRepository.createProductDetails(any()) } returns details

        val response = client.post("/api/products/1/details") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("Red"))
    }

    @Test
    fun `POST _api_products_{productId}_images should add product image`() = testApplication {
        application {
            testModule()
        }

        val request = ProductImageRequest(
            productId = 1,
            imageUrl = "http://test.com/image.jpg",
            isPrimary = true,
            displayOrder = 1
        )

        val image = ProductImage(
            id = 1,
            productId = 1,
            imageUrl = request.imageUrl,
            isPrimary = request.isPrimary,
            displayOrder = request.displayOrder
        )

        coEvery { mockRepository.addProductImage(any()) } returns image

        val response = client.post("/api/products/1/images") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("http://test.com/image.jpg"))
    }
} 