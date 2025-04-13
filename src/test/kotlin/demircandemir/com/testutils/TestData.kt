package demircandemir.com.testutils

import demircandemir.com.application.dto.CreateAddressRequest
import demircandemir.com.application.dto.UpdateAddressRequest
import demircandemir.com.domain.model.*
import java.math.BigDecimal
import java.time.LocalDateTime

object TestData {
    object Users {
        fun createTestUser(
            id: Int = 0,
            email: String = "test@test.com",
            password: String = "password123",
            firstName: String = "Test",
            lastName: String = "User",
            phoneNumber: String = "1234567890",
            registrationDate: LocalDateTime = LocalDateTime.now()
        ) = User(
            id = id,
            email = email,
            passwordHash = password,
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phoneNumber,
            registrationDate = registrationDate,
            status = AccountStatus.ACTIVE
        )
    }

    object Addresses {
        fun createTestAddress(
            id: Int = 0,
            userId: Int = 1,
            addressTitle: String = "Home",
            address: String = "123 Test St",
            city: String = "Test City",
            district: String = "Test District",
            postalCode: String = "12345",
            country: String = "Test Country"
        ) = Address(
            id = id,
            userId = userId,
            addressTitle = addressTitle,
            address = address,
            city = city,
            district = district,
            postalCode = postalCode,
            country = country
        )

        fun createTestAddressRequest(
            userId: Int = 1,
            addressTitle: String = "Home",
            address: String = "123 Test St",
            city: String = "Test City",
            district: String = "Test District",
            postalCode: String = "12345",
            country: String = "Test Country"
        ) = CreateAddressRequest(
            userId = userId,
            addressTitle = addressTitle,
            address = address,
            city = city,
            district = district,
            postalCode = postalCode,
            country = country
        )

        fun createTestUpdateAddressRequest(
            addressTitle: String? = "Updated Home",
            address: String? = null,
            city: String? = null,
            district: String? = null,
            postalCode: String? = null,
            country: String? = null
        ) = UpdateAddressRequest(
            addressTitle = addressTitle,
            address = address,
            city = city,
            district = district,
            postalCode = postalCode,
            country = country
        )

        fun createTestAddresses(userId: Int = 1, count: Int = 2): List<Address> {
            return (1..count).map { index ->
                createTestAddress(
                    id = index,
                    userId = userId,
                    addressTitle = if (index == 1) "Home" else "Work",
                    address = if (index == 1) "123 Test St" else "456 Test Ave"
                )
            }
        }
    }

    object Products {
        fun createTestProduct(
            id: Int = 0,
            productName: String = "Test Product",
            description: String? = "Test Description",
            price: BigDecimal = BigDecimal("99.99"),
            stockQuantity: Int = 100,
            categoryId: Int? = null,
            brand: String? = "Test Brand",
            dateAdded: LocalDateTime = LocalDateTime.now(),
            isActive: Boolean = true
        ) = Product(
            id = id,
            productName = productName,
            description = description,
            price = price,
            stockQuantity = stockQuantity,
            categoryId = categoryId,
            brand = brand,
            dateAdded = dateAdded,
            isActive = isActive
        )
    }

    object Categories {
        fun createTestCategory(
            id: Int = 0,
            categoryName: String = "Test Category",
            description: String? = "Test Description",
            parentCategoryId: Int? = null,
            parentCategory: Category? = null,
            subCategories: List<Category> = listOf(),
        ) = Category(
            id = id,
            categoryName = categoryName,
            description = description,
            parentCategoryId = parentCategoryId,
            parentCategory = parentCategory,
            subCategories = subCategories
        )
    }
} 