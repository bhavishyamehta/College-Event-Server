package services

import com.example.domain.EventDetailResponse
import com.example.domain.EventSummaryResponse
import config.DatabaseFactory.dbQuery
import database.EventRegistrationsTable
import database.EventsTable
import database.StudentsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class EventService {

    suspend fun getAllEvents(category: String?, enrollmentNo: String): List<EventSummaryResponse> = dbQuery {
        val query = if (!category.isNullOrBlank() && category != "All") {
            EventsTable.select { EventsTable.category eq category }
        } else {
            EventsTable.selectAll()
        }

        query.map {
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
    }

    suspend fun getEventDetail(eventId: Int, enrollmentNo: String): EventDetailResponse? = dbQuery {
        val eventRow = EventsTable.select { EventsTable.id eq eventId }.singleOrNull() ?: return@dbQuery null
        val studentRow = StudentsTable.select { StudentsTable.enrollmentNumber eq enrollmentNo }.singleOrNull() ?: return@dbQuery null
        
        val studentId = studentRow[StudentsTable.id]
        
        // Count actual registrations for this event
        val currentRegsCount = EventRegistrationsTable.select { EventRegistrationsTable.eventId eq eventId }.count().toInt()
        val totalSeats = eventRow[EventsTable.totalSeats]
        val spotsLeft = (totalSeats - currentRegsCount).coerceAtLeast(0)

        val isRegistered = EventRegistrationsTable.select {
            (EventRegistrationsTable.studentId eq studentId) and (EventRegistrationsTable.eventId eq eventId)
        }.any()

        EventDetailResponse(
            id = eventRow[EventsTable.id],
            title = eventRow[EventsTable.title],
            clubName = eventRow[EventsTable.clubName],
            bannerUrl = eventRow[EventsTable.bannerUrl],
            date = eventRow[EventsTable.eventDate],
            time = eventRow[EventsTable.eventTime],
            venue = eventRow[EventsTable.venue],
            description = eventRow[EventsTable.description],
            seatAvailability = "Spots Left: $spotsLeft",
            registrationFee = eventRow[EventsTable.registrationFee],
            isUserRegistered = isRegistered
        )
    }

    suspend fun registerForEvent(eventId: Int, enrollmentNo: String): Boolean = dbQuery {
        val studentRow = StudentsTable.select { StudentsTable.enrollmentNumber eq enrollmentNo }.singleOrNull() ?: return@dbQuery false
        val sId = studentRow[StudentsTable.id]
        
        // Already registered validation
        val alreadyExists = EventRegistrationsTable.select {
            (EventRegistrationsTable.studentId eq sId) and (EventRegistrationsTable.eventId eq eventId)
        }.any()
        if (alreadyExists) return@dbQuery true

        // Capacity check
        val eventRow = EventsTable.select { EventsTable.id eq eventId }.singleOrNull() ?: return@dbQuery false
        val currentRegsCount = EventRegistrationsTable.select { EventRegistrationsTable.eventId eq eventId }.count().toInt()
        if (currentRegsCount >= eventRow[EventsTable.totalSeats]) return@dbQuery false // Housefull

        EventRegistrationsTable.insert {
            it[studentId] = sId
            it[this.eventId] = eventId
        }
        true
    }

    suspend fun deregisterFromEvent(eventId: Int, enrollmentNo: String): Boolean = dbQuery {
        val studentRow = StudentsTable.select { StudentsTable.enrollmentNumber eq enrollmentNo }.singleOrNull() ?: return@dbQuery false
        val sId = studentRow[StudentsTable.id]

        val deletedCount = EventRegistrationsTable.deleteWhere {
            (studentId eq sId) and (EventRegistrationsTable.eventId eq eventId)
        }
        deletedCount > 0
    }
}