package com.ik0ha.ratibu.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ik0ha.ratibu.data.CacheManager
import com.ik0ha.ratibu.data.Session

class BookingRepository(private val cacheManager: CacheManager? = null) {
    private val db = FirebaseDatabase.getInstance().reference

    fun getBookingsByProvider(providerId: String, onResult: (List<Session>) -> Unit) {
        // Provide cached data first if available
        cacheManager?.let { 
            val cached = it.getBookings("provider_bookings_$providerId")
            if (cached.isNotEmpty()) onResult(cached)
        }

        db.child("bookings").orderByChild("providerId").equalTo(providerId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Session>()
                    for (child in snapshot.children) {
                        child.getValue(Session::class.java)?.let { list.add(it) }
                    }
                    cacheManager?.saveBookings("provider_bookings_$providerId", list)
                    onResult(list)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun getBookingsByClient(clientId: String, onResult: (List<Session>) -> Unit) {
        cacheManager?.let { 
            val cached = it.getBookings("client_bookings_$clientId")
            if (cached.isNotEmpty()) onResult(cached)
        }

        db.child("bookings").orderByChild("clientId").equalTo(clientId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Session>()
                    for (child in snapshot.children) {
                        child.getValue(Session::class.java)?.let { list.add(it) }
                    }
                    cacheManager?.saveBookings("client_bookings_$clientId", list)
                    onResult(list)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
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
