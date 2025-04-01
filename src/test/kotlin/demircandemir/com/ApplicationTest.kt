package demircandemir.com

import demircandemir.com.infrastructure.persistence.TestDatabaseFactory
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Before
    fun setUp() {
        // Initialize test database using TestDatabaseFactory
        TestDatabaseFactory.initTestDatabase()
    }

    @After
    fun tearDown() {
        // Cleanup if needed
    }

    @Test
    fun testRoot() = testApplication {
        environment {
            config = MapApplicationConfig(
                "database.host" to "localhost",
                "database.port" to "5432",
                "database.name" to "test_stylishdatabase",
                "database.user" to "postgres",
                "database.password" to "1852"
            )
        }

        application {
            module()
        }

        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
} 