package demircandemir.com

import demircandemir.com.application.serialization.appSerializersModule
import demircandemir.com.infrastructure.persistence.DatabaseFactory
import demircandemir.com.infrastructure.persistence.UserRepositoryImpl
import demircandemir.com.presentation.routes.userRoutes
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    // Initialize database with Flyway migrations
    DatabaseFactory.init(environment.config)

    // Dependency injection
    val userRepository = UserRepositoryImpl()

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
    userRoutes(userRepository)
} 