package demircandemir.com.infrastructure.persistence

import demircandemir.com.demircandemir.com.infrastructure.persistence.tables.*
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun init(config: ApplicationConfig) {
        logger.info("Initializing database with Flyway migrations")

        val jdbcUrl = config.property("database.jdbcURL").getString()
        val driverClassName = config.property("database.driverClassName").getString()
        val username = config.property("database.username").getString()
        val password = config.property("database.password").getString()
        
        // Run Flyway migrations
        val flyway = Flyway.configure()
            .dataSource(jdbcUrl, username, password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()
            
        try {
            val migrations = flyway.migrate()
            logger.info("Applied ${migrations.migrationsExecuted} migrations successfully")
        } catch (e: FlywayValidateException) {
            logger.warn("Flyway validation failed: ${e.message}")
            logger.warn("Trying to repair schema history...")
            try {
                flyway.repair()
                // Try to migrate again after repair
                val migrations = flyway.migrate()
                logger.info("After repair: Applied ${migrations.migrationsExecuted} migrations successfully")
            } catch (repairEx: Exception) {
                logger.error("Failed to repair schema history: ${repairEx.message}", repairEx)
                throw repairEx
            }
        } catch (e: Exception) {
            logger.error("Failed to apply migrations: ${e.message}", e)
            throw e
        }
        
        // Connect to the database with Exposed
        logger.info("Connecting to database with Exposed")
        Database.connect(
            url = jdbcUrl,
            driver = driverClassName,
            user = username,
            password = password
        )
        
        logger.info("Database initialization completed")
    }
    
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
} 