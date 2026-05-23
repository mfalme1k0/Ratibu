package com.ik0ha.ratibu.viewmodel

import androidx.lifecycle.ViewModel
import com.ik0ha.ratibu.data.ChatChannel
import com.ik0ha.ratibu.data.ChatMessage
import com.ik0ha.ratibu.data.repository.ChatRepository
import com.ik0ha.ratibu.data.repository.UserRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.map

class ChatViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val chatRepository = ChatRepository()
    private val currentUserId = userRepository.getCurrentUserId() ?: ""

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _channels = MutableStateFlow<List<ChatChannel>>(emptyList())
    val channels: StateFlow<List<ChatChannel>> = _channels

    fun fetchMessages(otherUserId: String) {
        val channelId = getChannelId(currentUserId, otherUserId)
        chatRepository.getMessages(channelId)
            .map { list -> list.sortedBy { it.timestamp } }
            .onEach { _messages.value = it }
            .launchIn(viewModelScope)
    }

    fun sendMessage(otherUserId: String, text: String, otherUserName: String, currentUserName: String) {
        if (text.isEmpty()) return
        val channelId = getChannelId(currentUserId, otherUserId)
        val msgId = chatRepository.generateMessageKey(channelId) ?: return
        val timestamp = System.currentTimeMillis()
        
        val message = ChatMessage(msgId, currentUserId, text, timestamp)
        
        // Update channel info for both users
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
        chatRepository.getChannels(currentUserId)
            .map { list -> list.sortedByDescending { it.lastTimestamp } }
            .onEach { _channels.value = it }
            .launchIn(viewModelScope)
    }

    private fun getChannelId(id1: String, id2: String): String {
        return if (id1 < id2) "${id1}_${id2}" else "${id2}_${id1}"
    }
}
