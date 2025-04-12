package demircandemir.com.di

import demircandemir.com.application.service.AuthService
import demircandemir.com.application.service.EmailService
import demircandemir.com.domain.repository.*
import demircandemir.com.infrastructure.persistence.repository.*
import demircandemir.com.infrastructure.security.JwtConfig
import io.ktor.server.application.*
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val repositoryModule = module {
    // Repositories
    single<UserRepository> { UserRepositoryImpl() }
    single<ProductRepository> { ProductRepositoryImpl() }
    single<CategoryRepository> { CategoryRepositoryImpl() }
    single<OrderRepository> { OrderRepositoryImpl() }
    single<OrderItemRepository> { OrderItemRepositoryImpl() }
    single<CartRepository> { CartRepositoryImpl() }
    single<CartItemRepository> { CartItemRepositoryImpl() }
    single<RefreshTokenRepository> { RefreshTokenRepositoryImpl() }
}

val serviceModule = module {
    // Configuration
    single { (application: Application) -> JwtConfig(application.environment.config) }

    // Services
    single { (application: Application) -> EmailService(application.environment.config) }

    single { (application: Application) ->
        AuthService(
            userRepository = get(),
            refreshTokenRepository = get(),
            jwtConfig = get { parametersOf(application) },
            emailService = get { parametersOf(application) }
        )
    }
} 