package services

import domain.AuthResponse
import domain.LoginRequest
import domain.RegisterRequest
import config.DatabaseFactory.dbQuery
import database.UsersTable
import domain.AuthJWT
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.mindrot.jbcrypt.BCrypt

class AuthService(private val userService: UserService) {

    suspend fun registerUser(req: RegisterRequest): AuthResponse? = dbQuery {
        // Validation constraint logic check checking duplicate users
        val existing = UsersTable.select { UsersTable.enrollmentNumber eq req.enrollmentNumber }.singleOrNull()
        if (existing != null) return@dbQuery null

        val hashedPassword = BCrypt.hashpw(req.password, BCrypt.gensalt(10))

        // Dynamic Role allocation processing mapping input safely Uppercased
        val formattedRole = req.role.uppercase().trim().ifBlank { "STUDENT" }

        val insertStatement = UsersTable.insert {
            it[id] = "user-" + java.util.UUID.randomUUID().toString().take(8)
            it[profileImage] = ""
            it[fullName] = req.fullName
            it[enrollmentNumber] = req.enrollmentNumber
            it[branchDepartment] = req.branchDepartment
            it[universityEmail] = req.universityEmail
            it[passwordHash] = hashedPassword
            it[certificatesCount] = 0
            it[role] = formattedRole // Stores dynamic user level roles inside database engine table
        }

        // Generate token injecting enrollment identity alongside user roles authorization claim attributes
        val token = AuthJWT.generateToken(req.enrollmentNumber, formattedRole)
        val profile = userService.getStudentProfile(req.enrollmentNumber) ?: return@dbQuery null

        AuthResponse(token, profile)
    }

    suspend fun loginUser(req: LoginRequest): AuthResponse? = dbQuery {
        val userRow = UsersTable.select { UsersTable.enrollmentNumber eq req.enrollmentNumber }.singleOrNull()
            ?: return@dbQuery null

        val passwordMatches = BCrypt.checkpw(req.password, userRow[UsersTable.passwordHash])
        if (!passwordMatches) return@dbQuery null

        val userRole = userRow[UsersTable.role] // 👈 Fetch role directly from row index reference pointer

        // Token generation scope linked tracking tracking attributes
        val token = AuthJWT.generateToken(req.enrollmentNumber, userRole)
        val profile = userService.getStudentProfile(req.enrollmentNumber) ?: return@dbQuery null

        AuthResponse(token, profile)
    }
}