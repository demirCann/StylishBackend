package demircandemir.com

import demircandemir.com.application.serialization.appSerializersModule
import demircandemir.com.di.repositoryModule
import demircandemir.com.domain.repository.*
import demircandemir.com.infrastructure.persistence.DatabaseFactory
import demircandemir.com.presentation.routes.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Initialize database with Flyway migrations
    DatabaseFactory.init(environment.config)

    // Install Koin
    install(Koin) {
        slf4jLogger()
        modules(repositoryModule)
    }

    // Content negotiation
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            serializersModule = appSerializersModule
        })
    }

    // Get dependencies
    val userRepository: UserRepository by inject()
    val productRepository: ProductRepository by inject()
    val categoryRepository: CategoryRepository by inject()
    val cartRepository: CartRepository by inject()
    val cartItemRepository: CartItemRepository by inject()
    val orderRepository: OrderRepository by inject()
    val orderItemRepository: OrderItemRepository by inject()

    // Routes
    rootRoutes()
    userRoutes(userRepository)
    addressRoutes(userRepository)
    productRoutes(productRepository)
    categoryRoutes(categoryRepository)
    cartRoutes(cartRepository, cartItemRepository)
    cartItemRoutes(cartItemRepository, cartRepository, productRepository)
    orderRoutes(orderRepository, orderItemRepository, productRepository, userRepository)
    orderItemRoutes(orderItemRepository, orderRepository)
} 