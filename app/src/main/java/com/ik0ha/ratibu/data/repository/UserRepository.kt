package com.ik0ha.ratibu.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ik0ha.ratibu.data.NetworkResult
import com.ik0ha.ratibu.data.ServiceProvider
import com.ik0ha.ratibu.data.User
import com.ik0ha.ratibu.data.UserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getUserRole(uid: String, onResult: (String?) -> Unit) {
        if (uid.isEmpty()) {
            onResult(null)
            return
        }
        db.child("users").child(uid).child("role").get()
            .addOnSuccessListener { onResult(it.getValue(String::class.java)) }
            .addOnFailureListener { 
                Log.e("UserRepository", "Error getting role", it)
                onResult(null) 
            }
    }

    fun observeUserRole(uid: String): Flow<NetworkResult<String?>> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(NetworkResult.Error("User ID is empty"))
            return@callbackFlow
        }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(NetworkResult.Success(snapshot.getValue(String::class.java)))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error(error.message))
            }
        }
        val ref = db.child("users").child(uid).child("role")
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.onStart { emit(NetworkResult.Loading) }

    fun saveUserBatch(user: User, isProvider: Boolean, onResult: (Boolean) -> Unit) {
        val updates = mutableMapOf<String, Any>()
        updates["users/${user.uid}"] = user
        if (isProvider) {
            updates["providers/${user.uid}"] = mapOf(
                "uid" to user.uid,
                "name" to user.name,
                "category" to "General",
                "rating" to 5.0,
                "slotDurationMinutes" to 60,
                "bufferTimeMinutes" to 15,
                "workStartHour" to 9,
                "workEndHour" to 18
            )
        }
        db.updateChildren(updates).addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun updateFcmToken(uid: String, token: String) {
        if (uid.isNotEmpty()) {
            db.child("users").child(uid).child("fcmToken").setValue(token)
        }
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
        if (uid.isEmpty()) {
            onFailure("User ID is empty")
            return
        }
        db.child("providers").child(uid).updateChildren(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Update failed") }
    }

    fun deleteUserData(uid: String, onResult: (Boolean) -> Unit) {
        if (uid.isEmpty()) {
            onResult(false)
            return
        }
        val updates = mapOf(
            "users/$uid" to null,
            "providers/$uid" to null
        )
        db.updateChildren(updates).addOnCompleteListener { onResult(it.isSuccessful) }
    }
}
