package services

import com.example.domain.AuthResponse
import com.example.domain.LoginRequest
import com.example.domain.RegisterRequest
import config.DatabaseFactory.dbQuery
import database.StudentsTable
import domain.AuthJWT
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.mindrot.jbcrypt.BCrypt

class AuthService(private val userService: UserService) {

    suspend fun registerStudent(req: RegisterRequest): AuthResponse? = dbQuery {
        val existing = StudentsTable.select { StudentsTable.enrollmentNumber eq req.enrollmentNumber }.singleOrNull()
        if (existing != null) return@dbQuery null

        val hashedPassword = BCrypt.hashpw(req.password, BCrypt.gensalt())
        
        val insertStatement = StudentsTable.insert {
            it[fullName] = req.fullName
            it[enrollmentNumber] = req.enrollmentNumber
            it[branchDepartment] = req.branchDepartment
            it[universityEmail] = req.universityEmail
            it[passwordHash] = hashedPassword
            it[certificatesCount] = 0
        }

        val studentId = insertStatement[StudentsTable.id]
        val token = AuthJWT.generateToken(req.enrollmentNumber)
        val profile = userService.getStudentProfile(req.enrollmentNumber) ?: return@dbQuery null
        
        AuthResponse(token, profile)
    }

    suspend fun loginStudent(req: LoginRequest): AuthResponse? = dbQuery {
        val studentRow = StudentsTable.select { StudentsTable.enrollmentNumber eq req.enrollmentNumber }.singleOrNull() ?: return@dbQuery null
        
        val passwordMatches = BCrypt.checkpw(req.password, studentRow[StudentsTable.passwordHash])
        if (!passwordMatches) return@dbQuery null

        val token = AuthJWT.generateToken(req.enrollmentNumber)
        val profile = userService.getStudentProfile(req.enrollmentNumber) ?: return@dbQuery null

        AuthResponse(token, profile)
    }
}