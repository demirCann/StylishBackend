package demircandemir.com.presentation.routes

import demircandemir.com.domain.model.Category
import demircandemir.com.domain.repository.CategoryRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CategoryRequest(
    val categoryName: String,
    val description: String? = null,
    val parentCategoryId: Int? = null
)

@Serializable
data class CategoryResponse(
    val id: Int,
    val categoryName: String,
    val description: String? = null,
    val parentCategoryId: Int? = null,
    val parentCategory: CategoryResponse? = null,
    val subCategories: List<CategoryResponse> = emptyList()
)

fun Application.categoryRoutes(categoryRepository: CategoryRepository) {
    routing {
        route("/api/categories") {
            // Get all categories
            get {
                val categories = categoryRepository.getAllCategories()
                call.respond(categories.map { it.toCategoryResponse() })
            }

            // Get category by ID with subcategories
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID format")

                val category = categoryRepository.getCategoryByIdWithSubcategories(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, "Category not found")

                call.respond(category.toCategoryResponseWithSubcategories())
            }

            // Create new category
            post {
                val request = call.receive<CategoryRequest>()
                val category = Category(
                    categoryName = request.categoryName,
                    description = request.description,
                    parentCategoryId = request.parentCategoryId
                )

                val createdCategory = categoryRepository.createCategory(category)
                call.respond(HttpStatusCode.Created, createdCategory.toCategoryResponse())
            }

            // Update category
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID format")

                val existingCategory = categoryRepository.getCategoryById(id)
                    ?: return@put call.respond(HttpStatusCode.NotFound, "Category not found")

                val request = call.receive<CategoryRequest>()
                val updatedCategory = existingCategory.copy(
                    categoryName = request.categoryName,
                    description = request.description,
                    parentCategoryId = request.parentCategoryId
                )

                val result = categoryRepository.updateCategory(updatedCategory)
                call.respond(result.toCategoryResponse())
            }

            // Delete category
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID format")

                categoryRepository.deleteCategory(id)
                call.respond(HttpStatusCode.NoContent)
            }

            // Get root categories (categories with no parent)
            get("/roots") {
                val rootCategories = categoryRepository.getRootCategories()
                call.respond(rootCategories.map { it.toCategoryResponse() })
            }

            // Get subcategories for a specific category
            get("/{id}/subcategories") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID format")

                val subcategories = categoryRepository.getSubcategories(id)
                call.respond(subcategories.map { it.toCategoryResponse() })
            }

            // Get full category tree structure
            get("/tree") {
                val categoryTree = categoryRepository.getCategoryTreeStructure()
                call.respond(categoryTree.map { it.toCategoryResponseWithSubcategories() })
            }
        }
    }
}

// Extension functions to convert domain models to response DTOs
private fun Category.toCategoryResponse() = CategoryResponse(
    id = id,
    categoryName = categoryName,
    description = description,
    parentCategoryId = parentCategoryId
)

private fun Category.toCategoryResponseWithSubcategories(): CategoryResponse {
    return CategoryResponse(
        id = id,
        categoryName = categoryName,
        description = description,
        parentCategoryId = parentCategoryId,
        parentCategory = parentCategory?.toCategoryResponse(),
        subCategories = subCategories.map { it.toCategoryResponseWithSubcategories() }
    )
} 