package demircandemir.com.di

import demircandemir.com.domain.repository.*
import demircandemir.com.infrastructure.persistence.repository.*
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
} 