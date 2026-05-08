package com.ik0ha.ratibu.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ik0ha.ratibu.data.CloudinaryHelper
import com.ik0ha.ratibu.data.ServiceProvider
import com.ik0ha.ratibu.data.Session
import com.ik0ha.ratibu.data.WorkSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
    private val providerId = auth.currentUser?.uid ?: ""

    private val _providerProfile = MutableStateFlow<ServiceProvider?>(null)
    val providerProfile: StateFlow<ServiceProvider?> = _providerProfile

    private val _bookings = MutableStateFlow<List<Session>>(emptyList())
    val bookings: StateFlow<List<Session>> = _bookings

    private val _analytics = MutableStateFlow<Map<String, String>>(emptyMap())
    val analytics: StateFlow<Map<String, String>> = _analytics

    private val _detailedAnalytics = MutableStateFlow<DetailedAnalytics?>(null)
    val detailedAnalytics: StateFlow<DetailedAnalytics?> = _detailedAnalytics

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    init {
        if (providerId.isNotEmpty()) {
            fetchProfile()
            fetchBookings()
        }
    }

    private fun fetchProfile() {
        db.child("providers").child(providerId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Try parsing as current model
                    val profile = snapshot.getValue(ServiceProvider::class.java)
                    _providerProfile.value = profile
                } catch (e: Exception) {
                    // Fallback: manually parse to handle old data format in workSamples
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val category = snapshot.child("category").getValue(String::class.java) ?: ""
                    val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                    val imageUrl = snapshot.child("imageUrl").getValue(String::class.java) ?: ""
                    val phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                    val location = snapshot.child("location").getValue(String::class.java) ?: ""
                    val latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    val rating = snapshot.child("rating").getValue(Double::class.java) ?: 0.0
                    
                    val workSamples = mutableListOf<WorkSample>()
                    val samplesSnapshot = snapshot.child("workSamples")
                    for (sampleSnap in samplesSnapshot.children) {
                        try {
                            val sample = sampleSnap.getValue(WorkSample::class.java)
                            if (sample != null) workSamples.add(sample)
                        } catch (e2: Exception) {
                            // It's likely a simple String URL (old format)
                            val url = sampleSnap.getValue(String::class.java) ?: ""
                            if (url.isNotEmpty()) {
                                workSamples.add(WorkSample(imageUrl = url, description = "Previous Work"))
                            }
                        }
                    }
                    
                    val profile = ServiceProvider(
                        uid = providerId,
                        name = name,
                        category = category,
                        bio = bio,
                        imageUrl = imageUrl,
                        phoneNumber = phoneNumber,
                        location = location,
                        latitude = latitude,
                        longitude = longitude,
                        rating = rating,
                        workSamples = workSamples
                    )
                    _providerProfile.value = profile
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchBookings() {
        db.child("bookings").orderByChild("providerId").equalTo(providerId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Session>()
                    for (child in snapshot.children) {
                        child.getValue(Session::class.java)?.let { list.add(it) }
                    }
                    val sorted = list.sortedBy { it.startTime }
                    _bookings.value = sorted
                    computeAnalytics(sorted)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun computeAnalytics(bookings: List<Session>) {
        val completed = bookings.filter { it.status == "COMPLETED" }
        val busiestDay = completed.groupBy { 
            SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(it.startTime))
        }.maxByOrNull { it.value.size }?.key ?: "None"
        
        val busiestHour = completed.groupBy { 
            SimpleDateFormat("HH:00", Locale.getDefault()).format(Date(it.startTime))
        }.maxByOrNull { it.value.size }?.key ?: "None"

        _analytics.value = mapOf(
            "Busiest Day" to busiestDay,
            "Peak Hour" to busiestHour,
            "Completion Rate" to "${if (bookings.isNotEmpty()) (completed.size * 100 / bookings.size) else 0}%"
        )

        // Compute Detailed Analytics
        val dayCounts = completed.groupBy { 
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date(it.startTime))
        }.mapValues { it.value.size }

        val hourCounts = completed.groupBy { 
            SimpleDateFormat("HH:00", Locale.getDefault()).format(Date(it.startTime))
        }.mapValues { it.value.size }

        val uniqueClients = bookings.filter { it.clientId != "walk-in" }.map { it.clientId }.distinct().size
        val repeatClients = bookings.filter { it.clientId != "walk-in" }
            .groupBy { it.clientId }
            .filter { it.value.size > 1 }.size

        val statusCounts = bookings.groupBy { it.status }.mapValues { it.value.size }

        _detailedAnalytics.value = DetailedAnalytics(
            dayDistribution = dayCounts,
            hourDistribution = hourCounts,
            uniqueClients = uniqueClients,
            repeatClients = repeatClients,
            statusBreakdown = statusCounts
        )
    }

data class DetailedAnalytics(
    val dayDistribution: Map<String, Int>,
    val hourDistribution: Map<String, Int>,
    val uniqueClients: Int,
    val repeatClients: Int,
    val statusBreakdown: Map<String, Int>
)

    fun updateBookingStatus(bookingId: String, newStatus: String) {
        db.child("bookings").child(bookingId).child("status").setValue(newStatus)
    }

    fun addWalkIn(clientName: String, startTime: Long, notes: String) {
        val sessionId = db.child("bookings").push().key ?: return
        val session = Session(
            id = sessionId,
            clientId = "walk-in",
            clientName = clientName,
            providerId = providerId,
            startTime = startTime,
            status = "CONFIRMED",
            type = "WALK_IN",
            notes = notes
        )
        db.child("bookings").child(sessionId).setValue(session)
    }

    fun blockTime(startTime: Long, durationMinutes: Int, reason: String) {
        val sessionId = db.child("bookings").push().key ?: return
        val session = Session(
            id = sessionId,
            providerId = providerId,
            startTime = startTime,
            endTime = startTime + (durationMinutes * 60 * 1000L),
            status = "CONFIRMED",
            type = "BLOCKED",
            notes = reason
        )
        db.child("bookings").child(sessionId).setValue(session)
    }

    fun updateProfile(
        bio: String,
        category: String,
        slotDuration: Int,
        bufferTime: Int,
        phoneNumber: String,
        location: String,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (providerId.isNotEmpty()) {
            val updates = mapOf(
                "bio" to bio,
                "category" to category,
                "slotDurationMinutes" to slotDuration,
                "bufferTimeMinutes" to bufferTime,
                "phoneNumber" to phoneNumber,
                "location" to location,
                "latitude" to latitude,
                "longitude" to longitude
            )
            db.child("providers").child(providerId).updateChildren(updates)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it.message ?: "Update failed") }
        }
    }

    fun uploadProfilePhoto(uri: Uri) {
        _uploading.value = true
        CloudinaryHelper.uploadImage(uri, 
            onSuccess = { url ->
                db.child("providers").child(providerId).child("imageUrl").setValue(url)
                _uploading.value = false
            },
            onError = {
                _uploading.value = false
            }
        )
    }

    fun addWorkSample(uri: Uri, description: String) {
        _uploading.value = true
        CloudinaryHelper.uploadImage(uri,
            onSuccess = { url ->
                val currentSamples = _providerProfile.value?.workSamples?.toMutableList() ?: mutableListOf()
                currentSamples.add(WorkSample(imageUrl = url, description = description))
                db.child("providers").child(providerId).child("workSamples").setValue(currentSamples)
                _uploading.value = false
            },
            onError = {
                _uploading.value = false
            }
        )
    }

    fun deleteWorkSample(sample: WorkSample) {
        val currentSamples = _providerProfile.value?.workSamples?.toMutableList() ?: return
        currentSamples.remove(sample)
        db.child("providers").child(providerId).child("workSamples").setValue(currentSamples)
    }
}
