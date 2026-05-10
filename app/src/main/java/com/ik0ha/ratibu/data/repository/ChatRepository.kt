package com.ik0ha.ratibu.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ik0ha.ratibu.data.ChatChannel
import com.ik0ha.ratibu.data.ChatMessage

class ChatRepository {
    private val db = FirebaseDatabase.getInstance().reference

    fun getMessages(channelId: String, onResult: (List<ChatMessage>) -> Unit) {
        db.child("chats").child(channelId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    child.getValue(ChatMessage::class.java)?.let { list.add(it) }
                }
                onResult(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun sendMessage(channelId: String, message: ChatMessage, channel: ChatChannel) {
        db.child("chats").child(channelId).child(message.id).setValue(message)
        
        db.child("channels").child(channel.clientId).child(channelId).setValue(channel)
        db.child("channels").child(channel.providerId).child(channelId).setValue(channel)
    }

    fun getChannels(userId: String, onResult: (List<ChatChannel>) -> Unit) {
        db.child("channels").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatChannel>()
                for (child in snapshot.children) {
                    child.getValue(ChatChannel::class.java)?.let { list.add(it) }
                }
                onResult(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun generateMessageKey(channelId: String): String? = db.child("chats").child(channelId).push().key
}
