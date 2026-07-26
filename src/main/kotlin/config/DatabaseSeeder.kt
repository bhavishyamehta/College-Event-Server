package config

import database.EventRegistrationsTable
import database.EventsTable
import database.UsersTable
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import mu.KotlinLogging
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import utils.Env
import java.util.UUID

private val logger = KotlinLogging.logger {}

// ── Fixed UUID IDs for cross-references ──────────────────────────────────────
private const val STUDENT_ALEX_ID = "seed-stud-alex-0000-000000000001"
private const val TEACHER_VIKRAM_ID = "seed-tech-vikr-0000-000000000002"
private const val ADMIN_NEHA_ID = "seed-admn-neha-0000-000000000003"

private const val EVENT_TECH_ID = "seed-even-tech-0000-000000000010"
private const val EVENT_BANDS_ID = "seed-even-band-0000-000000000011"
private const val EVENT_UIUX_ID = "seed-even-uiux-0000-000000000012"
private const val EVENT_BASKET_ID = "seed-even-bask-0000-000000000013"

private data class SeedUser(
    val id: String,
    val fullName: String,
    val enrollmentNumber: String, // Acts as UserID/EmployeeID for staff
    val branchDepartment: String,
    val universityEmail: String,
    val passwordPlain: String,
    val role: String,
    val certificatesCount: Int = 0
)

private data class SeedEvent(
    val id: String,
    val title: String,
    val clubName: String,
    val bannerUrl: String,
    val eventDate: String,
    val eventTime: String,
    val venue: String,
    val description: String,
    val totalSeats: Int,
    val registrationFee: String,
    val category: String,
    val statusBadge: String,
    val creatorId: String,
    val createdBy: String
)

