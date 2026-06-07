package com.ik0ha.ratibu.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ik0ha.ratibu.R
import com.ik0ha.ratibu.data.Session
import com.ik0ha.ratibu.viewmodel.AuthViewModel
import com.ik0ha.ratibu.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    rootNavController: NavHostController? = null,
    dashboardViewModel: DashboardViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val upcomingBookings by dashboardViewModel.upcomingBookings.collectAsState()
    val todayBookings by dashboardViewModel.todayBookings.collectAsState()
    val analytics by dashboardViewModel.analytics.collectAsState()
    val profile by dashboardViewModel.providerProfile.collectAsState()
    val isLoading by dashboardViewModel.isLoading.collectAsState()
    
    var showWalkInDialog by remember { mutableStateOf(false) }
    var showBlockTimeDialog by remember { mutableStateOf(false) }
    var selectedAnalytic by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (selectedAnalytic != null) {
        val (label, value) = selectedAnalytic!!
        val description = when(label) {
            "Today's Schedule" -> "You have $value appointments scheduled for today. High-demand days are a great opportunity to showcase your best work!"
            "Rating" -> "Your current average rating is $value. Maintaining a high rating helps you appear higher in client searches."
            "Busiest Day" -> "Statistics show $value is your most booked day. You might want to ensure you're fully prepared for the extra volume then."
            "Peak Hour" -> "Your most popular booking time is $value. This is when most clients are looking for your services."
            "Completion Rate" -> "Your completion rate is $value. High completion rates demonstrate reliability and encourage repeat bookings."
            else -> "Your current $label performance is $value. This data is calculated from your activity over the last 30 days."
        }
        AlertDialog(
            onDismissRequest = { selectedAnalytic = null },
            title = { Text(label) },
            text = { Text(description) },
            confirmButton = {
                TextButton(onClick = { selectedAnalytic = null }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.provider_dashboard)) },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { showBlockTimeDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Block, contentDescription = "Block Time")
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = { showWalkInDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Walk-in")
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (showWalkInDialog) {
                WalkInDialog(
                    onDismiss = { showWalkInDialog = false },
                    onConfirm = { name, time, notes ->
                        dashboardViewModel.addWalkIn(name, time, notes)
                        showWalkInDialog = false
                    }
                )
            }
            
            if (showBlockTimeDialog) {
                BlockTimeDialog(
                    onDismiss = { showBlockTimeDialog = false },
                    onConfirm = { time, duration, reason ->
                        dashboardViewModel.blockTime(time, duration, reason)
                        showBlockTimeDialog = false
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Analytics / Summary Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Overview",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { navController.navigate("analytics") }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.detailed_insights))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatCard(
                                label = "Today's Schedule",
                                value = todayBookings.size.toString(),
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate("today") }
                            )
                            StatCard(
                                label = "Rating",
                                value = profile?.rating.toString(),
                                modifier = Modifier.weight(1f),
                                onClick = { selectedAnalytic = "Rating" to "${profile?.rating} stars average" }
                            )
                        }
                        
                        if (analytics.isNotEmpty()) {
                            val items = analytics.toList()
                            items.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    rowItems.forEach { (label, value) ->
                                        StatCard(
                                            label = label,
                                            value = value,
                                            modifier = Modifier.weight(1f),
                                            onClick = { selectedAnalytic = label to value }
                                        )
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.upcoming_sessions),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (upcomingBookings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.no_upcoming_sessions), color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                } else {
                    items(upcomingBookings) { booking ->
                        BookingItem(
                            booking = booking,
                            onStatusUpdate = { newStatus ->
                                dashboardViewModel.updateBookingStatus(booking.id, newStatus)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            (rootNavController ?: navController).navigate("main_client") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(stringResource(R.string.switch_to_client_view))
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun BookingItem(booking: Session, onStatusUpdate: (String) -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val dateString = dateFormat.format(Date(booking.startTime))

    val backgroundColor = when (booking.type) {
        "BLOCKED" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        "WALK_IN" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val title = when (booking.type) {
                        "BLOCKED" -> "BLOCKED: ${booking.notes}"
                        "WALK_IN" -> "Walk-in: ${booking.clientName}"
                        else -> "Client: ${booking.clientName}"
                    }
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(dateString, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                }
                StatusBadge(status = booking.status)
            }
            
            if (booking.notes.isNotEmpty() && booking.type != "BLOCKED") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${booking.notes}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
            
            if (booking.type != "BLOCKED") {
                if (booking.status == "PENDING") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onStatusUpdate("CONFIRMED") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text(stringResource(R.string.confirm))
                        }
                        OutlinedButton(
                            onClick = { onStatusUpdate("CANCELLED") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.decline), color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else if (booking.status == "CONFIRMED") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onStatusUpdate("COMPLETED") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.mark_completed))
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onStatusUpdate("CANCELLED") }) {
                    Text(stringResource(R.string.unblock), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun WalkInDialog(onDismiss: () -> Unit, onConfirm: (String, Long, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    
    var selectedDateTime by remember { mutableStateOf(calendar.timeInMillis) }
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            TimePickerDialog(context, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                selectedDateTime = calendar.timeInMillis
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_walk_in)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.client_name)) })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.service_notes)) })
                
                Button(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text(stringResource(R.string.time_label, dateFormat.format(Date(selectedDateTime))))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, selectedDateTime, notes) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BlockTimeDialog(onDismiss: () -> Unit, onConfirm: (Long, Int, String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    
    var selectedDateTime by remember { mutableStateOf(calendar.timeInMillis) }
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            TimePickerDialog(context, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                selectedDateTime = calendar.timeInMillis
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Block Time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason (e.g. Lunch)") })
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (minutes)") })
                
                Button(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text("Starts at: ${dateFormat.format(Date(selectedDateTime))}")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedDateTime, duration.toIntOrNull() ?: 30, reason) }) {
                Text("Block")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "CONFIRMED" -> Color(0xFF4CAF50)
        "PENDING" -> Color(0xFFFFC107)
        "CANCELLED" -> Color(0xFFF44336)
        "COMPLETED" -> Color(0xFF2196F3)
        else -> MaterialTheme.colorScheme.secondary
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
