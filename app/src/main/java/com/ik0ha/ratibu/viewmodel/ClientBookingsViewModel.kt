package com.ik0ha.ratibu.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ik0ha.ratibu.data.CacheManager
import com.ik0ha.ratibu.data.NetworkResult
import com.ik0ha.ratibu.data.Session
import com.ik0ha.ratibu.data.repository.BookingRepository
import com.ik0ha.ratibu.data.repository.UserRepository
import kotlinx.coroutines.flow.*

class ClientBookingsViewModel(application: Application) : AndroidViewModel(application) {
    private val cacheManager = CacheManager(application)
    private val bookingRepository = BookingRepository(cacheManager)
    private val userRepository = UserRepository()

    private val _bookings = MutableStateFlow<List<Session>>(emptyList())
    val bookings: StateFlow<List<Session>> = _bookings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchMyBookings()
    }

    private fun fetchMyBookings() {
        val uid = userRepository.getCurrentUserId() ?: return
        bookingRepository.getBookingsByClient(uid)
            .onEach { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _bookings.value = result.data.sortedByDescending { it.startTime }
                        _isLoading.value = false
                    }
                    is NetworkResult.Error -> {
                        Log.e("ClientBookingsVM", "Error: ${result.message}")
                        _isLoading.value = false
                    }
                    is NetworkResult.Loading -> _isLoading.value = true
                }
            }
            .catch { e -> 
                Log.e("ClientBookingsVM", "Fatal error client bookings", e)
                _isLoading.value = false
            }
            .launchIn(viewModelScope)
    }
    
    fun cancelBooking(bookingId: String) {
        if (bookingId.isNotEmpty()) {
            bookingRepository.updateBookingStatus(bookingId, "CANCELLED")
        }
    }
}
