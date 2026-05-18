package com.ik0ha.ratibu.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.database.FirebaseDatabase
import com.ik0ha.ratibu.data.CacheManager
import com.ik0ha.ratibu.data.ReminderWorker
import com.ik0ha.ratibu.data.Session
import com.ik0ha.ratibu.data.ServiceProvider
import com.ik0ha.ratibu.data.repository.BookingRepository
import com.ik0ha.ratibu.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

class BookingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseDatabase.getInstance().reference
    private val cacheManager = CacheManager(application)
    private val bookingRepository = BookingRepository(cacheManager)
    private val userRepository = UserRepository()

    private val _bookedRanges = MutableStateFlow<List<LongRange>>(emptyList())
    val bookedRanges: StateFlow<List<LongRange>> = _bookedRanges

    private val _providerSettings = MutableStateFlow<ServiceProvider?>(null)
    val providerSettings: StateFlow<ServiceProvider?> = _providerSettings

    fun fetchBookedSlots(providerId: String) {
        db.child("providers").child(providerId).get().addOnSuccessListener { providerSnapshot ->
            val provider = providerSnapshot.getValue(ServiceProvider::class.java)
            _providerSettings.value = provider

            val duration = (provider?.slotDurationMinutes ?: 30) * 60 * 1000L
            val buffer = (provider?.bufferTimeMinutes ?: 10) * 60 * 1000L
            
            bookingRepository.getBookingsByProvider(providerId) { list ->
                val occupiedTimeRanges = list.filter { it.status != "CANCELLED" }
                    .map { it.startTime..(it.startTime + duration + buffer) }
                _bookedRanges.value = occupiedTimeRanges
            }
        }
    }

    fun bookSession(providerId: String, providerName: String, startTime: Long, reminderMinutes: Int, notes: String = "") {
        val clientId = userRepository.getCurrentUserId() ?: return
        
        db.child("users").child(clientId).child("name").get().addOnSuccessListener { snapshot ->
            val clientName = snapshot.getValue(String::class.java) ?: "Client"
            val sessionId = bookingRepository.generateBookingKey() ?: return@addOnSuccessListener
            
            val session = Session(
                id = sessionId,
                clientId = clientId,
                clientName = clientName,
                providerId = providerId,
                providerName = providerName,
                startTime = startTime,
                status = "PENDING",
                reminderTimeMinutes = reminderMinutes,
                notes = notes
            )

            bookingRepository.saveBooking(session) { success ->
                if (success) {
                    Toast.makeText(getApplication(), "Booking successful", Toast.LENGTH_SHORT).show()
                    
                    val now = System.currentTimeMillis()
                    // 24 hours before
                    val delay24h = (startTime - now) - (24 * 60 * 60 * 1000L)
                    if (delay24h > 0) {
                        scheduleReminder("Reminder: 24h before session", providerName, delay24h / (60 * 1000L), "24 hours")
                    }
                    
                    // 2 hours before
                    val delay2h = (startTime - now) - (2 * 60 * 60 * 1000L)
                    if (delay2h > 0) {
                        scheduleReminder("Reminder: 2h before session", providerName, delay2h / (60 * 1000L), "2 hours")
                    }
                } else {
                    Toast.makeText(getApplication(), "Booking failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun joinWaitlist(providerId: String, startTime: Long) {
        val clientId = userRepository.getCurrentUserId() ?: return
        db.child("waitlist").child(providerId).child(startTime.toString()).child(clientId).setValue(true)
            .addOnSuccessListener {
                Toast.makeText(getApplication(), "Joined waitlist", Toast.LENGTH_SHORT).show()
            }
    }

    private fun scheduleReminder(sessionName: String, providerName: String, delayMinutes: Long, timeLabel: String) {
        val inputData = Data.Builder()
            .putString("session_name", sessionName)
            .putString("provider_name", providerName)
            .putString("time_label", timeLabel)
            .build()

        val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(getApplication()).enqueue(reminderRequest)
    }
}
