package demircandemir.com.domain.repository

import demircandemir.com.domain.model.Product
import demircandemir.com.domain.model.ProductDetail
import demircandemir.com.domain.model.ProductImage

interface ProductRepository {
    // Product CRUD operations
    suspend fun createProduct(product: Product): Product
    suspend fun getProductById(id: Int): Product?
    suspend fun getProductByIdWithDetails(id: Int): Product?
    suspend fun updateProduct(product: Product): Product
    suspend fun deleteProduct(id: Int)
    suspend fun getAllProducts(page: Int, pageSize: Int): List<Product>
    suspend fun getActiveProducts(page: Int, pageSize: Int): List<Product>
    suspend fun getProductsByCategory(categoryId: Int, page: Int, pageSize: Int): List<Product>
    suspend fun searchProducts(query: String, page: Int, pageSize: Int): List<Product>
    suspend fun getProductCount(): Int
    suspend fun getProductCountByCategory(categoryId: Int): Int

    // Product Details operations
    suspend fun createProductDetails(details: ProductDetail): ProductDetail
    suspend fun getProductDetails(productId: Int): ProductDetail?
    suspend fun updateProductDetails(details: ProductDetail): ProductDetail

    // Product Images operations
    suspend fun addProductImage(image: ProductImage): ProductImage
    suspend fun getProductImages(productId: Int): List<ProductImage>
    suspend fun updateProductImage(image: ProductImage): ProductImage
    suspend fun deleteProductImage(id: Int)
    suspend fun setProductPrimaryImage(id: Int, productId: Int)
} 