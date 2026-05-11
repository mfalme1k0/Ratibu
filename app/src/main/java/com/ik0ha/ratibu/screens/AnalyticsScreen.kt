package com.ik0ha.ratibu.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ik0ha.ratibu.data.Session
import com.ik0ha.ratibu.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val detailedAnalytics by dashboardViewModel.detailedAnalytics.collectAsState()
    val completedBookings by dashboardViewModel.completedBookings.collectAsState()
    
    var showCompletedSessions by remember { mutableStateOf(false) }

    if (showCompletedSessions) {
        CompletedSessionsDialog(
            sessions = completedBookings,
            onDismiss = { showCompletedSessions = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Insights") },
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
        if (detailedAnalytics == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val data = detailedAnalytics!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                contentPadding = PaddingValues(vertical = 24.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { showCompletedSessions = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("History", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("Completed Sessions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    AnalyticsSection(
                        title = "Client Retention",
                        icon = Icons.Default.TrendingUp
                    ) {
                        RetentionCard(data.uniqueClients, data.repeatClients)
                    }
                }

                item {
                    AnalyticsSection(
                        title = "Daily Volume",
                        icon = Icons.Default.BarChart
                    ) {
                        DistributionChart(data.dayDistribution, "Sessions per day")
                    }
                }

                item {
                    AnalyticsSection(
                        title = "Peak Hours",
                        icon = Icons.Default.PieChart
                    ) {
                        DistributionChart(data.hourDistribution, "Sessions per hour")
                    }
                }

                item {
                    AnalyticsSection(
                        title = "Booking Success",
                        icon = Icons.Default.TrendingUp
                    ) {
                        StatusBreakdown(data.statusBreakdown)
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun RetentionCard(unique: Int, repeat: Int) {
    val rate = if (unique > 0) (repeat * 100 / unique) else 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Repeat Client Rate", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("$rate%", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Total: $unique", 
                        fontSize = 12.sp, 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Returning: $repeat", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun DistributionChart(distribution: Map<String, Int>, label: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (distribution.isEmpty()) {
                Text("No data available yet", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            } else {
                val max = distribution.values.maxOrNull() ?: 1
                distribution.forEach { (key, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(key, modifier = Modifier.width(80.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        LinearProgressIndicator(
                            progress = { count.toFloat() / max },
                            modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("$count", modifier = Modifier.width(40.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBreakdown(statusMap: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            statusMap.forEach { (status, count) ->
                val color = when(status) {
                    "COMPLETED" -> Color(0xFF4CAF50)
                    "CANCELLED" -> Color(0xFFF44336)
                    "PENDING" -> Color(0xFFFFC107)
                    else -> MaterialTheme.colorScheme.secondary
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(status, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("$count", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
                }
            }
        }
    }
}

@Composable
fun CompletedSessionsDialog(sessions: List<Session>, onDismiss: () -> Unit) {
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    val filteredSessions = if (selectedDate == null) {
        sessions
    } else {
        sessions.filter { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
            cal.get(Calendar.YEAR) == selectedDate!!.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == selectedDate!!.get(Calendar.DAY_OF_YEAR)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Session History") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDate == null) "All Completed" else dateFormat.format(selectedDate!!.time),
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { 
                        if (selectedDate == null) {
                            selectedDate = Calendar.getInstance() 
                        } else {
                            selectedDate = null
                        }
                    }) {
                        Text(if (selectedDate == null) "Filter Date" else "Show All")
                    }
                }
                
                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredSessions.isEmpty()) {
                        item { Text("No sessions found", color = MaterialTheme.colorScheme.secondary) }
                    } else {
                        items(filteredSessions) { session ->
                            SessionHistoryItem(session)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun SessionHistoryItem(session: Session) {
    val timeFormat = SimpleDateFormat("MMM dd - hh:mm a", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(session.clientName, fontWeight = FontWeight.Bold)
            Text(timeFormat.format(Date(session.startTime)), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            if (session.notes.isNotEmpty()) {
                Text("Note: ${session.notes}", fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
