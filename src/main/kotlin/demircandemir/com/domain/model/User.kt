package demircandemir.com.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class User(
    val id: Int,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?,
    val registrationDate: LocalDateTime,
    val isActive: Boolean
) 