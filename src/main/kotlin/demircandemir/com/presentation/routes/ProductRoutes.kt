package demircandemir.com.presentation.routes

import demircandemir.com.application.dto.*
import demircandemir.com.domain.model.Gender
import demircandemir.com.domain.model.Product
import demircandemir.com.domain.model.ProductDetail
import demircandemir.com.domain.model.ProductImage
import demircandemir.com.domain.repository.ProductRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.math.BigDecimal

fun Application.productRoutes(productRepository: ProductRepository) {
    routing {
        route("/api/products") {
            // General product listing and search operations - no authentication required
            // Get all products with pagination
            get {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                val showInactive = call.request.queryParameters["showInactive"]?.toBoolean() ?: false

                val products = if (showInactive) {
                    productRepository.getAllProducts(page, pageSize)
                } else {
                    productRepository.getActiveProducts(page, pageSize)
                }

                val productResponses = products.map { it.toProductResponse() }
                call.respond(productResponses)
            }

            // Get product by ID
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID format")

                val product = productRepository.getProductByIdWithDetails(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, "Product not found")

                call.respond(product.toProductResponse())
            }

            // Search products
            get("/search") {
                val query = call.request.queryParameters["q"] ?: ""
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

                val searchResults = productRepository.searchProducts(query, page, pageSize)
                call.respond(searchResults.map { it.toProductResponse() })
            }

            // Get products by category
            get("/category/{categoryId}") {
                val categoryId = call.parameters["categoryId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid category ID format")

                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

                val products = productRepository.getProductsByCategory(categoryId, page, pageSize)
                call.respond(products.map { it.toProductResponse() })
            }

            // Get product details - no authentication required
            route("/{productId}/details") {
                // Get product details
                get {
                    val productId = call.parameters["productId"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid product ID")

                    val details = productRepository.getProductDetails(productId)
                        ?: return@get call.respond(HttpStatusCode.NotFound, "Product details not found")

                    call.respond(details)
                }
            }

            // Get product images - no authentication required
            route("/{productId}/images") {
                // Get all images for a product
                get {
                    val productId = call.parameters["productId"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid product ID")

                    val images = productRepository.getProductImages(productId)
                    call.respond(images)
                }
            }

            // Authentication required for admin operations
            authenticate("auth-jwt") {
                // Admin-only: Create new product
                post {
                    // Admin permission check
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class) ?: ""

                    if (role != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("Admin access required to create products")
                        )
                        return@post
                    }

                    val request = call.receive<ProductRequest>()
                    val product = Product(
                        productName = request.productName,
                        description = request.description,
                        price = BigDecimal(request.price),
                        stockQuantity = request.stockQuantity,
                        categoryId = request.categoryId,
                        brand = request.brand,
                        isActive = request.isActive
                    )

                    val createdProduct = productRepository.createProduct(product)
                    call.respond(HttpStatusCode.Created, createdProduct.toProductResponse())
                }

                // Admin-only: Update product
                put("/{id}") {
                    // Admin permission check
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class) ?: ""

                    if (role != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("Admin access required to update products")
                        )
                        return@put
                    }

                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID format")

                    val existingProduct = productRepository.getProductById(id)
                        ?: return@put call.respond(HttpStatusCode.NotFound, "Product not found")

                    val request = call.receive<ProductRequest>()
                    val updatedProduct = existingProduct.copy(
                        productName = request.productName,
                        description = request.description,
                        price = BigDecimal(request.price),
                        stockQuantity = request.stockQuantity,
                        categoryId = request.categoryId,
                        brand = request.brand,
                        isActive = request.isActive
                    )

                    val result = productRepository.updateProduct(updatedProduct)
                    call.respond(result.toProductResponse())
                }

                // Admin-only: Delete product
                delete("/{id}") {
                    // Admin permission check
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class) ?: ""

                    if (role != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            MessageResponse("Admin access required to delete products")
                        )
                        return@delete
                    }

                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID format")

                    productRepository.deleteProduct(id)
                    call.respond(HttpStatusCode.NoContent)
                }

                // Product details routes - Admin only
                route("/{productId}/details") {
                    // Admin-only: Create or update product details
                    post {
                        // Admin permission check
                        val principal = call.principal<JWTPrincipal>()
                        val role = principal?.getClaim("role", String::class) ?: ""

                        if (role != "ADMIN") {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                MessageResponse("Admin access required to manage product details")
                            )
                            return@post
                        }

                        val productId = call.parameters["productId"]?.toIntOrNull()
                            ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid product ID")

                        val request = call.receive<ProductDetailRequest>()
                        val details = ProductDetail(
                            productId = productId,
                            color = request.color,
                            size = request.size,
                            material = request.material,
                            madeIn = request.madeIn,
                            careInstructions = request.careInstructions,
                            gender = Gender.valueOf(request.gender),
                            season = request.season
                        )

                        val existingDetails = productRepository.getProductDetails(productId)
                        val result = if (existingDetails != null) {
                            productRepository.updateProductDetails(details.copy(id = existingDetails.id))
                        } else {
                            productRepository.createProductDetails(details)
                        }

                        call.respond(HttpStatusCode.Created, result)
                    }
                }

                // Product images routes - Admin only
                route("/{productId}/images") {
                    // Admin-only: Add a new image
                    post {
                        // Admin permission check
                        val principal = call.principal<JWTPrincipal>()
                        val role = principal?.getClaim("role", String::class) ?: ""

                        if (role != "ADMIN") {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                MessageResponse("Admin access required to add product images")
                            )
                            return@post
                        }

                        val productId = call.parameters["productId"]?.toIntOrNull()
                            ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid product ID")

                        val request = call.receive<ProductImageRequest>()
                        val image = ProductImage(
                            productId = productId,
                            imageUrl = request.imageUrl,
                            isPrimary = request.isPrimary,
                            displayOrder = request.displayOrder
                        )

                        val result = productRepository.addProductImage(image)
                        call.respond(HttpStatusCode.Created, result)
                    }

                    // Admin-only: Delete an image
                    delete("/{imageId}") {
                        // Admin permission check
                        val principal = call.principal<JWTPrincipal>()
                        val role = principal?.getClaim("role", String::class) ?: ""

                        if (role != "ADMIN") {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                MessageResponse("Admin access required to delete product images")
                            )
                            return@delete
                        }

                        val imageId = call.parameters["imageId"]?.toIntOrNull()
                            ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid image ID")

                        productRepository.deleteProductImage(imageId)
                        call.respond(HttpStatusCode.NoContent)
                    }

                    // Admin-only: Set primary image
                    put("/{imageId}/primary") {
                        // Admin permission check
                        val principal = call.principal<JWTPrincipal>()
                        val role = principal?.getClaim("role", String::class) ?: ""

                        if (role != "ADMIN") {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                MessageResponse("Admin access required to set primary product image")
                            )
                            return@put
                        }

                        val productId = call.parameters["productId"]?.toIntOrNull()
                            ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid product ID")

                        val imageId = call.parameters["imageId"]?.toIntOrNull()
                            ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid image ID")

                        productRepository.setProductPrimaryImage(imageId, productId)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}

// Extension function to convert domain model to response DTO
private fun Product.toProductResponse() = ProductResponse(
    id = id,
    productName = productName,
    description = description,
    price = price.toDouble(),
    stockQuantity = stockQuantity,
    categoryId = categoryId,
    brand = brand,
    dateAdded = dateAdded.toString(),
    isActive = isActive
) 