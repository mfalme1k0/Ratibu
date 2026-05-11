package com.ik0ha.ratibu.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ik0ha.ratibu.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val todayBookings by dashboardViewModel.todayBookings.collectAsState()
    val profile by dashboardViewModel.providerProfile.collectAsState()
    
    val dateStr = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Itinerary") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = dateStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Working Hours: ${profile?.workStartHour ?: 9}:00 AM - ${if ((profile?.workEndHour ?: 18) > 12) (profile?.workEndHour ?: 18) - 12 else (profile?.workEndHour ?: 18)}:00 ${if ((profile?.workEndHour ?: 18) >= 12) "PM" else "AM"}",
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Schedule",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (todayBookings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                            Text("No appointments for today", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            } else {
                items(todayBookings) { booking ->
                    BookingItem(
                        booking = booking,
                        onStatusUpdate = { newStatus ->
                            dashboardViewModel.updateBookingStatus(booking.id, newStatus)
                        }
                    )
                }
            }
        }
    }
}
