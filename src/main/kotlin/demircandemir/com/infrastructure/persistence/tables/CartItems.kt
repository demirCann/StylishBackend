package demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object CartItems : IntIdTable("CART_ITEMS") {
    val cartId = reference("cart_id", Carts)
    val productId = reference("product_id", Products)
    val quantity = integer("quantity").default(1)
    val sizeId = reference("size_id", Sizes).nullable()
    val colorId = reference("color_id", Colors).nullable()
    val unitPrice = decimal("unit_price", 10, 2)

    init {
        index(false, cartId, productId)
    }
}