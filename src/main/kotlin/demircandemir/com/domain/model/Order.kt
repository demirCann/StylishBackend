package demircandemir.com.domain.model

import demircandemir.com.infrastructure.persistence.tables.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class Order(
    val id: Int = 0,
    val userId: Int,
    val addressId: Int,
    val totalAmount: BigDecimal,
    val orderDate: LocalDateTime = LocalDateTime.now(),
    val paymentMethod: String,
    val orderStatus: OrderStatus = OrderStatus.Pending,
    val trackingNumber: String? = null,
    val shippingFee: BigDecimal = BigDecimal.ZERO
)