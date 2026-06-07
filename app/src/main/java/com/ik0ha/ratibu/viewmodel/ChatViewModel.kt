package com.ik0ha.ratibu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ik0ha.ratibu.data.ChatChannel
import com.ik0ha.ratibu.data.ChatMessage
import com.ik0ha.ratibu.data.NetworkResult
import com.ik0ha.ratibu.data.repository.ChatRepository
import com.ik0ha.ratibu.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ChatViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val chatRepository = ChatRepository()
    private val currentUserId = userRepository.getCurrentUserId() ?: ""

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _channels = MutableStateFlow<List<ChatChannel>>(emptyList())
    val channels: StateFlow<List<ChatChannel>> = _channels

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun fetchMessages(otherUserId: String) {
        chatRepository.getMessages(getChannelId(currentUserId, otherUserId))
            .onEach { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _messages.value = result.data.sortedBy { it.timestamp }
                        _isLoading.value = false
                    }
                    is NetworkResult.Error -> {
                        Log.e("ChatViewModel", "Error fetching messages: ${result.message}")
                        _isLoading.value = false
                    }
                    is NetworkResult.Loading -> _isLoading.value = true
                }
            }
            .catch { e -> Log.e("ChatViewModel", "Fatal error messages", e) }
            .launchIn(viewModelScope)
    }

    fun sendMessage(otherUserId: String, text: String, otherUserName: String, currentUserName: String) {
        if (text.isEmpty() || currentUserId.isEmpty()) return
        val channelId = getChannelId(currentUserId, otherUserId)
        val msgId = chatRepository.generateMessageKey(channelId) ?: return
        val timestamp = System.currentTimeMillis()
        
        val message = ChatMessage(msgId, currentUserId, text, timestamp)
        
        val channel = ChatChannel(
            id = channelId,
            clientId = if (currentUserId < otherUserId) currentUserId else otherUserId,
            providerId = if (currentUserId < otherUserId) otherUserId else currentUserId,
            clientName = if (currentUserId < otherUserId) currentUserName else otherUserName,
            providerName = if (currentUserId < otherUserId) otherUserName else currentUserName,
            lastMessage = text,
            lastTimestamp = timestamp
        )
        
        chatRepository.sendMessage(channelId, message, channel)
    }

    fun fetchChannels() {
        if (currentUserId.isEmpty()) return
        
        chatRepository.getChannels(currentUserId)
            .onEach { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _channels.value = result.data.sortedByDescending { it.lastTimestamp }
                        _isLoading.value = false
                    }
                    is NetworkResult.Error -> {
                        Log.e("ChatViewModel", "Error fetching channels: ${result.message}")
                        _isLoading.value = false
                    }
                    is NetworkResult.Loading -> _isLoading.value = true
                }
            }
            .catch { e -> Log.e("ChatViewModel", "Fatal error channels", e) }
            .launchIn(viewModelScope)
    }

    private fun getChannelId(id1: String, id2: String): String {
        return if (id1 < id2) "${id1}_${id2}" else "${id2}_${id1}"
    }
}
