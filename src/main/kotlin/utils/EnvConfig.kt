package utils

import io.github.cdimascio.dotenv.Dotenv

object EnvConfig {
    private val dotenv: Dotenv = Dotenv.configure().ignoreIfMissing().load()

    val dbHost: String = dotenv["DB_HOST"] ?: "localhost"
    val dbPort: String = dotenv["DB_PORT"] ?: "3306"
    val dbName: String = dotenv["DB_NAME"] ?: "college_event_db"
    val dbUser: String = dotenv["DB_USER"] ?: "root"
    val dbPassword: String = dotenv["DB_PASSWORD"] ?: ""

    val jwtSecret: String = dotenv["JWT_SECRET"] ?: "default_secret_key"
    val jwtIssuer: String = dotenv["JWT_ISSUER"] ?: "http://0.0.0.0:8080/"
    val jwtAudience: String = dotenv["JWT_AUDIENCE"] ?: "college_event_students"
}