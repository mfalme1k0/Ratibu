package com.ik0ha.ratibu.data

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "CLIENT" // CLIENT or PROVIDER
)

data class ServiceProvider(
    val uid: String = "",
    val name: String = "",
    val category: String = "",
    val bio: String = "",
    val imageUrl: String = "",
    val phoneNumber: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Double = 0.0,
    val workSamples: List<WorkSample> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val slotDurationMinutes: Int = 60,
    val bufferTimeMinutes: Int = 15,
    val workStartHour: Int = 9,    // 9 AM
    val workEndHour: Int = 18,    // 6 PM
    val availability: Map<String, List<String>> = emptyMap() // Day -> List of slots
)

data class WorkSample(
    val imageUrl: String = "",
    val description: String = ""
)

data class Review(
    val reviewerName: String = "",
    val rating: Double = 0.0,
    val comment: String = "",
    val timestamp: Long = 0L
)

data class Session(
    val id: String = "",
    val clientId: String = "",
    val clientName: String = "Walk-in",
    val providerId: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val status: String = "PENDING", // PENDING, CONFIRMED, CANCELLED, COMPLETED
    val type: String = "BOOKING", // BOOKING, WALK_IN, BLOCKED
    val reminderTimeMinutes: Int = 30,
    val notes: String = ""
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

data class ChatChannel(
    val id: String = "",
    val clientId: String = "",
    val providerId: String = "",
    val clientName: String = "",
    val providerName: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = 0L
)

data class DetailedAnalytics(
    val dayDistribution: Map<String, Int> = emptyMap(),
    val hourDistribution: Map<String, Int> = emptyMap(),
    val uniqueClients: Int = 0,
    val repeatClients: Int = 0,
    val statusBreakdown: Map<String, Int> = emptyMap()
)
