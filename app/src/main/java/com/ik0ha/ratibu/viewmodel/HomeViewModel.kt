package com.ik0ha.ratibu.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ik0ha.ratibu.data.Review
import com.ik0ha.ratibu.data.ServiceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class HomeViewModel : ViewModel() {
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    
    private val _providers = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val providers: StateFlow<List<ServiceProvider>> = _providers

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    init {
        fetchProviders()
        fetchUserRole()
    }

    private fun fetchProviders() {
        db.child("providers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ServiceProvider>()
                for (child in snapshot.children) {
                    try {
                        val provider = child.getValue(ServiceProvider::class.java)
                        if (provider != null) {
                            list.add(provider)
                        }
                    } catch (e: Exception) {
                        // Handle legacy data format
                        val uid = child.child("uid").getValue(String::class.java) ?: ""
                        val name = child.child("name").getValue(String::class.java) ?: ""
                        val category = child.child("category").getValue(String::class.java) ?: ""
                        val bio = child.child("bio").getValue(String::class.java) ?: ""
                        val imageUrl = child.child("imageUrl").getValue(String::class.java) ?: ""
                        val phoneNumber = child.child("phoneNumber").getValue(String::class.java) ?: ""
                        val location = child.child("location").getValue(String::class.java) ?: ""
                        val rating = child.child("rating").getValue(Double::class.java) ?: 0.0

                        val workSamples = mutableListOf<com.ik0ha.ratibu.data.WorkSample>()
                        for (sampleSnap in child.child("workSamples").children) {
                            try {
                                val sample = sampleSnap.getValue(com.ik0ha.ratibu.data.WorkSample::class.java)
                                if (sample != null) workSamples.add(sample)
                            } catch (e2: Exception) {
                                val url = sampleSnap.getValue(String::class.java) ?: ""
                                if (url.isNotEmpty()) {
                                    workSamples.add(com.ik0ha.ratibu.data.WorkSample(imageUrl = url, description = "Portfolio"))
                                }
                            }
                        }

                        list.add(ServiceProvider(
                            uid = uid,
                            name = name,
                            category = category,
                            bio = bio,
                            imageUrl = imageUrl,
                            phoneNumber = phoneNumber,
                            location = location,
                            rating = rating,
                            workSamples = workSamples
                        ))
                    }
                }
                _providers.value = list
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchUserRole() {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("role").get().addOnSuccessListener {
            _userRole.value = it.getValue(String::class.java)
        }
    }

    fun submitReview(providerId: String, rating: Double, comment: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.child("users").child(currentUserId).child("name").get().addOnSuccessListener { snapshot ->
            val reviewerName = snapshot.getValue(String::class.java) ?: "Anonymous"
            val review = Review(
                reviewerName = reviewerName,
                rating = rating,
                comment = comment,
                timestamp = System.currentTimeMillis()
            )

            val providerRef = db.child("providers").child(providerId)
            providerRef.get().addOnSuccessListener { providerSnapshot ->
                val provider = providerSnapshot.getValue(ServiceProvider::class.java)
                if (provider != null) {
                    val currentReviews = provider.reviews.toMutableList()
                    currentReviews.add(review)
                    
                    val newRating = if (currentReviews.isEmpty()) 0.0 else currentReviews.map { it.rating }.average()
                    
                    val updates = mapOf(
                        "reviews" to currentReviews,
                        "rating" to String.format(Locale.US, "%.1f", newRating).toDouble()
                    )
                    providerRef.updateChildren(updates)
                }
            }
        }
    }
}
