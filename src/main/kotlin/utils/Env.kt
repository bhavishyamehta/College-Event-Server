package utils

import mu.KotlinLogging
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Properties

private val logger = KotlinLogging.logger {}

data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val type: String
)

/**
 * Single source of truth for all environment variables.
 *
 * Reading priority (highest first):
 *   1. Real OS environment  (System.getenv)
 *   2. .env file values     (loaded into System properties by Application.kt → loadDotEnv)
 *
 * This means real env vars always win over .env — useful for Docker / CI.
 *
 * Usage anywhere in the app:
 *   val secret = Env.JWT_SECRET
 *   val dbUrl  = Env.DB_URL
 */
object Env {
    private val properties = loadEnvFile()

    private fun loadEnvFile(): Properties {
        val props = Properties()
        val envFile = File(".env")

        if (envFile.exists()) {
            try {
                envFile.inputStream().use { input ->
                    props.load(input)
                }
            } catch (e: Exception) {
                println("Warning: Could not load .env file: ${e.message}")
            }
        }

        return props
    }

    private fun getEnv(key: String, defaultValue: String): String {
        return System.getenv(key) ?: properties.getProperty(key) ?: defaultValue
    }

    private fun getEnvInt(key: String, defaultValue: Int): Int {
        val value = System.getenv(key) ?: properties.getProperty(key)
        return value?.toIntOrNull() ?: defaultValue
    }

