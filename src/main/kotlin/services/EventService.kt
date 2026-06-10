package services

import domain.CreateEventRequest
import domain.EventDetailResponse
import domain.EventSummaryResponse
import config.DatabaseFactory.dbQuery
import database.EventRegistrationsTable
import database.EventsTable
import database.UsersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

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
                bannerUrl = it[EventsTable.bannerUrl],
                date = it[EventsTable.eventDate],
                time = it[EventsTable.eventTime],
                venue = it[EventsTable.venue],
                registrationBadge = it[EventsTable.statusBadge]
            )
        }
    }

    suspend fun getEventDetail(eventId: String, enrollmentNo: String): EventDetailResponse? = dbQuery {
        val eventRow = EventsTable.select { EventsTable.id eq eventId }.singleOrNull() ?: return@dbQuery null
        val studentRow = UsersTable.select { UsersTable.enrollmentNumber eq enrollmentNo }.singleOrNull() ?: return@dbQuery null
        
        val studentId = studentRow[UsersTable.id]
        
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

    suspend fun registerForEvent(eventId: String, enrollmentNo: String): Boolean = dbQuery {
        val studentRow = UsersTable.select { UsersTable.enrollmentNumber eq enrollmentNo }.singleOrNull() ?: return@dbQuery false
        val sId = studentRow[UsersTable.id]
        
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
            it[id] = UUID.randomUUID().toString()
            it[studentId] = sId
            it[this.eventId] = eventId
        }
        true
    }

    suspend fun deregisterFromEvent(eventId: String, enrollmentNo: String): Boolean = dbQuery {
        val studentRow = UsersTable.select { UsersTable.enrollmentNumber eq enrollmentNo }.singleOrNull() ?: return@dbQuery false
        val sId = studentRow[UsersTable.id]

        val deletedCount = EventRegistrationsTable.deleteWhere {
            (studentId eq sId) and (EventRegistrationsTable.eventId eq eventId)
        }
        deletedCount > 0
    }

    // ── 1. CREATE EVENT (Accessible only by TEACHER/ADMIN) ───────────────────
    suspend fun createEvent(req: CreateEventRequest): String? = dbQuery {
        val newId = "event-" + UUID.randomUUID().toString().take(8)

        EventsTable.insert {
            it[id] = newId
            it[title] = req.title
            it[clubName] = req.clubName
            it[bannerUrl] = req.bannerUrl
            it[eventDate] = req.eventDate
            it[eventTime] = req.eventTime
            it[venue] = req.venue
            it[description] = req.description
            it[totalSeats] = req.totalSeats
            it[registrationFee] = req.registrationFee
            it[category] = req.category
            it[statusBadge] = req.statusBadge
        }
        newId
    }

    // ── 2. UPDATE EVENT (Accessible only by TEACHER/ADMIN) ───────────────────
    suspend fun updateEvent(eventId: String, req: CreateEventRequest): Boolean = dbQuery {
        val updatedRows = EventsTable.update({ EventsTable.id eq eventId }) {
            it[title] = req.title
            it[clubName] = req.clubName
            it[bannerUrl] = req.bannerUrl
            it[eventDate] = req.eventDate
            it[eventTime] = req.eventTime
            it[venue] = req.venue
            it[description] = req.description
            it[totalSeats] = req.totalSeats
            it[registrationFee] = req.registrationFee
            it[category] = req.category
            it[statusBadge] = req.statusBadge
        }
        updatedRows > 0
    }

    // ── 3. DELETE EVENT (Accessible only by TEACHER/ADMIN) ───────────────────
    suspend fun deleteEvent(eventId: String): Boolean = dbQuery {
        // First delete cascade handling for references inside registrations to avoid relational foreign key errors
        EventRegistrationsTable.deleteWhere { EventRegistrationsTable.eventId eq eventId }

        val deletedRows = EventsTable.deleteWhere { EventsTable.id eq eventId }
        deletedRows > 0
    }
}