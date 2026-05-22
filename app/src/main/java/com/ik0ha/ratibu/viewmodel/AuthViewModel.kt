package com.ik0ha.ratibu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.ik0ha.ratibu.data.CacheManager
import com.ik0ha.ratibu.data.User
import com.ik0ha.ratibu.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class AuthEvent {
    object LoginSuccess : AuthEvent()
    object ProviderLoginSuccess : AuthEvent()
    object PasswordResetSent : AuthEvent()
    data class Error(val message: String) : AuthEvent()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val repository = UserRepository()
    private val cacheManager = CacheManager(application)

    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()

    fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: ""
                    updateFcmToken(uid)
                    repository.getUserRole(uid) { role ->
                        viewModelScope.launch {
                            if (role == "PROVIDER") {
                                _events.emit(AuthEvent.ProviderLoginSuccess)
                            } else {
                                _events.emit(AuthEvent.LoginSuccess)
                            }
                        }
                    }
                } else {
                    viewModelScope.launch { _events.emit(AuthEvent.Error("Login failed: ${task.exception?.message}")) }
                }
            }
    }

    fun register(email: String, pass: String, name: String, role: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: ""
                    updateFcmToken(uid)
                    val user = User(uid, name, email, role)
                    
                    repository.saveUserBatch(user, role == "PROVIDER") { success ->
                        viewModelScope.launch {
                            if (success) {
                                if (role == "PROVIDER") {
                                    _events.emit(AuthEvent.ProviderLoginSuccess)
                                } else {
                                    _events.emit(AuthEvent.LoginSuccess)
                                }
                            } else {
                                _events.emit(AuthEvent.Error("Failed to save user data"))
                            }
                        }
                    }
                } else {
                    viewModelScope.launch { _events.emit(AuthEvent.Error("Registration failed: ${task.exception?.message}")) }
                }
            }
    }

    private fun updateFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                repository.updateFcmToken(uid, task.result)
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        auth.signOut()
        cacheManager.clearAll()
        onComplete()
    }

    fun resetPassword(email: String) {
        if (email.isEmpty()) {
            viewModelScope.launch { _events.emit(AuthEvent.Error("Please enter your email address")) }
            return
        }
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                viewModelScope.launch {
                    if (task.isSuccessful) {
                        _events.emit(AuthEvent.PasswordResetSent)
                    } else {
                        _events.emit(AuthEvent.Error("Reset failed: ${task.exception?.message}"))
                    }
                }
            }
    }
}
