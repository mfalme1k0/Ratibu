package com.ik0ha.ratibu.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ik0ha.ratibu.data.*
import com.ik0ha.ratibu.data.repository.BookingRepository
import com.ik0ha.ratibu.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
        if (providerId.isEmpty()) return
        
        db.child("providers").child(providerId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val profile = snapshot.getValue(ServiceProvider::class.java)
                    profile?.let { 
                        _providerProfile.value = it
                        cacheManager.saveProfile(it)
                    }
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Error parsing profile", e)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DashboardViewModel", "Profile sync cancelled: ${error.message}")
            }
        })
    }

    private fun observeBookings() {
        if (providerId.isEmpty()) {
            _isLoading.value = false
            return
        }
        
        bookingRepository.getBookingsByProvider(providerId)
            .onEach { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                    }
                    is NetworkResult.Success -> {
                        val list = result.data
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
                    }
                    is NetworkResult.Error -> {
                        Log.e("DashboardViewModel", "Error observing bookings: ${result.message}")
                        _isLoading.value = false
                    }
                }
            }
            .catch { e ->
                Log.e("DashboardViewModel", "Fatal error observing bookings", e)
                _isLoading.value = false
            }
            .launchIn(viewModelScope)
    }

    private fun computeAnalytics(bookings: List<Session>) {
        try {
            _analytics.value = AnalyticsEngine.computeSummary(bookings)
            _detailedAnalytics.value = AnalyticsEngine.computeDetailed(bookings)
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error computing analytics", e)
        }
    }

    fun updateBookingStatus(bookingId: String, newStatus: String) {
        bookingRepository.updateBookingStatus(bookingId, newStatus)
    }

    fun addWalkIn(clientName: String, startTime: Long, notes: String) {
        if (providerId.isEmpty()) return
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
        if (providerId.isEmpty()) return
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
        if (providerId.isEmpty()) return
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
        if (providerId.isEmpty()) return
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
        if (providerId.isEmpty()) return
        val currentSamples = _providerProfile.value?.workSamples?.toMutableList() ?: return
        currentSamples.remove(sample)
        db.child("providers").child(providerId).child("workSamples").setValue(currentSamples)
    }
}
