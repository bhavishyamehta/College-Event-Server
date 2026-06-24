package domain

import kotlinx.serialization.Serializable

// Authentication Requests
@Serializable
data class RegisterRequest(
    val fullName: String,
    val enrollmentNumber: String,
    val branchDepartment: String,
    val universityEmail: String,
    val password: String,
    val role: String = "STUDENT"
)

@Serializable
data class LoginRequest(
    val enrollmentNumber: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val student: StudentProfileResponse
)

// UI Data Structures based on design
@Serializable
data class StudentProfileResponse(
    val id: String,
    val profileImage: String? = "",
    val fullName: String,
    val enrollmentNumber: String,
    val branchDepartment: String,
    val universityEmail: String,
    val totalEvents: Int,
    val certificatesCount: Int,
    val registeredEvents: List<EventSummaryResponse>,
    val role: String
)

@Serializable
data class CreateEventRequest(
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
    val statusBadge: String = "Registration Open"
)

@Serializable
data class CreateEventResponse(
    val success: Boolean,
    val message: String,
    val id: String
)

@Serializable
data class EventSummaryResponse(
    val id: String,
    val title: String,
    val clubName: String,
    val bannerUrl: String,
    val date: String,
    val time: String,
    val venue: String,
    val registrationBadge: String // e.g., "Registration Open", "Starts in 2 Days"
)

@Serializable
data class EventDetailResponse(
    val id: String,
    val title: String,
    val clubName: String,
    val bannerUrl: String,
    val date: String,
    val time: String,
    val venue: String,
    val description: String,
    val seatAvailability: String, // e.g., "Seat 34/50" or "Spots Left: 12"
    val registrationFee: String,   // e.g., "$15.00" or "Free"
    val isUserRegistered: Boolean
)

@Serializable
data class GenericResponse(
    val success: Boolean,
    val message: String
)