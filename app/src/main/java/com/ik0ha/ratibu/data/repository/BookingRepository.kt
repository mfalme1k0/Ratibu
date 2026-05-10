package com.ik0ha.ratibu.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ik0ha.ratibu.data.Session

class BookingRepository {
    private val db = FirebaseDatabase.getInstance().reference

    fun getBookingsByProvider(providerId: String, onResult: (List<Session>) -> Unit) {
        db.child("bookings").orderByChild("providerId").equalTo(providerId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Session>()
                    for (child in snapshot.children) {
                        child.getValue(Session::class.java)?.let { list.add(it) }
                    }
                    onResult(list)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun saveBooking(session: Session, onResult: (Boolean) -> Unit) {
        db.child("bookings").child(session.id).setValue(session)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun updateBookingStatus(bookingId: String, newStatus: String) {
        db.child("bookings").child(bookingId).child("status").setValue(newStatus)
    }

    fun generateBookingKey(): String? = db.child("bookings").push().key
}
