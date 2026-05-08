package com.ik0ha.ratibu.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ik0ha.ratibu.data.ChatChannel
import com.ik0ha.ratibu.data.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
    private val currentUserId = auth.currentUser?.uid ?: ""

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _channels = MutableStateFlow<List<ChatChannel>>(emptyList())
    val channels: StateFlow<List<ChatChannel>> = _channels

    fun fetchMessages(otherUserId: String) {
        val channelId = getChannelId(currentUserId, otherUserId)
        db.child("chats").child(channelId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    child.getValue(ChatMessage::class.java)?.let { list.add(it) }
                }
                _messages.value = list.sortedBy { it.timestamp }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun sendMessage(otherUserId: String, text: String, otherUserName: String, currentUserName: String) {
        if (text.isEmpty()) return
        val channelId = getChannelId(currentUserId, otherUserId)
        val msgId = db.child("chats").child(channelId).push().key ?: return
        val timestamp = System.currentTimeMillis()
        
        val message = ChatMessage(msgId, currentUserId, text, timestamp)
        
        db.child("chats").child(channelId).child(msgId).setValue(message)
        
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
        
        db.child("channels").child(currentUserId).child(channelId).setValue(channel)
        db.child("channels").child(otherUserId).child(channelId).setValue(channel)
    }

    fun fetchChannels() {
        db.child("channels").child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatChannel>()
                for (child in snapshot.children) {
                    child.getValue(ChatChannel::class.java)?.let { list.add(it) }
                }
                _channels.value = list.sortedByDescending { it.lastTimestamp }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun getChannelId(id1: String, id2: String): String {
        return if (id1 < id2) "${id1}_${id2}" else "${id2}_${id1}"
    }
}
