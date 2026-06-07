package com.ik0ha.ratibu.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ik0ha.ratibu.data.ChatChannel
import com.ik0ha.ratibu.data.ChatMessage
import com.ik0ha.ratibu.data.NetworkResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

class ChatRepository {
    private val db = FirebaseDatabase.getInstance().reference

    fun getMessages(channelId: String): Flow<NetworkResult<List<ChatMessage>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    child.getValue(ChatMessage::class.java)?.let { list.add(it) }
                }
                trySend(NetworkResult.Success(list))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error(error.message))
            }
        }
        val ref = db.child("chats").child(channelId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.onStart { emit(NetworkResult.Loading) }

    fun sendMessage(channelId: String, message: ChatMessage, channel: ChatChannel) {
        db.child("chats").child(channelId).child(message.id).setValue(message)
        
        db.child("channels").child(channel.clientId).child(channelId).setValue(channel)
        db.child("channels").child(channel.providerId).child(channelId).setValue(channel)
    }

    fun getChannels(userId: String): Flow<NetworkResult<List<ChatChannel>>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(NetworkResult.Error("User ID is empty"))
            return@callbackFlow
        }
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatChannel>()
                for (child in snapshot.children) {
                    child.getValue(ChatChannel::class.java)?.let { list.add(it) }
                }
                trySend(NetworkResult.Success(list))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error(error.message))
            }
        }
        val ref = db.child("channels").child(userId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.onStart { emit(NetworkResult.Loading) }

    fun generateMessageKey(channelId: String): String? = db.child("chats").child(channelId).push().key
}
