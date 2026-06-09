package config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import database.EventRegistrationsTable
import database.EventsTable
import database.UsersTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    private var dataSource: HikariDataSource? = null

    fun init(databaseUrl: String, username: String, password: String, databaseType: String = "postgresql") {
        try {
            println("🔧 Creating HikariCP configuration...")
            val config = HikariConfig().apply {
                jdbcUrl = databaseUrl
                this.username = username
                this.password = password

                driverClassName = when (databaseType.lowercase()) {
                    "mysql" -> "com.mysql.cj.jdbc.Driver"
                    else -> "org.postgresql.Driver"
                }

                maximumPoolSize = 10
                minimumIdle = 2
                connectionTimeout = 30000
                idleTimeout = 600000
                maxLifetime = 1800000

                if (databaseType.lowercase() == "mysql") {
                    addDataSourceProperty("cachePrepStmts", "true")
                    addDataSourceProperty("prepStmtCacheSize", "250")
                    addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                    addDataSourceProperty("useServerPrepStmts", "true")
                    addDataSourceProperty("useLocalSessionState", "true")
                    addDataSourceProperty("rewriteBatchedStatements", "true")
                    addDataSourceProperty("cacheResultSetMetadata", "true")
                    addDataSourceProperty("cacheServerConfiguration", "true")
                    addDataSourceProperty("elideSetAutoCommits", "true")
                    addDataSourceProperty("maintainTimeStats", "false")
                    addDataSourceProperty("allowPublicKeyRetrieval", "true")
                    addDataSourceProperty("useSSL", "false")
                    addDataSourceProperty("serverTimezone", "UTC")
                }
            }

            println("📦 Creating HikariCP data source...")
            dataSource = HikariDataSource(config)

            println("🔌 Connecting to database...")
            Database.connect(dataSource!!)
            println("✅ Database connection established")

            println("🏗️ Creating/updating database schema...")
            transaction {
                try {
                    // ✅ Create tables in correct order (dependencies first)
                    SchemaUtils.createMissingTablesAndColumns(
                        UsersTable,
                        EventsTable,
                        EventRegistrationsTable
                    )
                    println("✅ Database schema updated successfully")
                } catch (e: Exception) {
                    println("❌ ERROR creating schema: ${e.message}")
                    e.printStackTrace()
                    throw e
                }
            }

            println("🔍 Verifying database connection...")
            transaction {
                exec("SELECT 1") // Test query
            }
            println("✅ Database verification successful")

        } catch (e: Exception) {
            println("❌ ERROR in DatabaseConfig.init: ${e.message}")
            println("Database URL: ${databaseUrl.take(100)}...")
            println("Database Type: $databaseType")
            e.printStackTrace()
            throw RuntimeException("Failed to initialize database: ${e.message}", e)
        }
    }

    fun close() {
        println("🔌 Closing database connection...")
        dataSource?.close()
        println("✅ Database connection closed")
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}