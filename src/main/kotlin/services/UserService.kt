package services

import com.example.domain.EventSummaryResponse
import com.example.domain.StudentProfileResponse
import config.DatabaseFactory.dbQuery
import database.EventRegistrationsTable
import database.EventsTable
import database.UsersTable
import org.jetbrains.exposed.sql.select

class UserService {

    suspend fun getStudentProfile(enrollmentNo: String): StudentProfileResponse? = dbQuery {
        val studentRow = UsersTable.select { UsersTable.enrollmentNumber eq enrollmentNo }.singleOrNull() ?: return@dbQuery null
        val sId = studentRow[UsersTable.id]

        // Count registrations
        val registeredEventIds = EventRegistrationsTable
            .select { EventRegistrationsTable.studentId eq sId }
            .map { it[EventRegistrationsTable.eventId] }

        val registeredEventsList = if (registeredEventIds.isNotEmpty()) {
            EventsTable.select { EventsTable.id inList registeredEventIds }.map {
                EventSummaryResponse(
                    id = it[EventsTable.id],
                    title = it[EventsTable.title],
                    clubName = it[EventsTable.clubName],
                    date = it[EventsTable.eventDate],
                    time = it[EventsTable.eventTime],
                    venue = it[EventsTable.venue],
                    registrationBadge = it[EventsTable.statusBadge]
                )
            }
        } else {
            emptyList()
        }

        val userRole = studentRow[UsersTable.role]

        StudentProfileResponse(
            id = sId,
            fullName = studentRow[UsersTable.fullName],
            enrollmentNumber = studentRow[UsersTable.enrollmentNumber],
            branchDepartment = studentRow[UsersTable.branchDepartment],
            universityEmail = studentRow[UsersTable.universityEmail],
            totalEvents = registeredEventsList.size,
            certificatesCount = studentRow[UsersTable.certificatesCount],
            registeredEvents = registeredEventsList,
            role = userRole,
        )
    }
}