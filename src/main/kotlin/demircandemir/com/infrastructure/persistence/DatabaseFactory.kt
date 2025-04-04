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

        val jdbcURL: String
        val driver: String
        val user: String
        val password: String

        // Yapılandırma formatını kontrol et
        if (config.propertyOrNull("database.url") != null) {
            // Test veya H2 yapılandırması
            jdbcURL = config.property("database.url").getString()
            driver = config.property("database.driver").getString()
            user = config.property("database.user").getString()
            password = config.property("database.password").getString()

            isTestDatabase = true // H2 kullanılıyorsa test modunda çalışıyoruz
            logger.info("Using H2 test database: $jdbcURL")
        } else {
            // Production PostgreSQL yapılandırması
            val host = config.property("database.host").getString()
            val port = config.property("database.port").getString()
            val name = config.property("database.name").getString()
            user = config.property("database.user").getString()
            password = config.property("database.password").getString()

            jdbcURL = "jdbc:postgresql://$host:$port/$name"
            driver = "org.postgresql.Driver"
            logger.info("Using PostgreSQL database: $jdbcURL")
        }

        database = Database.connect(
            url = jdbcURL,
            driver = driver,
            user = user,
            password = password
        )

        if (!isTestDatabase) {
            runFlywayMigrations(jdbcURL, user, password)
        }
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
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(false)
                .validateMigrationNaming(false)
                .ignoreMigrationPatterns("*:missing")
                .load()
            try {
                try {
                    logger.info("Attempting to repair Flyway schema history...")
                    flyway.repair()
                    logger.info("Flyway repair succeeded")
                } catch (repairEx: Exception) {
                    logger.warn("Flyway repair failed: ${repairEx.message}")
                }
                
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