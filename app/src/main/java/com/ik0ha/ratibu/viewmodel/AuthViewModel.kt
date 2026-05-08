package com.ik0ha.ratibu.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.ik0ha.ratibu.data.User

class AuthViewModel(
    private val navController: NavHostController,
    private val context: Context
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    fun login(email: String, pass: String) {
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    db.child("users").child(uid).child("role").get().addOnSuccessListener { snapshot ->
                        val role = snapshot.getValue(String::class.java)
                        if (role == "PROVIDER") {
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }.addOnFailureListener {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                } else {
                    Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun register(email: String, pass: String, name: String, role: String) {
        if (email.isEmpty() || pass.isEmpty() || name.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    val user = User(uid, name, email, role)
                    
                    db.child("users").child(uid).setValue(user)
                        .addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                if (role == "PROVIDER") {
                                    // Also add to providers list for searching
                                    val providerData = mapOf(
                                        "uid" to uid,
                                        "name" to name,
                                        "category" to "General", // Default
                                        "rating" to 5.0
                                    )
                                    db.child("providers").child(uid).setValue(providerData)
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Failed to save user data", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun logout() {
        auth.signOut()
        navController.navigate("login") {
            popUpTo(0)
        }
    }
}
