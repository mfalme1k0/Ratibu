package com.ik0ha.ratibu

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.ik0ha.ratibu.data.CloudinaryHelper
import com.ik0ha.ratibu.data.MyFirebaseMessagingService
import com.ik0ha.ratibu.navigation.AppNavigation
import com.ik0ha.ratibu.ui.theme.RatibuAppTheme
import com.ik0ha.ratibu.viewmodel.ChatViewModel
import com.ik0ha.ratibu.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        askNotificationPermission()
        updateFcmToken()

        CloudinaryHelper.init(this)
        enableEdgeToEdge()
        setContent {
            val themePreference by mainViewModel.themePreference.collectAsState()
            val context = LocalContext.current
            
            val channels by chatViewModel.channels.collectAsState()
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            
            LaunchedEffect(Unit) {
                chatViewModel.fetchChannels()
            }

            // Notification Observer with last notified timestamp to prevent duplicates
            val lastNotifiedTimestamp = androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }
            
            LaunchedEffect(channels) {
                if (currentUserId != null && channels.isNotEmpty()) {
                    val latestChannel = channels.maxByOrNull { it.lastTimestamp }
                    if (latestChannel != null && 
                        latestChannel.lastTimestamp > lastNotifiedTimestamp.longValue &&
                        latestChannel.lastTimestamp > System.currentTimeMillis() - 15000) {
                        
                        lastNotifiedTimestamp.longValue = latestChannel.lastTimestamp
                        
                        val senderName = if (latestChannel.clientId == currentUserId) 
                            latestChannel.providerName else latestChannel.clientName
                        
                        MyFirebaseMessagingService.showNotification(
                            context,
                            "New message from $senderName",
                            latestChannel.lastMessage
                        )
                    }
                }
            }

            RatibuAppTheme(themePreference = themePreference) {
                AppNavigation()
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    FirebaseDatabase.getInstance().reference.child("users").child(uid).child("fcmToken").setValue(token)
                }
            }
        }
    }
}

fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
