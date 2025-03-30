package demircandemir.com.infrastructure.persistence.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object Users : IntIdTable("USERS") {
    val firstName = varchar("first_name", 50)
    val lastName = varchar("last_name", 50)
    val email = varchar("email", 100).uniqueIndex("idx_user_email_unique")
    val password = varchar("password", 255)
    val phoneNumber = varchar("phone_number", 20).nullable()
    val registrationDate = datetime("registration_date").default(LocalDateTime.now())
    val isActive = bool("is_active").default(true)

    init {
        index(isUnique = true, email)
    }
}