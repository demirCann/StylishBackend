package demircandemir.com.infrastructure.persistence

import demircandemir.com.infrastructure.persistence.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object TestDatabaseFactory {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var isInitialized = false

    fun initTestDatabase() {
        if (isInitialized) return

        logger.info("Initializing H2 test database")
        val testDatabase = Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        // Exposed üzerinden tabloları oluştur
        transaction(testDatabase) {
            SchemaUtils.create(
                Users,
                Categories,
                Products,
                ProductDetails,
                ProductImages,
                Addresses,
                Carts,
                CartItems,
                Orders,
                OrderItems,
                Reviews,
                Sizes,
                Colors
            )
        }

        DatabaseFactory.initTest(testDatabase)
        isInitialized = true
        logger.info("H2 test database initialized successfully")
    }
} 