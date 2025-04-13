package demircandemir.com.infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import demircandemir.com.domain.model.User
import io.ktor.server.config.*
import java.util.*
import java.util.concurrent.TimeUnit

class JwtConfig(config: ApplicationConfig) {
    // JWT configuration values from application.conf
    private val issuer = config.property("jwt.issuer").getString()
    private val audience = config.property("jwt.audience").getString()
    private val realm = config.property("jwt.realm").getString()
    private val secret = config.property("jwt.secret").getString()

    // Token durations
    private val accessTokenExpiration = TimeUnit.MINUTES.toMillis(15) // 15 minutes
    private val refreshTokenExpiration = TimeUnit.DAYS.toMillis(7) // 7 days

    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    /**
     * Generate an access token for the user
     */
    fun generateAccessToken(user: User): String {
        return JWT.create()
            .withSubject("Authentication")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", user.id)
            .withClaim("email", user.email)
            .withClaim("role", user.role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + accessTokenExpiration))
            .sign(algorithm)
    }

    /**
     * Generate a refresh token for the user
     */
    fun generateRefreshToken(user: User): String {
        return JWT.create()
            .withSubject("Authentication")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", user.id)
            .withClaim("tokenType", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + refreshTokenExpiration))
            .sign(algorithm)
    }

    /**
     * Decode and validate a token.
     * Returns a map of claims if the token is valid, otherwise returns an empty map.
     */
    fun validateToken(token: String): Map<String, Any> {
        return try {
            val decoded: DecodedJWT = verifier.verify(token)

            val claims = mutableMapOf<String, Any>()
            // Add standard claims
            claims["iss"] = decoded.issuer
            claims["sub"] = decoded.subject
            claims["aud"] = decoded.audience // Audience is List<String>
            decoded.expiresAt?.let { claims["exp"] = it } // Add expiration if present

            // Add custom claims
            decoded.getClaim("userId").asInt()?.let { claims["userId"] = it }
            decoded.getClaim("email").asString()?.let { claims["email"] = it }
            decoded.getClaim("role").asString()?.let { claims["role"] = it }
            decoded.getClaim("tokenType").asString()?.let { claims["tokenType"] = it } // For refresh tokens

            claims // Return populated claims map
        } catch (exception: JWTVerificationException) {
            // Token is invalid (bad signature, expired, etc.)
            // Log the exception if needed
            // logger.warn("JWT Validation failed: ${exception.message}") 
            emptyMap() // Return empty map as per test expectation
        }
    }

    fun getRealm(): String {
        return realm
    }
} 