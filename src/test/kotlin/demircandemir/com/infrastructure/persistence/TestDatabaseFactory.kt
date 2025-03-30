package demircandemir.com.infrastructure.persistence

import demircandemir.com.infrastructure.persistence.tables.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object TestDatabaseFactory {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun initTestDatabase(): Database {
        logger.info("Initializing H2 test database")
        val database = Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        // Create tables in H2
        transaction(database) {
            logger.info("Creating database schema")
            SchemaUtils.create(Users)
            logger.info("Database schema created successfully")
        }

        return database
    }
} 