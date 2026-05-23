package com.ik0ha.ratibu.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ik0ha.ratibu.data.UserRole
import com.ik0ha.ratibu.data.repository.UserRepository
import com.ik0ha.ratibu.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val userRepository = UserRepository()

    LaunchedEffect(Unit) {
        val currentUid = userRepository.getCurrentUserId()
        if (currentUid != null) {
            userRepository.getUserRole(currentUid) { role ->
                val destination = if (role == UserRole.PROVIDER) "dashboard" else "home"
                navController.navigate(destination) {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                navController = navController,
                onRegisterClick = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegistrationScreen(
                navController = navController,
                onBackToLogin = { navController.popBackStack() }
            )
        }
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("my_bookings") {
            MyBookingsScreen(navController = navController)
        }
        composable("dashboard") {
            DashboardScreen(navController = navController)
        }
        composable("today") {
            TodayScreen(navController = navController)
        }
        composable("analytics") {
            AnalyticsScreen(navController = navController)
        }
        composable("provider_profile") {
            ProviderProfileScreen(navController = navController)
        }
        composable("chat_list") {
            ChatListScreen(navController = navController)
        }
        composable("chat/{otherUserId}") { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            ChatScreen(navController = navController, otherUserId = otherUserId)
        }
        composable("provider_detail/{providerId}") { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
            ProviderDetailScreen(navController = navController, providerId = providerId)
        }
        composable("booking/{providerId}") { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
            BookingScreen(navController = navController, providerId = providerId)
        }
    }
}
