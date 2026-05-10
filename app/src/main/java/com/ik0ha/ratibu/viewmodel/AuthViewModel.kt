package com.ik0ha.ratibu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ik0ha.ratibu.data.User
import com.ik0ha.ratibu.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

sealed class AuthEvent {
    object LoginSuccess : AuthEvent()
    object ProviderLoginSuccess : AuthEvent()
    data class Error(val message: String) : AuthEvent()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val repository = UserRepository()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events

    fun login(email: String, pass: String) {
        if (email.isEmpty() || pass.isEmpty()) {
            viewModelScope.launch { _events.emit(AuthEvent.Error("Please fill all fields")) }
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
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
        if (email.isEmpty() || pass.isEmpty() || name.isEmpty()) {
            viewModelScope.launch { _events.emit(AuthEvent.Error("Please fill all fields")) }
            return
        }

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    val user = User(uid, name, email, role)
                    
                    repository.saveUser(user) { success ->
                        if (success) {
                            if (role == "PROVIDER") {
                                repository.saveProviderData(uid, name) { providerSuccess ->
                                    viewModelScope.launch {
                                        if (providerSuccess) {
                                            _events.emit(AuthEvent.ProviderLoginSuccess)
                                        } else {
                                            _events.emit(AuthEvent.Error("Failed to save provider data"))
                                        }
                                    }
                                }
                            } else {
                                viewModelScope.launch { _events.emit(AuthEvent.LoginSuccess) }
                            }
                        } else {
                            viewModelScope.launch { _events.emit(AuthEvent.Error("Failed to save user data")) }
                        }
                    }
                } else {
                    viewModelScope.launch { _events.emit(AuthEvent.Error("Registration failed: ${task.exception?.message}")) }
                }
            }
    }

    fun logout(onComplete: () -> Unit) {
        auth.signOut()
        onComplete()
    }
}