fun Application.databaseSeeder() {
    environment.monitor.subscribe(ApplicationStarted) {
        //  Respect the SEED_DATABASE flag — production mein ye false hona chahiye
        if (!Env.SEED_DATABASE) {
            logger.info { "Database seeding skipped (SEED_DATABASE=false)" }
            return@subscribe
        }

        try {
            transaction {
                // Idempotency guard — check if the core student already exists
                val alreadySeeded = UsersTable
                    .select { UsersTable.enrollmentNumber eq "ENR2024-884920" }
                    .firstOrNull()

                if (alreadySeeded != null) {
                    logger.info { "Database already seeded with college data — skipping" }
                    return@transaction
                }

                logger.info { "Seeding Role-Based College Users and UI-Matched Events..." }

                seedUsers()
                seedEvents()
                seedRegistrations()

                logger.info {
                    """
                    
                    ╔══════════════════════════════════════════════════════════════╗
                    ║            COLLEGE EVENT ROLE-BASED SEED COMPLETE            ║
                    ╠══════════════════════════════════════════════════════════════╣
                    ║  1. STUDENT ROLE:                                            ║
                    ║     ID/Enr   → ENR2024-884920 | Pass: password123            ║
                    ║     Name     → Alex Thompson (Pre-registered in 2 events)    ║
                    ║                                                              ║
                    ║  2. TEACHER ROLE:                                            ║
                    ║     ID/Emp   → TCH2026-112233 | Pass: teacher123             ║
                    ║     Name     → Prof. Vikram Malhotra                         ║
                    ║                                                              ║
                    ║  3. ADMIN ROLE:                                              ║
                    ║     ID/Emp   → ADM2026-998877 | Pass: admin123               ║
                    ║     Name     → Neha Sharma (Full System Control)             ║
                    ╚══════════════════════════════════════════════════════════════╝
                    """.trimIndent()
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "ERROR in college database seeding: ${e.message}" }
        }
    }
}

// ── Private Seed Helpers ─────────────────────────────────────────────────────

private fun seedUsers() {
    val users = listOf(
        SeedUser(
            id = STUDENT_ALEX_ID,
            fullName = "Alex Thompson",
            enrollmentNumber = "ENR2024-884920",
            branchDepartment = "B.Tech - Computer Science",
            universityEmail = "alex.thompson@university.edu",
            passwordPlain = "password123",
            role = "STUDENT",
            certificatesCount = 4
        ),
        SeedUser(
            id = TEACHER_VIKRAM_ID,
            fullName = "Prof. Vikram Malhotra",
            enrollmentNumber = "TCH2026-112233", // Staff ID used as enrollment key
            branchDepartment = "Department of AI & Robotics",
            universityEmail = "vikram.malhotra@university.edu",
            passwordPlain = "teacher123",
            role = "TEACHER"
        ),
        SeedUser(
            id = ADMIN_NEHA_ID,
            fullName = "Neha Sharma",
            enrollmentNumber = "ADM2026-998877", // Admin ID used as enrollment key
            branchDepartment = "University Administration Center",
            universityEmail = "neha.admin@university.edu",
            passwordPlain = "admin123",
            role = "ADMIN"
        )
    )

    users.forEach { user ->
        UsersTable.insert {
            it[id] = user.id
            it[fullName] = user.fullName
            it[enrollmentNumber] = user.enrollmentNumber
            it[branchDepartment] = user.branchDepartment
            it[universityEmail] = user.universityEmail
            it[passwordHash] = BCrypt.hashpw(user.passwordPlain, BCrypt.gensalt(10))
            it[certificatesCount] = user.certificatesCount
            it[role] = user.role // 👈 Enforces matching roles assignment natively inside the DB
        }
        logger.debug { "  Seeded User: ${user.fullName} [Role: ${user.role}]" }
    }
}

private fun seedEvents() {
    val events = listOf(
        SeedEvent(
            id = EVENT_TECH_ID,
            title = "Future Tech Summit 2024: The AI Revolution",
            clubName = "TECH INNOVATORS SOCIETY",
            bannerUrl = "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800",
            eventDate = "October 24, 2024",
            eventTime = "09:00 AM - 04:00 PM",
            venue = "Main Auditorium, BLOCK C",
            description = "Join us for the most anticipated technology event of the semester. The Future Tech Summit brings together industry leaders, academic researchers, and student innovators to explore how Artificial Intelligence is reshaping our professional landscape. Participants will engage in high octane workshops, witness live demonstrations of neural network projects, and network with recruiters from top tier tech firms. Whether you're a beginner or an advanced coder, there's something for everyone at the AI Revolution.",
            totalSeats = 100,
            registrationFee = "$15.00",
            category = "Technical",
            statusBadge = "TRENDING",
            creatorId = TEACHER_VIKRAM_ID,          // ✅ new
            createdBy = "Prof. Vikram Malhotra"      // ✅ new
        ),
        SeedEvent(
            id = EVENT_BANDS_ID,
            title = "Battle of the Bands",
            clubName = "Music & Arts Club",
            bannerUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800",
            eventDate = "Fri, 24 Oct",
            eventTime = "05:30 PM",
            venue = "Open Air Theatre",
            description = "An electric evening where the best college bands face off for the ultimate title. Rock, pop, fusion, and pure energy await you at the OAT.",
            totalSeats = 500,
            registrationFee = "Free",
            category = "Cultural",
            statusBadge = "Registration Open",
            creatorId = ADMIN_NEHA_ID,               // ✅ new
            createdBy = "Neha Sharma"                 // ✅ new
        ),
        SeedEvent(
            id = EVENT_UIUX_ID,
            title = "Mastering UI/UX Design",
            clubName = "Design Enthusiasts Guild",
            bannerUrl = "https://images.unsplash.com/photo-1586717791821-3f44a563fa4c?w=800",
            eventDate = "Sat, 25 Oct",
            eventTime = "11:00 AM",
            venue = "Computer Lab 04",
            description = "Learn the fundamentals of wireframing, prototyping, and modern design systems like Material 3 and iOS Human Interface Guidelines from industry veterans.",
            totalSeats = 40,
            registrationFee = "Free",
            category = "Technical",
            statusBadge = "Registration Open",
            creatorId = TEACHER_VIKRAM_ID,           // ✅ new
            createdBy = "Prof. Vikram Malhotra"      // ✅ new
        ),
        SeedEvent(
            id = EVENT_BASKET_ID,
            title = "Inter-College Basketball Finals",
            clubName = "Athletics Council",
            bannerUrl = "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800",
            eventDate = "Sun, 26 Oct",
            eventTime = "09:00 AM",
            venue = "Main Indoor Arena",
            description = "The ultimate clash for the championship trophy. Come out and cheer for our varsity team as they take on our biggest rivals.",
            totalSeats = 150,
            registrationFee = "Free",
            category = "Sports",
            statusBadge = "Starts in 2 Days",
            creatorId = ADMIN_NEHA_ID,               // ✅ new
            createdBy = "Neha Sharma"                 // ✅ new
        )
    )

    events.forEach { event ->
        EventsTable.insert {
            it[id] = event.id
            it[title] = event.title
            it[clubName] = event.clubName
            it[bannerUrl] = event.bannerUrl
            it[eventDate] = event.eventDate
            it[eventTime] = event.eventTime
            it[venue] = event.venue
            it[description] = event.description
            it[totalSeats] = event.totalSeats
            it[registrationFee] = event.registrationFee
            it[category] = event.category
            it[statusBadge] = event.statusBadge
            it[creatorId] = event.creatorId           // ✅ new
            it[createdBy] = event.createdBy           // ✅ new
        }
        logger.debug { "  Seeded Event: ${event.title}" }
    }
}

private fun seedRegistrations() {
    val registrations = listOf(
        EVENT_TECH_ID,
        EVENT_BANDS_ID
    )

    registrations.forEach { eventId ->
        EventRegistrationsTable.insert {
            it[id] = UUID.randomUUID().toString()
            it[studentId] = STUDENT_ALEX_ID
            it[this.eventId] = eventId
        }
    }
    logger.debug { "  Pre-registered student Alex to ${registrations.size} events" }
}