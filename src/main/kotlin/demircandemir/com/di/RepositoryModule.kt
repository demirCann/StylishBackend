package demircandemir.com.di

import demircandemir.com.domain.repository.UserRepository
import demircandemir.com.infrastructure.persistence.UserRepositoryImpl
import org.koin.dsl.module

val repositoryModule = module {
    // Repositories
    single<UserRepository> { UserRepositoryImpl() }
} 