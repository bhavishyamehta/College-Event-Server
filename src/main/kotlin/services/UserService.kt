package services

import com.example.domain.EventSummaryResponse
import com.example.domain.StudentProfileResponse
import config.DatabaseFactory.dbQuery
import database.EventRegistrationsTable
import database.EventsTable
import database.StudentsTable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

class UserService {

    suspend fun getStudentProfile(enrollmentNo: String): StudentProfileResponse? = dbQuery {
        val studentRow = StudentsTable.select { StudentsTable.enrollmentNumber eq enrollmentNo }.singleOrNull() ?: return@dbQuery null
        val sId = studentRow[StudentsTable.id]

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

        StudentProfileResponse(
            id = sId,
            fullName = studentRow[StudentsTable.fullName],
            enrollmentNumber = studentRow[StudentsTable.enrollmentNumber],
            branchDepartment = studentRow[StudentsTable.branchDepartment],
            universityEmail = studentRow[StudentsTable.universityEmail],
            totalEvents = registeredEventsList.size,
            certificatesCount = studentRow[StudentsTable.certificatesCount],
            registeredEvents = registeredEventsList
        )
    }
}