package demircandemir.com.infrastructure.persistence

import demircandemir.com.infrastructure.persistence.tables.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseRepositoryTest {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

    // List of all known tables in reverse dependency order for deletion
    private val tablesToDeleteInOrder: Array<Table> = arrayOf(
        OrderItems, CartItems, ProductImages, ProductDetails, RefreshTokens,
        Orders, Carts, Reviews, Products, Addresses, Categories, Users
    )

    // List of all known tables in drop order for schema management
    private val allPossibleTablesInDropOrder: Array<Table> = arrayOf(
        OrderItems,
        CartItems,
        ProductImages,
        ProductDetails,
        RefreshTokens,
        Orders,
        Carts,
        Reviews,
        Products,
        Sizes,
        Colors,
        Addresses,
        Categories, // Categories might have self-references, drop before Users
        Users
    )

    // List of all tables to create
    private val allTablesToCreate: Array<Table> = arrayOf(
        Users, Categories, Addresses, Colors, Sizes, Products, Reviews,
        Carts, Orders, RefreshTokens, ProductDetails, ProductImages,
        CartItems, OrderItems
    )

    @BeforeAll
    fun setupDatabase() {
        logger.info("Initializing test database and creating schema for ${this::class.java.simpleName}")
        TestDatabaseFactory.initTestDatabase()
        transaction {
            // Ensure a clean slate by dropping first, then create all tables
            SchemaUtils.drop(*allPossibleTablesInDropOrder, inBatch = true)
            SchemaUtils.create(*allTablesToCreate)
        }
    }

    @BeforeEach
    fun cleanDataBeforeTest() {
        logger.debug("Cleaning data for test in ${this::class.java.simpleName}")
        // Delete data from all tables in the correct order before each test
        transaction {
            tablesToDeleteInOrder.forEach { table ->
                try {
                    table.deleteAll()
                } catch (e: Exception) {
                    // Log warning but continue, table might not exist in all test contexts if schema creation differs
                    logger.warn("Could not delete from table ${table.tableName}: ${e.message}")
                }
            }
        }
    }

    @AfterAll
    fun tearDownDatabase() {
        logger.info("Cleaning up test database after ${this::class.java.simpleName}")
        transaction {
            SchemaUtils.drop(*allPossibleTablesInDropOrder, inBatch = true)
        }
    }
}