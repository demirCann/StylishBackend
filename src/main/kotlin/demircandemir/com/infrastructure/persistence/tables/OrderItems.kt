package demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object OrderItems : IntIdTable("ORDER_ITEMS") {
    val orderId = reference("order_id", Orders)
    val productId = reference("product_id", Products)
    val quantity = integer("quantity").default(1)
    val unitPrice = decimal("unit_price", 10, 2)
    val sizeId = reference("size_id", Sizes).nullable()
    val colorId = reference("color_id", Colors).nullable()

    init {
        index(false, orderId)
    }
}