package database

import org.jetbrains.exposed.sql.Table

object StudentsTable : Table("students") {
    val id = integer("id").autoIncrement()
    val fullName = varchar("full_name", 255)
    val enrollmentNumber = varchar("enrollment_number", 50).uniqueIndex()
    val branchDepartment = varchar("branch_department", 100)
    val universityEmail = varchar("university_email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val certificatesCount = integer("certificates_count").default(0)

    override val primaryKey = PrimaryKey(id)
}

object EventsTable : Table("events") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val clubName = varchar("club_name", 150)
    val bannerUrl = varchar("banner_url", 500)
    val eventDate = varchar("event_date", 100)
    val eventTime = varchar("event_time", 50)
    val venue = varchar("venue", 255)
    val description = text("description")
    val totalSeats = integer("total_seats").default(50)
    val registrationFee = varchar("registration_fee", 50).default("Free")
    val category = varchar("category", 50) // Technical, Cultural, Sports, etc.
    val statusBadge = varchar("status_badge", 50).default("Registration Open")

    override val primaryKey = PrimaryKey(id)
}

object EventRegistrationsTable : Table("event_registrations") {
    val id = integer("id").autoIncrement()
    val studentId = integer("student_id").references(StudentsTable.id)
    val eventId = integer("event_id").references(EventsTable.id)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("student_event_unique", studentId, eventId)
    }
}