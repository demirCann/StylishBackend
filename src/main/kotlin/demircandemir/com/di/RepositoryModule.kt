package demircandemir.com.di

import demircandemir.com.domain.repository.CategoryRepository
import demircandemir.com.domain.repository.ProductRepository
import demircandemir.com.domain.repository.UserRepository
import demircandemir.com.infrastructure.persistence.CategoryRepositoryImpl
import demircandemir.com.infrastructure.persistence.ProductRepositoryImpl
import demircandemir.com.infrastructure.persistence.UserRepositoryImpl
import org.koin.dsl.module

val repositoryModule = module {
    // Repositories
    single<UserRepository> { UserRepositoryImpl() }
    single<ProductRepository> { ProductRepositoryImpl() }
    single<CategoryRepository> { CategoryRepositoryImpl() }
} 