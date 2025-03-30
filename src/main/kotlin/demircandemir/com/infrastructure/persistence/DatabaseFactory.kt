package demircandemir.com.infrastructure.persistence

import io.ktor.server.config.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private lateinit var database: Database
    private var isTestDatabase = false

    fun init(config: ApplicationConfig) {
        logger.info("Initializing production database")
        val host = config.property("database.host").getString()
        val port = config.property("database.port").getString()
        val name = config.property("database.name").getString()
        val user = config.property("database.user").getString()
        val password = config.property("database.password").getString()

        val jdbcURL = "jdbc:postgresql://$host:$port/$name"

        database = Database.connect(
            url = jdbcURL,
            driver = "org.postgresql.Driver",
            user = user,
            password = password
        )

        runFlywayMigrations(jdbcURL, user, password)
    }

    fun initTest(testDatabase: Database) {
        logger.info("Initializing test database")
        database = testDatabase
        isTestDatabase = true
    }

    private fun runFlywayMigrations(jdbcURL: String, user: String, password: String) {
        if (!isTestDatabase) {
            logger.info("Running Flyway migrations on PostgreSQL database")
            val flyway = Flyway.configure()
                .dataSource(jdbcURL, user, password)
                .load()
            try {
                flyway.migrate()
                logger.info("Flyway migrations completed successfully")
            } catch (e: Exception) {
                logger.error("Failed to run Flyway migrations", e)
                throw e
            }
        } else {
            logger.info("Skipping Flyway migrations for test database")
        }
    }

    fun <T> dbQuery(block: () -> T): T {
        return org.jetbrains.exposed.sql.transactions.transaction(database) { block() }
    }
} 