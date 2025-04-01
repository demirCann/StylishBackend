package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.CategoryRequest
import demircandemir.com.application.serialization.appSerializersModule
import demircandemir.com.domain.model.Category
import demircandemir.com.domain.repository.CategoryRepository
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryRoutesTest {
    private val mockRepository: CategoryRepository = mockk()

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        serializersModule = appSerializersModule
    }

    private val testCategory = Category(
        id = 1,
        categoryName = "Test Category",
        description = "Test Description"
    )

    private fun Application.testModule() {
        install(ContentNegotiation) {
            json(json)
        }

        routing {
            categoryRoutes(mockRepository)
        }
    }

    @Test
    fun `GET _api_categories should return all categories`() = testApplication {
        application {
            testModule()
        }

        val categories = listOf(testCategory)
        coEvery { mockRepository.getAllCategories() } returns categories

        val response = client.get("/api/categories")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Test Category"))
    }

    @Test
    fun `GET _api_categories_{id} should return category when exists`() = testApplication {
        application {
            testModule()
        }

        coEvery { mockRepository.getCategoryByIdWithSubcategories(1) } returns testCategory

        val response = client.get("/api/categories/1")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Test Category"))
    }

    @Test
    fun `GET _api_categories_{id} should return 404 when category does not exist`() = testApplication {
        application {
            testModule()
        }

        coEvery { mockRepository.getCategoryByIdWithSubcategories(999) } returns null

        val response = client.get("/api/categories/999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST _api_categories should create new category`() = testApplication {
        application {
            testModule()
        }

        val request = CategoryRequest(
            categoryName = "New Category",
            description = "New Description"
        )

        coEvery { mockRepository.createCategory(any()) } returns testCategory.copy(
            categoryName = request.categoryName,
            description = request.description
        )

        val response = client.post("/api/categories") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CategoryRequest.serializer(), request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("New Category"))
    }

    @Test
    fun `PUT _api_categories_{id} should update existing category`() = testApplication {
        application {
            testModule()
        }

        val request = CategoryRequest(
            categoryName = "Updated Category",
            description = "Updated Description"
        )

        coEvery { mockRepository.getCategoryById(1) } returns testCategory
        coEvery { mockRepository.updateCategory(any()) } returns testCategory.copy(
            categoryName = request.categoryName,
            description = request.description
        )

        val response = client.put("/api/categories/1") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CategoryRequest.serializer(), request))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Updated Category"))
    }

    @Test
    fun `DELETE _api_categories_{id} should delete category`() = testApplication {
        application {
            testModule()
        }

        coEvery { mockRepository.deleteCategory(any()) } returns Unit

        val response = client.delete("/api/categories/1")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET _api_categories_roots should return root categories`() = testApplication {
        application {
            testModule()
        }

        val rootCategories = listOf(testCategory)
        coEvery { mockRepository.getRootCategories() } returns rootCategories

        val response = client.get("/api/categories/roots")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Test Category"))
    }

    @Test
    fun `GET _api_categories_{id}_subcategories should return subcategories`() = testApplication {
        application {
            testModule()
        }

        val subcategories = listOf(testCategory.copy(id = 2, categoryName = "Sub Category"))
        coEvery { mockRepository.getSubcategories(1) } returns subcategories

        val response = client.get("/api/categories/1/subcategories")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Sub Category"))
    }

    @Test
    fun `GET _api_categories_tree should return category tree`() = testApplication {
        application {
            testModule()
        }

        val categoryTree = listOf(
            testCategory.copy(
                subCategories = listOf(
                    testCategory.copy(id = 2, categoryName = "Sub Category")
                )
            )
        )
        coEvery { mockRepository.getCategoryTreeStructure() } returns categoryTree

        val response = client.get("/api/categories/tree")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Test Category"))
        assertTrue(response.bodyAsText().contains("Sub Category"))
    }

    @Test
    fun `POST _api_categories should handle parent category`() = testApplication {
        application {
            testModule()
        }

        val request = CategoryRequest(
            categoryName = "Child Category",
            description = "Child Description",
            parentCategoryId = 1
        )

        coEvery { mockRepository.createCategory(any()) } returns testCategory.copy(
            id = 2,
            categoryName = request.categoryName,
            description = request.description,
            parentCategoryId = request.parentCategoryId
        )

        val response = client.post("/api/categories") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CategoryRequest.serializer(), request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("Child Category"))
    }
} 