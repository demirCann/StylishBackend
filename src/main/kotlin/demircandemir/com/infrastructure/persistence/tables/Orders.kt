package demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Orders : IntIdTable("ORDERS") {
    val userId = reference("user_id", Users)
    val addressId = reference("address_id", Addresses)
    val totalAmount = decimal("total_amount", 10, 2)
    val orderDate = datetime("order_date").default(LocalDateTime.now())
    val paymentMethod = varchar("payment_method", 50)
    val orderStatus = enumerationByName("order_status", 10, OrderStatus::class).default(OrderStatus.Pending)
    val trackingNumber = varchar("tracking_number", 50).nullable()
    val shippingProvider = varchar("shipping_provider", 50).nullable()
    val shippingFee = decimal("shipping_fee", 10, 2).default(0.toBigDecimal())

    init {
        index(false, userId)
        index(false, orderStatus)
    }
}

enum class OrderStatus {
    Pending, Confirmed, Processing, Shipped, Delivered, Cancelled, Returned
}