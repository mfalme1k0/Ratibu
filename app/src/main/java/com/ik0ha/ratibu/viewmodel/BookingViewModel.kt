package com.ik0ha.ratibu.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ik0ha.ratibu.data.ReminderWorker
import com.ik0ha.ratibu.data.Session
import com.ik0ha.ratibu.data.ServiceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

class BookingViewModel(private val context: Context) : ViewModel() {
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    fun fetchBookedSlots(providerId: String) {
        db.child("providers").child(providerId).get().addOnSuccessListener { providerSnapshot ->
            val provider = providerSnapshot.getValue(ServiceProvider::class.java)
            val duration = (provider?.slotDurationMinutes ?: 30) * 60 * 1000L
            val buffer = (provider?.bufferTimeMinutes ?: 10) * 60 * 1000L
            
            db.child("bookings").orderByChild("providerId").equalTo(providerId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val occupiedTimeRanges = mutableListOf<LongRange>()
                        for (child in snapshot.children) {
                            child.getValue(Session::class.java)?.let { 
                                if (it.status != "CANCELLED") {
                                    // Block the start time + duration + buffer
                                    occupiedTimeRanges.add(it.startTime..(it.startTime + duration + buffer))
                                }
                            }
                        }
                        
                        // For the simple UI, we still return startTimes but the logic in UI will change
                        // Actually, let's keep it simple: if ANY part of a slot is inside occupied ranges, it's booked.
                        _bookedRanges.value = occupiedTimeRanges
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private val _bookedRanges = MutableStateFlow<List<LongRange>>(emptyList())
    val bookedRanges: StateFlow<List<LongRange>> = _bookedRanges

    fun bookSession(providerId: String, providerName: String, startTime: Long, reminderMinutes: Int, notes: String = "") {
        val clientId = auth.currentUser?.uid ?: return
        
        db.child("users").child(clientId).child("name").get().addOnSuccessListener { snapshot ->
            val clientName = snapshot.getValue(String::class.java) ?: "Client"
            val sessionId = db.child("bookings").push().key ?: return@addOnSuccessListener
            
            val session = Session(
                id = sessionId,
                clientId = clientId,
                clientName = clientName,
                providerId = providerId,
                startTime = startTime,
                status = "PENDING",
                reminderTimeMinutes = reminderMinutes,
                notes = notes
            )

            db.child("bookings").child(sessionId).setValue(session)
                .addOnSuccessListener {
                    Toast.makeText(context, "Booking successful", Toast.LENGTH_SHORT).show()
                    
                    // Schedule reminders
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
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Booking failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun joinWaitlist(providerId: String, startTime: Long) {
        val clientId = auth.currentUser?.uid ?: return
        db.child("waitlist").child(providerId).child(startTime.toString()).child(clientId).setValue(true)
            .addOnSuccessListener {
                Toast.makeText(context, "Joined waitlist", Toast.LENGTH_SHORT).show()
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

        WorkManager.getInstance(context).enqueue(reminderRequest)
    }
}
