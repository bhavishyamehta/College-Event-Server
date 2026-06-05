package config

import database.EventRegistrationsTable
import database.EventsTable
import database.StudentsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import utils.EnvConfig

object DatabaseFactory {
    fun init() {
        val driverClassName = "com.mysql.cj.jdbc.Driver"
        val jdbcUrl = "jdbc:mysql://${EnvConfig.dbHost}:${EnvConfig.dbPort}/${EnvConfig.dbName}?useSSL=false&allowPublicKeyRetrieval=true"

        val database = Database.connect(
            url = jdbcUrl,
            driver = driverClassName,
            user = EnvConfig.dbUser,
            password = EnvConfig.dbPassword
        )

        transaction(database) {
            SchemaUtils.create(StudentsTable, EventsTable, EventRegistrationsTable)
            // Seeds setup if required for testing
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}