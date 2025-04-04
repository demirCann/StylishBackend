package demircandemir.com.infrastructure.persistence.repository

import demircandemir.com.domain.model.Gender
import demircandemir.com.domain.model.Product
import demircandemir.com.domain.model.ProductDetail
import demircandemir.com.domain.model.ProductImage
import demircandemir.com.domain.repository.ProductRepository
import demircandemir.com.infrastructure.persistence.DatabaseFactory
import demircandemir.com.infrastructure.persistence.tables.Categories
import demircandemir.com.infrastructure.persistence.tables.ProductDetails
import demircandemir.com.infrastructure.persistence.tables.ProductImages
import demircandemir.com.infrastructure.persistence.tables.Products
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory

class ProductRepositoryImpl : ProductRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // Product CRUD operations
    override suspend fun createProduct(product: Product): Product = DatabaseFactory.dbQuery {
        logger.info("Creating product: ${product.productName}")

        val insertStatement = Products.insert {
            it[productName] = product.productName
            it[description] = product.description
            it[price] = product.price
            it[stockQuantity] = product.stockQuantity
            it[categoryId] = product.categoryId
            it[brand] = product.brand
            it[dateAdded] = product.dateAdded
            it[isActive] = product.isActive
        }

        val productId = insertStatement[Products.id].value
        logger.info("Created product with ID: $productId")

        // Insert product details if provided
        product.details?.let { details ->
            ProductDetails.insert {
                it[ProductDetails.productId] = productId
                it[color] = details.color
                it[size] = details.size
                it[material] = details.material
                it[madeIn] = details.madeIn
                it[careInstructions] = details.careInstructions
                it[gender] = Gender.valueOf(details.gender.name)
                it[season] = details.season
            }
        }

        // Insert product images if provided
        product.images.forEach { image ->
            ProductImages.insert {
                it[ProductImages.productId] = productId
                it[imageUrl] = image.imageUrl
                it[isPrimary] = image.isPrimary
                it[displayOrder] = image.displayOrder
            }
        }

        product.copy(id = productId)
    }

    override suspend fun getProductById(id: Int): Product? = DatabaseFactory.dbQuery {
        Products.selectAll()
            .where { Products.id eq id }
            .map { it.toProduct() }
            .singleOrNull()
    }

    override suspend fun getProductByIdWithDetails(id: Int): Product? = DatabaseFactory.dbQuery {
        val product = Products.selectAll()
            .where { Products.id eq id }
            .map { it.toProduct() }
            .singleOrNull() ?: return@dbQuery null

        // Get product details
        val details = ProductDetails.selectAll()
            .where { ProductDetails.productId eq id }
            .map { it.toProductDetails() }
            .singleOrNull()

        // Get product images
        val images = ProductImages.selectAll()
            .where { ProductImages.productId eq id }
            .orderBy(ProductImages.displayOrder to SortOrder.ASC)
            .map { it.toProductImage() }

        // Get category if exists
        val category = product.categoryId?.let { categoryId ->
            Categories.selectAll()
                .where { Categories.id eq categoryId }
                .map { it.toCategory() }
                .singleOrNull()
        }

        product.copy(details = details, images = images, category = category)
    }

    override suspend fun updateProduct(product: Product): Product = DatabaseFactory.dbQuery {
        Products.update({ Products.id eq product.id }) {
            it[productName] = product.productName
            it[description] = product.description
            it[price] = product.price
            it[stockQuantity] = product.stockQuantity
            it[categoryId] = product.categoryId
            it[brand] = product.brand
            it[isActive] = product.isActive
        }
        product
    }

    override suspend fun deleteProduct(id: Int): Unit = DatabaseFactory.dbQuery {
        // Delete product images and details first (due to foreign key constraints)
        ProductImages.deleteWhere { productId eq id }
        ProductDetails.deleteWhere { productId eq id }
        Products.deleteWhere { Products.id eq id }
    }

    override suspend fun getAllProducts(page: Int, pageSize: Int): List<Product> = DatabaseFactory.dbQuery {
        Products.selectAll()
            .orderBy(Products.id to SortOrder.DESC)
            .limit(pageSize).offset(start = ((page - 1) * pageSize).toLong())
            .map { it.toProduct() }
    }

    override suspend fun getActiveProducts(page: Int, pageSize: Int): List<Product> = DatabaseFactory.dbQuery {
        Products.selectAll()
            .where { Products.isActive eq true }
            .orderBy(Products.id to SortOrder.DESC)
            .limit(pageSize).offset(start = ((page - 1) * pageSize).toLong())
            .map { it.toProduct() }
    }

    override suspend fun getProductsByCategory(categoryId: Int, page: Int, pageSize: Int): List<Product> =
        DatabaseFactory.dbQuery {
            Products.selectAll()
                .where {
                    (Products.categoryId eq categoryId) and (Products.isActive eq true)
                }
                .orderBy(Products.id to SortOrder.DESC)
                .limit(pageSize).offset(start = ((page - 1) * pageSize).toLong())
                .map { it.toProduct() }
        }

    override suspend fun searchProducts(query: String, page: Int, pageSize: Int): List<Product> =
        DatabaseFactory.dbQuery {
            val lowerQuery = "%${query.lowercase()}%"
            Products.selectAll()
                .where {
                    (Products.productName.lowerCase() like lowerQuery or
                            (Products.description.isNotNull() and (Products.description.lowerCase() like lowerQuery)) or
                            (Products.brand.isNotNull() and (Products.brand.lowerCase() like lowerQuery))) and
                            (Products.isActive eq true)
                }
                .orderBy(Products.id to SortOrder.DESC)
                .limit(pageSize).offset(start = ((page - 1) * pageSize).toLong())
                .map { it.toProduct() }
        }

    override suspend fun getProductCount(): Int = DatabaseFactory.dbQuery {
        Products.selectAll().count().toInt()
    }

    override suspend fun getProductCountByCategory(categoryId: Int): Int = DatabaseFactory.dbQuery {
        Products.selectAll()
            .where { Products.categoryId eq categoryId }
            .count().toInt()
    }

    // Product Details operations
    override suspend fun createProductDetails(details: ProductDetail): ProductDetail = DatabaseFactory.dbQuery {
        val insertStatement = ProductDetails.insert {
            it[productId] = details.productId
            it[color] = details.color
            it[size] = details.size
            it[material] = details.material
            it[madeIn] = details.madeIn
            it[careInstructions] = details.careInstructions
            it[gender] = Gender.valueOf(details.gender.name)
            it[season] = details.season
        }

        val detailsId = insertStatement[ProductDetails.id].value
        details.copy(id = detailsId)
    }

    override suspend fun getProductDetails(productId: Int): ProductDetail? = DatabaseFactory.dbQuery {
        ProductDetails.selectAll()
            .where { ProductDetails.productId eq productId }
            .map { it.toProductDetails() }
            .singleOrNull()
    }

    override suspend fun updateProductDetails(details: ProductDetail): ProductDetail = DatabaseFactory.dbQuery {
        ProductDetails.update({ ProductDetails.id eq details.id }) {
            it[color] = details.color
            it[size] = details.size
            it[material] = details.material
            it[madeIn] = details.madeIn
            it[careInstructions] = details.careInstructions
            it[gender] = Gender.valueOf(details.gender.name)
            it[season] = details.season
        }
        details
    }

    // Product Images operations
    override suspend fun addProductImage(image: ProductImage): ProductImage = DatabaseFactory.dbQuery {
        // If this is set as primary, reset all other primary images for this product
        if (image.isPrimary) {
            ProductImages.update({
                (ProductImages.productId eq image.productId) and (ProductImages.isPrimary eq true)
            }) {
                it[isPrimary] = false
            }
        }

        val insertStatement = ProductImages.insert {
            it[productId] = image.productId
            it[imageUrl] = image.imageUrl
            it[isPrimary] = image.isPrimary
            it[displayOrder] = image.displayOrder
        }

        val imageId = insertStatement[ProductImages.id].value
        image.copy(id = imageId)
    }

    override suspend fun getProductImages(productId: Int): List<ProductImage> = DatabaseFactory.dbQuery {
        ProductImages.selectAll()
            .where { ProductImages.productId eq productId }
            .orderBy(ProductImages.displayOrder to SortOrder.ASC)
            .map { it.toProductImage() }
    }

    override suspend fun updateProductImage(image: ProductImage): ProductImage = DatabaseFactory.dbQuery {
        // If this is set as primary, reset all other primary images for this product
        if (image.isPrimary) {
            ProductImages.update({
                (ProductImages.productId eq image.productId) and
                        (ProductImages.id neq image.id) and
                        (ProductImages.isPrimary eq true)
            }) {
                it[isPrimary] = false
            }
        }

        ProductImages.update({ ProductImages.id eq image.id }) {
            it[imageUrl] = image.imageUrl
            it[isPrimary] = image.isPrimary
            it[displayOrder] = image.displayOrder
        }
        image
    }

    override suspend fun deleteProductImage(id: Int): Unit = DatabaseFactory.dbQuery {
        ProductImages.deleteWhere { ProductImages.id eq id }
    }

    override suspend fun setProductPrimaryImage(id: Int, productId: Int): Unit = DatabaseFactory.dbQuery {
        // --- Added Validation --- 
        // Check if the image exists and belongs to the correct product
        val imageExists = ProductImages.selectAll()
            .where { ProductImages.id eq id }
            .map { it[ProductImages.productId].value }
            .singleOrNull()

        if (imageExists == null) {
            throw IllegalArgumentException("Image with id $id not found.")
        }

        if (imageExists != productId) {
            throw IllegalArgumentException("Image with id $id does not belong to product with id $productId.")
        }
        // --- End Validation ---

        // First, reset all primary flags for this product
        ProductImages.update({
            (ProductImages.productId eq productId) and (ProductImages.isPrimary eq true)
        }) {
            it[isPrimary] = false
        }

        // Then set the specified image as primary
        ProductImages.update({ ProductImages.id eq id }) {
            it[isPrimary] = true
        }
    }

    // Mapping functions
    private fun ResultRow.toProduct() = Product(
        id = this[Products.id].value,
        productName = this[Products.productName],
        description = this[Products.description],
        price = this[Products.price],
        stockQuantity = this[Products.stockQuantity],
        categoryId = this[Products.categoryId]?.value,
        brand = this[Products.brand],
        dateAdded = this[Products.dateAdded],
        isActive = this[Products.isActive]
    )

    private fun ResultRow.toProductDetails() = ProductDetail(
        id = this[ProductDetails.id].value,
        productId = this[ProductDetails.productId].value,
        color = this[ProductDetails.color],
        size = this[ProductDetails.size],
        material = this[ProductDetails.material],
        madeIn = this[ProductDetails.madeIn],
        careInstructions = this[ProductDetails.careInstructions],
        gender = Gender.valueOf(this[ProductDetails.gender].name),
        season = this[ProductDetails.season]
    )

    private fun ResultRow.toProductImage() = ProductImage(
        id = this[ProductImages.id].value,
        productId = this[ProductImages.productId].value,
        imageUrl = this[ProductImages.imageUrl],
        isPrimary = this[ProductImages.isPrimary],
        displayOrder = this[ProductImages.displayOrder]
    )

    private fun ResultRow.toCategory() = demircandemir.com.domain.model.Category(
        id = this[Categories.id].value,
        categoryName = this[Categories.categoryName],
        description = this[Categories.description],
        parentCategoryId = this[Categories.parentCategoryId]?.value
    )
} 