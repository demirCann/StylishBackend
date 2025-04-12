package demircandemir.com

import demircandemir.com.application.serialization.appSerializersModule
import demircandemir.com.application.service.AuthService
import demircandemir.com.di.repositoryModule
import demircandemir.com.di.serviceModule
import demircandemir.com.domain.repository.*
import demircandemir.com.infrastructure.persistence.DatabaseFactory
import demircandemir.com.infrastructure.plugins.configureSecurity
import demircandemir.com.infrastructure.security.JwtConfig
import demircandemir.com.presentation.routes.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import org.koin.core.parameter.parametersOf
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
        modules(repositoryModule, serviceModule)
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

    // Get JWT configuration
    val jwtConfig by inject<JwtConfig> { parametersOf(this@module) }

    // Configure security (JWT and CORS)
    configureSecurity(jwtConfig)

    // Get dependencies
    val userRepository: UserRepository by inject()
    val productRepository: ProductRepository by inject()
    val categoryRepository: CategoryRepository by inject()
    val cartRepository: CartRepository by inject()
    val cartItemRepository: CartItemRepository by inject()
    val orderRepository: OrderRepository by inject()
    val orderItemRepository: OrderItemRepository by inject()

    // Auth service
    val authService: AuthService by inject { parametersOf(this@module) }

    // Routes
    rootRoutes()
    authRoutes(authService)
    userRoutes(userRepository)
    addressRoutes(userRepository)
    productRoutes(productRepository)
    categoryRoutes(categoryRepository)
    cartRoutes(cartRepository, cartItemRepository)
    cartItemRoutes(cartItemRepository, cartRepository, productRepository)
    orderRoutes(orderRepository, orderItemRepository, productRepository, userRepository)
    orderItemRoutes(orderItemRepository, orderRepository)
} 