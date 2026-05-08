package com.ik0ha.ratibu.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

import androidx.compose.ui.platform.LocalContext
import com.ik0ha.ratibu.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(navController: NavHostController, providerId: String) {
    val context = LocalContext.current
    val bookingViewModel = remember { BookingViewModel(context) }
    
    val bookedRanges by bookingViewModel.bookedRanges.collectAsState()
    
    val days = remember {
        (0..6).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, i)
            cal
        }
    }
    
    var selectedDay by remember { mutableStateOf(days[0]) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    
    val timeSlots = listOf("09:00 AM", "10:00 AM", "11:00 AM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM")

    LaunchedEffect(providerId) {
        bookingViewModel.fetchBookedSlots(providerId)
    }

    fun getTimestamp(day: Calendar, timeStr: String): Long {
        val timeCal = Calendar.getInstance()
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val date = format.parse(timeStr) ?: return 0L
        timeCal.time = date
        
        val result = day.clone() as Calendar
        result.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
        result.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
        result.set(Calendar.SECOND, 0)
        result.set(Calendar.MILLISECOND, 0)
        return result.timeInMillis
    }

    fun isSlotOccupied(startTime: Long): Boolean {
        return bookedRanges.any { range -> 
            startTime in range
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Session") },
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Select Date",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.forEach { day ->
                    val isSelected = selectedDay.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
                    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(day.time)
                    val dateNum = day.get(Calendar.DAY_OF_MONTH).toString()
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                            .clickable { 
                                selectedDay = day 
                                selectedTime = null // Reset time when day changes
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = dayName,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = dateNum,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Available Slots",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val chunks = timeSlots.chunked(3)
                chunks.forEach { rowSlots ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowSlots.forEach { time ->
                            val timestamp = getTimestamp(selectedDay, time)
                            val isBooked = isSlotOccupied(timestamp)
                            
                            var showWaitlistDialog by remember { mutableStateOf(false) }

                            if (showWaitlistDialog) {
                                AlertDialog(
                                    onDismissRequest = { showWaitlistDialog = false },
                                    title = { Text("Slot Unavailable") },
                                    text = { Text("This slot is already booked. Would you like to join the waitlist?") },
                                    confirmButton = {
                                        Button(onClick = { 
                                            bookingViewModel.joinWaitlist(providerId, timestamp)
                                            showWaitlistDialog = false
                                        }) { Text("Join Waitlist") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showWaitlistDialog = false }) { Text("Cancel") }
                                    }
                                )
                            }

                            TimeSlotCard(
                                time = time,
                                isSelected = selectedTime == time,
                                isBooked = isBooked,
                                modifier = Modifier.weight(1f),
                                onClick = { 
                                    if (!isBooked) {
                                        selectedTime = time 
                                    } else {
                                        showWaitlistDialog = true
                                    }
                                }
                            )
                        }
                        if (rowSlots.size < 3) {
                            repeat(3 - rowSlots.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Add Notes (Style preferences, etc.)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Reminder",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            var reminderMinutes by remember { mutableStateOf(30f) }
            Slider(
                value = reminderMinutes,
                onValueChange = { reminderMinutes = it },
                valueRange = 10f..120f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "Notify me ${reminderMinutes.toInt()} minutes before",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    selectedTime?.let { time ->
                        val startTime = getTimestamp(selectedDay, time)
                        bookingViewModel.bookSession(providerId, "Service Provider", startTime, reminderMinutes.toInt(), notes)
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedTime != null,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("CONFIRM BOOKING", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TimeSlotCard(time: String, isSelected: Boolean, isBooked: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val backgroundColor = when {
        isBooked -> Color(0xFFF44336) // Red
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color(0xFF4CAF50) // Green
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor.copy(alpha = if (isSelected || isBooked) 1f else 0.1f))
            .clickable(enabled = !isBooked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = time,
            color = if (isSelected || isBooked) Color.White else backgroundColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textDecoration = if (isBooked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
        )
    }
}
