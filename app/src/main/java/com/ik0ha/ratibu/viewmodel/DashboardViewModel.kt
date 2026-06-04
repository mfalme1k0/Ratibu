package com.ik0ha.ratibu.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ik0ha.ratibu.data.CacheManager
import com.ik0ha.ratibu.data.CloudinaryHelper
import com.ik0ha.ratibu.data.DetailedAnalytics
import com.ik0ha.ratibu.data.ServiceProvider
import com.ik0ha.ratibu.data.Session
import com.ik0ha.ratibu.data.WorkSample
import com.ik0ha.ratibu.data.repository.BookingRepository
import com.ik0ha.ratibu.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
    private val providerId = auth.currentUser?.uid ?: ""
    
    private val cacheManager = CacheManager(application)
    private val userRepository = UserRepository()
    private val bookingRepository = BookingRepository(cacheManager)

    private val _providerProfile = MutableStateFlow<ServiceProvider?>(null)
    val providerProfile: StateFlow<ServiceProvider?> = _providerProfile

    private val _bookings = MutableStateFlow<List<Session>>(emptyList())
    val bookings: StateFlow<List<Session>> = _bookings

    private val _upcomingBookings = MutableStateFlow<List<Session>>(emptyList())
    val upcomingBookings: StateFlow<List<Session>> = _upcomingBookings

    private val _completedBookings = MutableStateFlow<List<Session>>(emptyList())
    val completedBookings: StateFlow<List<Session>> = _completedBookings

    private val _todayBookings = MutableStateFlow<List<Session>>(emptyList())
    val todayBookings: StateFlow<List<Session>> = _todayBookings

    private val _analytics = MutableStateFlow<Map<String, String>>(emptyMap())
    val analytics: StateFlow<Map<String, String>> = _analytics

    private val _detailedAnalytics = MutableStateFlow<DetailedAnalytics?>(null)
    val detailedAnalytics: StateFlow<DetailedAnalytics?> = _detailedAnalytics

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        if (providerId.isNotEmpty()) {
            _providerProfile.value = cacheManager.getProfile(providerId)
            fetchProfile()
            observeBookings()
        } else {
            _isLoading.value = false
        }
    }

    private fun fetchProfile() {
        db.child("providers").child(providerId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val profile = snapshot.getValue(ServiceProvider::class.java)
                    profile?.let { 
                        _providerProfile.value = it
                        cacheManager.saveProfile(it)
                    }
                } catch (e: Exception) {
                    // Fallback logic for legacy data
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
                    cacheManager.saveProfile(profile)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun observeBookings() {
        bookingRepository.getBookingsByProvider(providerId)
            .onEach { list ->
                val sorted = list.sortedBy { it.startTime }
                _bookings.value = sorted
                
                _upcomingBookings.value = sorted.filter { 
                    it.status != "COMPLETED" && it.status != "CANCELLED"
                }

                _completedBookings.value = sorted.filter { it.status == "COMPLETED" }

                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis
                
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val endOfDay = cal.timeInMillis
                
                _todayBookings.value = sorted.filter { it.startTime in startOfDay..endOfDay }

                viewModelScope.launch(Dispatchers.Default) {
                    computeAnalytics(sorted)
                }
                _isLoading.value = false
            }.launchIn(viewModelScope)
    }

    private fun computeAnalytics(bookings: List<Session>) {
        val completed = bookings.filter { it.status == "COMPLETED" }
        
        val dayCounts = completed.groupBy { 
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date(it.startTime))
        }.mapValues { it.value.size }

        val hourCounts = completed.groupBy { 
            SimpleDateFormat("HH:00", Locale.getDefault()).format(Date(it.startTime))
        }.mapValues { it.value.size }

        val busiestDay = dayCounts.maxByOrNull { it.value }?.key ?: "None"
        val busiestHour = hourCounts.maxByOrNull { it.value }?.key ?: "None"

        val uniqueClients = bookings.filter { it.clientId != "walk-in" }.map { it.clientId }.distinct().size
        val repeatClients = bookings.filter { it.clientId != "walk-in" }
            .groupBy { it.clientId }
            .filter { it.value.size > 1 }.size
        val retentionRate = if (uniqueClients > 0) (repeatClients * 100 / uniqueClients) else 0

        val statusCounts = bookings.groupBy { it.status }.mapValues { it.value.size }

        _analytics.value = mapOf(
            "Busiest Day" to busiestDay,
            "Peak Hour" to busiestHour,
            "Retention" to "$retentionRate%",
            "Completion Rate" to "${if (bookings.isNotEmpty()) (completed.size * 100 / bookings.size) else 0}%"
        )

        _detailedAnalytics.value = DetailedAnalytics(
            dayDistribution = dayCounts,
            hourDistribution = hourCounts,
            uniqueClients = uniqueClients,
            repeatClients = repeatClients,
            statusBreakdown = statusCounts
        )
    }

    fun updateBookingStatus(bookingId: String, newStatus: String) {
        bookingRepository.updateBookingStatus(bookingId, newStatus)
    }

    fun addWalkIn(clientName: String, startTime: Long, notes: String) {
        val sessionId = bookingRepository.generateBookingKey() ?: return
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
        bookingRepository.saveBooking(session) {}
    }

    fun blockTime(startTime: Long, durationMinutes: Int, reason: String) {
        val sessionId = bookingRepository.generateBookingKey() ?: return
        val session = Session(
            id = sessionId,
            providerId = providerId,
            startTime = startTime,
            endTime = startTime + (durationMinutes * 60 * 1000L),
            status = "CONFIRMED",
            type = "BLOCKED",
            notes = reason
        )
        bookingRepository.saveBooking(session) {}
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
        workStart: Int,
        workEnd: Int,
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
                "longitude" to longitude,
                "workStartHour" to workStart,
                "workEndHour" to workEnd
            )
            userRepository.updateProviderProfile(providerId, updates, onSuccess, onFailure)
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
