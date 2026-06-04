package com.ik0ha.ratibu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ik0ha.ratibu.data.CacheManager
import com.ik0ha.ratibu.data.Session
import com.ik0ha.ratibu.data.repository.BookingRepository
import com.ik0ha.ratibu.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ClientBookingsViewModel(application: Application) : AndroidViewModel(application) {
    private val cacheManager = CacheManager(application)
    private val bookingRepository = BookingRepository(cacheManager)
    private val userRepository = UserRepository()

    private val _bookings = MutableStateFlow<List<Session>>(emptyList())
    val bookings: StateFlow<List<Session>> = _bookings

    init {
        fetchMyBookings()
    }

    private fun fetchMyBookings() {
        val uid = userRepository.getCurrentUserId() ?: return
        bookingRepository.getBookingsByClient(uid)
            .onEach { list ->
                _bookings.value = list.sortedByDescending { it.startTime }
            }.launchIn(viewModelScope)
    }
    
    fun cancelBooking(bookingId: String) {
        bookingRepository.updateBookingStatus(bookingId, "CANCELLED")
    }
}
