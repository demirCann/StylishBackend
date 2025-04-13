package demircandemir.com.application.service

import io.ktor.server.config.*
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Service to handle email operations
 * Implements email functionality using Jakarta Mail
 */
class EmailService(private val config: ApplicationConfig) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val isDevelopment = config.propertyOrNull("application.environment")?.getString() == "dev"
    private val smtpHost = config.propertyOrNull("email.smtp.host")?.getString() ?: "smtp.gmail.com"
    private val smtpPort = config.propertyOrNull("email.smtp.port")?.getString() ?: "587"
    private val smtpUser = config.propertyOrNull("email.smtp.user")?.getString() ?: ""
    private val smtpPassword = config.propertyOrNull("email.smtp.password")?.getString() ?: ""
    private val senderEmail = config.propertyOrNull("email.sender")?.getString() ?: "stylish@example.com"
    private val baseUrl = config.propertyOrNull("application.baseUrl")?.getString() ?: "http://localhost:8080"

    // Email templates could be externalized to configuration or database
    private val verificationEmailTemplate = """
        Hello,
        
        Thank you for registering with Stylish App. Please verify your email by clicking the link below:
        
        $baseUrl/api/auth/verify-email?token=%s&email=%s
        
        This link will expire in 24 hours.
        
        Best regards,
        The Stylish Team
    """.trimIndent()

    private val passwordResetEmailTemplate = """
        Hello,
        
        You requested a password reset. Please use the link below to reset your password:
        
        $baseUrl/reset-password?token=%s&email=%s
        
        This link will expire in 1 hour.
        
        If you did not request this reset, please ignore this email.
        
        Best regards,
        The Stylish Team
    """.trimIndent()

    private val orderConfirmationTemplate = """
        Hello %s,
        
        Thank you for your order! 
        
        Order Number: %s
        Total Amount: %s
        
        You can track your order status at $baseUrl/orders/%s
        
        Best regards,
        The Stylish Team
    """.trimIndent()

    /**
     * Configures email session properties
     */
    private fun createSession(): Session {
        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.host"] = smtpHost
        props["mail.smtp.port"] = smtpPort

        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(smtpUser, smtpPassword)
            }
        })
    }

    /**
     * Send email - either logs in development or actually sends in production
     */
    private fun sendEmail(recipientEmail: String, subject: String, content: String) {
        if (isDevelopment) {
            // In development, just log the email
            logger.info("==== EMAIL SENT (DEV MODE) ====")
            logger.info("To: $recipientEmail")
            logger.info("Subject: $subject")
            logger.info("Content:\n$content")
            logger.info("==== END EMAIL ====")
            return
        }

        try {
            val session = createSession()
            val message = MimeMessage(session)

            message.setFrom(InternetAddress(senderEmail))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
            message.subject = subject
            message.setText(content)

            Transport.send(message)
            logger.info("Email successfully sent to $recipientEmail")
        } catch (e: Exception) {
            logger.error("Failed to send email to $recipientEmail", e)
            // In a production application, we might want to retry or queue the email
        }
    }

    /**
     * Send verification email
     */
    fun sendVerificationEmail(email: String, token: String) {
        val emailContent = verificationEmailTemplate.format(token, email)
        sendEmail(email, "Verify Your Email", emailContent)
    }

    /**
     * Send password reset email
     */
    fun sendPasswordResetEmail(email: String, token: String) {
        val emailContent = passwordResetEmailTemplate.format(token, email)
        sendEmail(email, "Reset Your Password", emailContent)
    }

    /**
     * Send order confirmation email
     */
    fun sendOrderConfirmationEmail(email: String, name: String, orderNumber: String, totalAmount: String) {
        val emailContent = orderConfirmationTemplate.format(name, orderNumber, totalAmount, orderNumber)
        sendEmail(email, "Your Order Confirmation", emailContent)
    }

    /**
     * Send shipping notification email
     */
    fun sendShippingNotificationEmail(
        email: String,
        name: String,
        orderNumber: String,
        trackingNumber: String,
        carrier: String
    ) {
        val content = """
            Hello $name,
            
            Great news! Your order #$orderNumber has been shipped.
            
            Tracking Number: $trackingNumber
            Carrier: $carrier
            
            You can track your package using the carrier's website.
            
            Best regards,
            The Stylish Team
        """.trimIndent()

        sendEmail(email, "Your Order Has Been Shipped", content)
    }

    /**
     * Send custom email notification
     */
    fun sendCustomEmail(email: String, subject: String, content: String) {
        sendEmail(email, subject, content)
    }
} 