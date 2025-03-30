package demircandemir.com

import demircandemir.com.application.serialization.appSerializersModule
import demircandemir.com.di.repositoryModule
import demircandemir.com.domain.repository.UserRepository
import demircandemir.com.infrastructure.persistence.DatabaseFactory
import demircandemir.com.presentation.routes.addressRoutes
import demircandemir.com.presentation.routes.rootRoutes
import demircandemir.com.presentation.routes.userRoutes
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

    // Get dependencies
    val userRepository: UserRepository by inject()

    // Content negotiation
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            serializersModule = appSerializersModule
        })
    }

    // Routes
    rootRoutes()
    userRoutes(userRepository)
    addressRoutes(userRepository)
} 