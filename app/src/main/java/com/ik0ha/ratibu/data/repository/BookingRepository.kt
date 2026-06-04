package com.ik0ha.ratibu.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ik0ha.ratibu.data.CacheManager
import com.ik0ha.ratibu.data.Session
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BookingRepository(private val cacheManager: CacheManager? = null) {
    private val db = FirebaseDatabase.getInstance().reference

    fun getBookingsByProvider(providerId: String): Flow<List<Session>> = callbackFlow {
        // Provide cached data first if available
        cacheManager?.let { 
            val cached = it.getBookings("provider_bookings_$providerId")
            if (cached.isNotEmpty()) trySend(cached)
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Session>()
                for (child in snapshot.children) {
                    child.getValue(Session::class.java)?.let { list.add(it) }
                }
                cacheManager?.saveBookings("provider_bookings_$providerId", list)
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        val ref = db.child("bookings").orderByChild("providerId").equalTo(providerId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getBookingsByClient(clientId: String): Flow<List<Session>> = callbackFlow {
        cacheManager?.let { 
            val cached = it.getBookings("client_bookings_$clientId")
            if (cached.isNotEmpty()) trySend(cached)
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Session>()
                for (child in snapshot.children) {
                    child.getValue(Session::class.java)?.let { list.add(it) }
                }
                cacheManager?.saveBookings("client_bookings_$clientId", list)
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        val ref = db.child("bookings").orderByChild("clientId").equalTo(clientId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun updateBookingStatus(bookingId: String, newStatus: String) {
        db.child("bookings").child(bookingId).child("status").setValue(newStatus)
    }

    fun generateBookingKey(): String? {
        return db.child("bookings").push().key
    }

    fun saveBooking(session: Session, onResult: (Boolean) -> Unit) {
        db.child("bookings").child(session.id).setValue(session)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }
}
