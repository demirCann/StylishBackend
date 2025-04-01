package demircandemir.com.infrastructure.persistence

import demircandemir.com.domain.model.Gender
import demircandemir.com.domain.model.Product
import demircandemir.com.domain.model.ProductDetail
import demircandemir.com.domain.model.ProductImage
import demircandemir.com.infrastructure.persistence.tables.Categories
import demircandemir.com.infrastructure.persistence.tables.ProductDetails
import demircandemir.com.infrastructure.persistence.tables.ProductImages
import demircandemir.com.infrastructure.persistence.tables.Products
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductRepositoryTest {
    private lateinit var repository: ProductRepositoryImpl
    private lateinit var testProduct: Product
    private lateinit var testProductDetail: ProductDetail
    private lateinit var testProductImage: ProductImage

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            TestDatabaseFactory.initTestDatabase()
            transaction {
                SchemaUtils.create(Products, ProductDetails, ProductImages, Categories)
            }
        }
    }

    @BeforeEach
    fun setupTestData() {
        repository = ProductRepositoryImpl()
        transaction {
            SchemaUtils.drop(ProductImages, ProductDetails, Products, Categories)
            SchemaUtils.create(Categories, Products, ProductDetails, ProductImages)
        }

        testProduct = Product(
            productName = "Test Product",
            description = "Test Description",
            price = BigDecimal("99.99"),
            stockQuantity = 100,
            brand = "Test Brand",
            isActive = true,
            categoryId = null
        )

        testProductDetail = ProductDetail(
            productId = 0,
            color = "Red",
            size = "M",
            material = "Cotton",
            madeIn = "Turkey",
            careInstructions = "Machine wash",
            gender = Gender.Male,
            season = "Summer"
        )

        testProductImage = ProductImage(
            productId = 0,
            imageUrl = "http://test.com/image.jpg",
            isPrimary = true,
            displayOrder = 1
        )
    }

    @Test
    fun `createProduct should create a new product`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        assertEquals(testProduct.productName, createdProduct.productName)
        assertEquals(testProduct.price, createdProduct.price)
        assertTrue(createdProduct.id > 0)
    }

    @Test
    fun `getProductById should return product when exists`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val retrievedProduct = repository.getProductById(createdProduct.id)
        assertEquals(createdProduct.id, retrievedProduct?.id)
        assertEquals(createdProduct.productName, retrievedProduct?.productName)
    }

    @Test
    fun `getProductById should return null when product does not exist`() = runBlocking {
        val retrievedProduct = repository.getProductById(999)
        assertNull(retrievedProduct)
    }

    @Test
    fun `updateProduct should update existing product`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val updatedProduct = createdProduct.copy(
            productName = "Updated Product",
            price = BigDecimal("149.99")
        )
        val result = repository.updateProduct(updatedProduct)
        assertEquals("Updated Product", result.productName)
        assertEquals(BigDecimal("149.99"), result.price)
    }

    @Test
    fun `deleteProduct should delete existing product`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        repository.deleteProduct(createdProduct.id)
        val deletedProduct = repository.getProductById(createdProduct.id)
        assertNull(deletedProduct)
    }

    @Test
    fun `createProductDetails should create product details`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val details = testProductDetail.copy(productId = createdProduct.id)
        val createdDetails = repository.createProductDetails(details)
        assertEquals(details.color, createdDetails.color)
        assertEquals(details.size, createdDetails.size)
        assertTrue(createdDetails.id > 0)
    }

    @Test
    fun `getProductDetails should return details when they exist`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val details = testProductDetail.copy(productId = createdProduct.id)
        val createdDetails = repository.createProductDetails(details)
        val retrievedDetails = repository.getProductDetails(createdProduct.id)
        assertEquals(createdDetails.id, retrievedDetails?.id)
        assertEquals(createdDetails.color, retrievedDetails?.color)
    }

    @Test
    fun `getProductDetails should return null when no details exist`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val retrievedDetails = repository.getProductDetails(createdProduct.id)
        assertNull(retrievedDetails)
    }

    @Test
    fun `updateProductDetails should update existing details`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val details = testProductDetail.copy(productId = createdProduct.id)
        val createdDetails = repository.createProductDetails(details)

        val updatedDetailsData = createdDetails.copy(
            color = "Blue",
            size = "L"
        )
        val result = repository.updateProductDetails(updatedDetailsData)
        assertNotNull(result)
        assertEquals("Blue", result.color)
        assertEquals("L", result.size)

        val retrievedAfterUpdate = repository.getProductDetails(createdProduct.id)
        assertEquals("Blue", retrievedAfterUpdate?.color)
    }

    @Test
    fun `deleting product should also delete its details`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val details = testProductDetail.copy(productId = createdProduct.id)
        val createdDetails = repository.createProductDetails(details)
        assertNotNull(repository.getProductDetails(createdProduct.id))

        repository.deleteProduct(createdProduct.id)

        assertNull(repository.getProductById(createdProduct.id))
        assertNull(repository.getProductDetails(createdProduct.id))
    }

    @Test
    fun `addProductImage should add new image`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val image = testProductImage.copy(productId = createdProduct.id)
        val createdImage = repository.addProductImage(image)
        assertEquals(image.imageUrl, createdImage.imageUrl)
        assertEquals(image.isPrimary, createdImage.isPrimary)
        assertTrue(createdImage.id > 0)
    }

    @Test
    fun `getProductImages should return all images for a product`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val image1 = testProductImage.copy(productId = createdProduct.id)
        val image2 = testProductImage.copy(
            productId = createdProduct.id,
            imageUrl = "http://test.com/image2.jpg",
            displayOrder = 2
        )
        repository.addProductImage(image1)
        repository.addProductImage(image2)
        val images = repository.getProductImages(createdProduct.id)
        assertEquals(2, images.size)
    }

    @Test
    fun `getProductImages should return empty list when no images exist`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val images = repository.getProductImages(createdProduct.id)
        assertTrue(images.isEmpty())
    }

    @Test
    fun `updateProductImage should update existing image`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val image = testProductImage.copy(productId = createdProduct.id)
        val createdImage = repository.addProductImage(image)

        val updatedImageData = createdImage.copy(
            imageUrl = "http://new.url/image.png",
            displayOrder = 5
        )
        val result = repository.updateProductImage(updatedImageData)
        assertNotNull(result)
        assertEquals("http://new.url/image.png", result.imageUrl)
        assertEquals(5, result.displayOrder)

        val retrievedAfterUpdate = repository.getProductImages(createdProduct.id).find { it.id == createdImage.id }
        assertEquals("http://new.url/image.png", retrievedAfterUpdate?.imageUrl)
    }

    @Test
    fun `deleteProductImage should remove image`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val image = testProductImage.copy(productId = createdProduct.id)
        val createdImage = repository.addProductImage(image)
        assertEquals(1, repository.getProductImages(createdProduct.id).size)

        repository.deleteProductImage(createdImage.id)

        val retrievedAfterDelete = repository.getProductImages(createdProduct.id)
        assertTrue(retrievedAfterDelete.isEmpty())
    }

    @Test
    fun `setProductPrimaryImage should update primary image`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val image1 = testProductImage.copy(productId = createdProduct.id)
        val image2 = testProductImage.copy(
            productId = createdProduct.id,
            imageUrl = "http://test.com/image2.jpg",
            displayOrder = 2
        )
        val createdImage1 = repository.addProductImage(image1)
        val createdImage2 = repository.addProductImage(image2)

        repository.setProductPrimaryImage(createdImage2.id, createdProduct.id)
        val updatedImages = repository.getProductImages(createdProduct.id)
        val primaryImage = updatedImages.find { it.isPrimary }
        assertEquals(createdImage2.id, primaryImage?.id)
    }

    @Test
    fun `setProductPrimaryImage should only set one primary image per product`() = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        val image1 = repository.addProductImage(
            testProductImage.copy(
                productId = createdProduct.id,
                isPrimary = true,
                displayOrder = 1
            )
        )
        val image2 = repository.addProductImage(
            testProductImage.copy(
                productId = createdProduct.id,
                imageUrl = "url2",
                isPrimary = false,
                displayOrder = 2
            )
        )
        val image3 = repository.addProductImage(
            testProductImage.copy(
                productId = createdProduct.id,
                imageUrl = "url3",
                isPrimary = false,
                displayOrder = 3
            )
        )

        repository.setProductPrimaryImage(image2.id, createdProduct.id)
        var images = repository.getProductImages(createdProduct.id)
        assertEquals(image2.id, images.find { it.isPrimary }?.id)
        assertEquals(1, images.count { it.isPrimary })

        repository.setProductPrimaryImage(image3.id, createdProduct.id)
        images = repository.getProductImages(createdProduct.id)
        assertEquals(image3.id, images.find { it.isPrimary }?.id)
        assertEquals(1, images.count { it.isPrimary })
    }

    @Test
    fun `setProductPrimaryImage should fail for invalid imageId`(): Unit = runBlocking {
        val createdProduct = repository.createProduct(testProduct)
        assertFailsWith<IllegalArgumentException> {
            repository.setProductPrimaryImage(999, createdProduct.id)
        }
    }

    @Test
    fun `setProductPrimaryImage should fail for image of different product`(): Unit = runBlocking {
        val product1 = repository.createProduct(testProduct.copy(productName = "P1"))
        val product2 = repository.createProduct(testProduct.copy(productName = "P2"))
        val image1 = repository.addProductImage(testProductImage.copy(productId = product1.id))
        val image2 = repository.addProductImage(testProductImage.copy(productId = product2.id))

        assertFailsWith<IllegalArgumentException> {
            repository.setProductPrimaryImage(image1.id, product2.id)
        }
    }

    @Test
    fun `searchProducts should return matching products`() = runBlocking {
        val product1 = testProduct.copy(productName = "Blue Shirt")
        val product2 = testProduct.copy(productName = "Red Pants")
        repository.createProduct(product1)
        repository.createProduct(product2)

        val searchResults = repository.searchProducts("Blue", 1, 10)
        assertEquals(1, searchResults.size)
        assertEquals("Blue Shirt", searchResults[0].productName)
    }

    @Test
    fun `searchProducts should respect pagination`() = runBlocking {
        repository.createProduct(testProduct.copy(productName = "Apple iPhone"))
        repository.createProduct(testProduct.copy(productName = "Apple iPad"))
        repository.createProduct(testProduct.copy(productName = "Samsung Galaxy"))

        var results = repository.searchProducts("Apple", 1, 2)
        assertEquals(2, results.size)
        assertTrue(results.all { it.productName.contains("Apple") })

        results = repository.searchProducts("Apple", 2, 2)
        assertEquals(0, results.size)

        results = repository.searchProducts("Apple", 1, 1)
        assertEquals(1, results.size)
    }

    @Test
    fun `searchProducts should return empty list when no match found`() = runBlocking {
        repository.createProduct(testProduct.copy(productName = "Apple iPhone"))
        val results = repository.searchProducts("NonExistent", 1, 10)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `getProductsByCategory should return products in category`() = runBlocking {
        val categoryId = transaction {
            Categories.insert { it[categoryName] = "Test Cat for GetByCategory" }[Categories.id].value
        }

        val product1 = testProduct.copy(categoryId = categoryId)
        val product2 = testProduct.copy(categoryId = categoryId)
        repository.createProduct(product1)
        repository.createProduct(product2)

        val categoryProducts = repository.getProductsByCategory(categoryId, 1, 10)
        assertEquals(2, categoryProducts.size)
        assertTrue(categoryProducts.all { it.categoryId == categoryId })
    }

    @Test
    fun `getProductsByCategory should respect pagination`() = runBlocking {
        val categoryId1 = transaction {
            Categories.insert { it[categoryName] = "Test Cat 1 for Pagination" }[Categories.id].value
        }
        val categoryId2 = transaction {
            Categories.insert { it[categoryName] = "Test Cat 2 for Pagination" }[Categories.id].value
        }

        repository.createProduct(testProduct.copy(categoryId = categoryId1, productName = "P1"))
        repository.createProduct(testProduct.copy(categoryId = categoryId1, productName = "P2"))
        repository.createProduct(testProduct.copy(categoryId = categoryId1, productName = "P3"))
        repository.createProduct(testProduct.copy(categoryId = categoryId2, productName = "P4"))

        var results = repository.getProductsByCategory(categoryId1, 1, 2)
        assertEquals(2, results.size)
        assertTrue(results.all { it.categoryId == categoryId1 })

        results = repository.getProductsByCategory(categoryId1, 2, 2)
        assertEquals(1, results.size)
        assertEquals(categoryId1, results[0].categoryId)
        assertEquals("P1", results[0].productName)
    }
} 