    private fun parseHerokuDatabaseUrl(url: String): DatabaseConfig? {
        return try {
            println("Parsing Heroku DB_URL format...")
            // Heroku format: postgres://user:password@host:port/database
            // or: postgresql://user:password@host:port/database
            // Note: Passwords with special characters are URL-encoded by Heroku
            val uri = URI(url)
            val scheme = uri.scheme
            println("Database scheme: $scheme")

            val userInfo = uri.userInfo?.split(":", limit = 2) ?: run {
                println("WARNING: No user info in DB_URL")
                return null
            }
            val username = URLDecoder.decode(userInfo[0], StandardCharsets.UTF_8.name())
            val password = URLDecoder.decode(userInfo.getOrNull(1) ?: "", StandardCharsets.UTF_8.name())
            val host = uri.host
            val port = if (uri.port != -1) uri.port else when (scheme) {
                "postgres", "postgresql" -> 5432
                "mysql" -> 3306
                else -> 5432
            }
            val database = uri.path.removePrefix("/")

            println("Parsed database config: host=$host, port=$port, database=$database, user=$username")

            val dbType = when (scheme) {
                "postgres", "postgresql" -> "postgresql"
                "mysql" -> "mysql"
                else -> "postgresql"
            }

            val jdbcUrl = when (dbType) {
                "mysql" -> "jdbc:mysql://$host:$port/$database?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                else -> "jdbc:postgresql://$host:$port/$database"
            }

            println("Successfully parsed Heroku DATABASE_URL")
            DatabaseConfig(jdbcUrl, username, password, dbType)
        } catch (e: Exception) {
            println("ERROR parsing Heroku DATABASE_URL: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private val databaseConfig: DatabaseConfig by lazy {
        val herokuUrl = System.getenv("DB_URL") ?: properties.getProperty("DB_URL")
        println("DB_URL from environment: ${herokuUrl?.take(50)}...")

        if (herokuUrl != null && !herokuUrl.startsWith("jdbc:")) {
            // Try to parse as Heroku format
            val parsed = parseHerokuDatabaseUrl(herokuUrl)
            if (parsed != null) {
                return@lazy parsed
            }

            println("WARNING: Failed to parse Heroku DB_URL, falling back to individual env vars")
            // Fall back to individual env vars
            val dbType = getEnv("DATABASE_TYPE", "postgresql")
            DatabaseConfig(
                jdbcUrl = getEnv(
                    "DATABASE_URL", when (dbType.lowercase()) {
                        "mysql" -> "jdbc:mysql://localhost:3306/college_event_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                        else -> "jdbc:postgresql://localhost:5432/college_event_db"
                    }
                ),
                username = getEnv(
                    "DB_USER", when (dbType.lowercase()) {
                        "mysql" -> "root"
                        else -> "postgres"
                    }
                ),
                password = getEnv(
                    "DB_PASSWORD", when (dbType.lowercase()) {
                        "mysql" -> "root1234"
                        else -> "root1234"
                    }
                ),
                type = dbType
            )
        } else {
            // Already in JDBC format or using individual env vars
            val dbType = getEnv("DATABASE_TYPE", "postgresql")
            DatabaseConfig(
                jdbcUrl = getEnv(
                    "DATABASE_URL", when (dbType.lowercase()) {
                        "mysql" -> "jdbc:mysql://localhost:3306/college_event_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                        else -> "jdbc:postgresql://localhost:5432/college_event_db"
                    }
                ),
                username = getEnv(
                    "DB_USER", when (dbType.lowercase()) {
                        "mysql" -> "root"
                        else -> "postgres"
                    }
                ),
                password = getEnv(
                    "DB_PASSWORD", when (dbType.lowercase()) {
                        "mysql" -> "root1234"
                        else -> "root1234"
                    }
                ),
                type = dbType
            )
        }
    }

    val DATABASE_TYPE = databaseConfig.type
    val DB_URL = databaseConfig.jdbcUrl
    val DB_USER = databaseConfig.username
    val DB_PASSWORD = databaseConfig.password

    val JWT_SECRET = getEnv("JWT_SECRET", "default-secret-change-in-production")
    // Access token expires in 15 minutes (short-lived for security)
    val ACCESS_TOKEN_TTL_MIN = getEnvInt("ACCESS_TOKEN_TTL_MIN", 15)
    // Refresh token expires in 30 days (long-lived for user convenience)
    val REFRESH_TOKEN_TTL_DAYS = getEnvInt("REFRESH_TOKEN_TTL_DAYS", 30)

    val JWT_ISSUER = getEnv("JWT_ISSUER", "college-event-tracker")
    val JWT_AUDIENCE = getEnv("JWT_AUDIENCE", "college_event_students")

    // ── Server ────────────────────────────────────────────────────────────────
    val PORT: Int
        get() = getInt("PORT", default = 8080)

    // ── Database ──────────────────────────────────────────────────────────────
//    val DB_URL: String
//        get() = require("DB_URL")


    val DB_POOL_SIZE: Int
        get() = getInt("DB_POOL_SIZE", default = 10)

    // ── JWT ───────────────────────────────────────────────────────────────────
//    val JWT_SECRET: String
//        get() = require("JWT_SECRET")

    /*val JWT_ISSUER: String
        get() = get("JWT_ISSUER", default = "college_event_students")

    val JWT_AUDIENCE: String
        get() = get("JWT_AUDIENCE", default = "college_event_students-users")*/

    val JWT_REALM: String
        get() = get("JWT_REALM", default = "college_event_students")

    val JWT_EXPIRATION_MS: Long
        get() = getLong("JWT_EXPIRATION_MS", default = 86_400_000L)

    // ── Firebase ──────────────────────────────────────────────────────────────
    val FIREBASE_SERVICE_ACCOUNT_PATH: String
        get() = get("FIREBASE_SERVICE_ACCOUNT_PATH", default = "firebase-service-account.json")


    // ── Seeding ───────────────────────────────────────────────────────────────
    val SEED_DATABASE: Boolean
        get() = read("SEED_DATABASE")?.trim()?.lowercase() == "true"

    // ══════════════════════════════════════════════════════════════════════════
    // Startup validation
    // ══════════════════════════════════════════════════════════════════════════

    fun validate() {
        val required = listOf("DB_URL", "DB_USER", "DB_PASSWORD", "JWT_SECRET")
        val missing  = required.filter { read(it).isNullOrBlank() }

        if (missing.isNotEmpty()) {
            val msg = buildString {
                appendLine()
                appendLine("  ╔══════════════════════════════════════════════════════════╗")
                appendLine("  ║  Missing required environment variables:                 ║")
                missing.forEach { appendLine("  ║    ✗  $it${" ".repeat(52 - it.length)}║") }
                appendLine("  ║                                                          ║")
                appendLine("  ║  Copy .env.example → .env and fill in the values.       ║")
                appendLine("  ╚══════════════════════════════════════════════════════════╝")
            }
            throw IllegalStateException(msg)
        }

        if (JWT_SECRET.length < 32) {
            logger.warn { "JWT_SECRET is shorter than 32 characters — use a longer secret in production!" }
        }

        logger.info {
            "✓ Env validated  [DB=${DB_URL.substringBefore("?")}  PORT=$PORT  SEED=$SEED_DATABASE]"
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Reads a value with priority:
     *   real OS env  >  System property (set by loadDotEnv in Application.kt)
     */
    private fun read(key: String): String? {
        return System.getenv(key)                            // real OS env
            ?: System.getProperty(key)                       // set by loadDotEnv()
    }


    private fun require(key: String): String {
        return read(key)?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "Required env variable '$key' is not set. Check your .env file."
            )
    }

    private fun get(key: String, default: String): String =
        read(key)?.takeIf { it.isNotBlank() } ?: default

    private fun getInt(key: String, default: Int): Int =
        read(key)?.trim()?.toIntOrNull() ?: default

    private fun getLong(key: String, default: Long): Long =
        read(key)?.trim()?.toLongOrNull() ?: default

    private fun getDouble(key: String, default: Double): Double =
        read(key)?.trim()?.toDoubleOrNull() ?: default
}