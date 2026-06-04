package com.ik0ha.ratibu.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ik0ha.ratibu.data.CacheManager
import com.ik0ha.ratibu.data.UserRole
import com.ik0ha.ratibu.data.repository.UserRepository
import com.ik0ha.ratibu.screens.*
import com.ik0ha.ratibu.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    // Client Screens
    object Home : Screen("home", "Explore", Icons.Default.Search)
    object MyBookings : Screen("my_bookings", "Bookings", Icons.Default.CalendarMonth)
    object Chats : Screen("chat_list", "Messages", Icons.Default.Chat)
    object Settings : Screen("settings", "Account", Icons.Default.Person)

    // Provider Screens
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Dashboard)
    object Today : Screen("today", "Schedule", Icons.Default.Today)
    object Analytics : Screen("analytics", "Insights", Icons.Default.BarChart)
    object ProviderProfile : Screen("provider_profile", "Account", Icons.Default.Person)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val userRepository = UserRepository()
    val cacheManager = remember { CacheManager(context) }
    
    val homeViewModel: HomeViewModel = viewModel()
    val userRole by homeViewModel.userRole.collectAsState()

    LaunchedEffect(Unit) {
        val currentUid = userRepository.getCurrentUserId()
        if (currentUid != null) {
            val cachedRole = cacheManager.getUserRole(currentUid)
            if (cachedRole != null) {
                val destination = if (cachedRole == UserRole.PROVIDER) "main_provider" else "main_client"
                navController.navigate(destination) {
                    popUpTo("login") { inclusive = true }
                }
            }

            userRepository.getUserRole(currentUid) { role ->
                if (role != null) {
                    cacheManager.saveUserRole(currentUid, role)
                    if (role != cachedRole) {
                        val destination = if (role == UserRole.PROVIDER) "main_provider" else "main_client"
                        navController.navigate(destination) {
                            popUpTo("login") { inclusive = true }
                        }
                    }
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
        
        composable("main_client") {
            MainContainer(role = UserRole.CLIENT, rootNavController = navController)
        }
        
        composable("main_provider") {
            MainContainer(role = UserRole.PROVIDER, rootNavController = navController)
        }

        // Deep links or screens outside the bottom bar
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

@Composable
fun MainContainer(role: String, rootNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    
    val items = if (role == UserRole.PROVIDER) {
        listOf(Screen.Dashboard, Screen.Today, Screen.Chats, Screen.Analytics, Screen.ProviderProfile)
    } else {
        listOf(Screen.Home, Screen.MyBookings, Screen.Chats, Screen.Settings)
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title, fontSize = 10.sp) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            bottomNavController.navigate(screen.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = items[0].route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController = rootNavController) }
            composable(Screen.MyBookings.route) { MyBookingsScreen(navController = rootNavController) }
            composable(Screen.Chats.route) { ChatListScreen(navController = rootNavController) }
            composable(Screen.Settings.route) { 
                SettingsScreen(
                    navController = rootNavController,
                    currentViewRole = role
                ) 
            }
            
            composable(Screen.Dashboard.route) { 
                DashboardScreen(
                    navController = bottomNavController,
                    rootNavController = rootNavController
                ) 
            }
            composable(Screen.Today.route) { TodayScreen(navController = rootNavController) }
            composable(Screen.Analytics.route) { AnalyticsScreen(navController = bottomNavController) }
            composable(Screen.ProviderProfile.route) { ProviderProfileScreen(navController = rootNavController) }
            
            // Shared Settings route accessible by both
            composable("settings") { 
                SettingsScreen(
                    navController = rootNavController,
                    currentViewRole = role
                ) 
            }
        }
    }
}
