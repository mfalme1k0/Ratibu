package com.ik0ha.ratibu.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.ik0ha.ratibu.data.ServiceProvider
import com.ik0ha.ratibu.data.User

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getUserRole(uid: String, onResult: (String?) -> Unit) {
        db.child("users").child(uid).child("role").get()
            .addOnSuccessListener { onResult(it.getValue(String::class.java)) }
            .addOnFailureListener { onResult(null) }
    }

    fun saveUser(user: User, onResult: (Boolean) -> Unit) {
        db.child("users").child(user.uid).setValue(user)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun saveProviderData(uid: String, name: String, onResult: (Boolean) -> Unit) {
        val providerData = mapOf(
            "uid" to uid,
            "name" to name,
            "category" to "General",
            "rating" to 5.0
        )
        db.child("providers").child(uid).setValue(providerData)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun updateProviderProfile(
        uid: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.child("providers").child(uid).updateChildren(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Update failed") }
    }
}
