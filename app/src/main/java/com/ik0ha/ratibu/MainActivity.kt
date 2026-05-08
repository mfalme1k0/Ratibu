package com.ik0ha.ratibu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ik0ha.ratibu.data.CloudinaryHelper
import com.ik0ha.ratibu.navigation.AppNavigation
import com.ik0ha.ratibu.ui.theme.RatibuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        CloudinaryHelper.init(this)
        enableEdgeToEdge()
        setContent {
            RatibuTheme {
                AppNavigation()
            }
        }
    }
}
