package com.ik0ha.ratibu.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ik0ha.ratibu.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val detailedAnalytics by dashboardViewModel.detailedAnalytics.collectAsState()

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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Repeat Client Rate", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                Text("$rate%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Total Clients: $unique", fontSize = 12.sp)
                Text("Returning: $repeat", fontSize = 12.sp)
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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(key, modifier = Modifier.width(60.dp), fontSize = 12.sp)
                        LinearProgressIndicator(
                            progress = { count.toFloat() / max },
                            modifier = Modifier.weight(1f).height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                        Text("$count", modifier = Modifier.width(30.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
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
        Column(modifier = Modifier.padding(16.dp)) {
            statusMap.forEach { (status, count) ->
                val color = when(status) {
                    "COMPLETED" -> Color(0xFF4CAF50)
                    "CANCELLED" -> Color(0xFFF44336)
                    "PENDING" -> Color(0xFFFFC107)
                    else -> MaterialTheme.colorScheme.secondary
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(status, fontSize = 14.sp)
                    Text("$count", fontWeight = FontWeight.Bold, color = color)
                }
            }
        }
    }
}
