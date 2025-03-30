package demircandemir.com.di

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.check.checkModules

class RepositoryModuleTest : KoinTest {

    @Before
    fun setUp() {
        stopKoin() // Stop any existing Koin app
    }

    @After
    fun tearDown() {
        stopKoin() // Clean up after test
    }

    @Test
    fun `verify Koin module`() {
        startKoin {
            modules(repositoryModule)
        }.checkModules()
    }
} 