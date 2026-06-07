package com.ik0ha.ratibu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ik0ha.ratibu.data.Review
import com.ik0ha.ratibu.data.ServiceProvider
import com.ik0ha.ratibu.data.NetworkResult
import com.ik0ha.ratibu.data.repository.UserRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.Locale

class HomeViewModel : ViewModel() {
    private val db = FirebaseDatabase.getInstance().reference
    private val userRepository = UserRepository()
    
    private val _providers = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val providers: StateFlow<List<ServiceProvider>> = _providers

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchProviders()
        fetchUserRole()
    }

    private fun fetchProviders() {
        callbackFlow<NetworkResult<List<ServiceProvider>>> {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ServiceProvider>()
                    for (child in snapshot.children) {
                        try {
                            val provider = child.getValue(ServiceProvider::class.java)
                            if (provider != null) {
                                list.add(provider)
                            }
                        } catch (e: Exception) {
                            Log.e("HomeViewModel", "Fallback parsing for provider", e)
                        }
                    }
                    trySend(NetworkResult.Success(list))
                }

                override fun onCancelled(error: DatabaseError) {
                    // CRITICAL FIX: Do NOT call close(error.toException()) which crashes the app
                    trySend(NetworkResult.Error(error.message))
                }
            }
            val ref = db.child("providers")
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }
        .onStart { _isLoading.value = true }
        .onEach { result ->
            when (result) {
                is NetworkResult.Success -> {
                    _providers.value = result.data
                    _isLoading.value = false
                }
                is NetworkResult.Error -> {
                    Log.e("HomeViewModel", "Error: ${result.message}")
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> _isLoading.value = true
            }
        }
        .catch { e -> 
            Log.e("HomeViewModel", "Fatal error in provider flow", e)
            _isLoading.value = false
        }
        .launchIn(viewModelScope)
    }

    private fun fetchUserRole() {
        val uid = userRepository.getCurrentUserId() ?: return
        userRepository.getUserRole(uid) { role ->
            _userRole.value = role
        }
    }

    fun submitReview(providerId: String, rating: Double, comment: String) {
        val currentUserId = userRepository.getCurrentUserId() ?: return
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
