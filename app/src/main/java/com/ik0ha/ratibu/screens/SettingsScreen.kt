package com.ik0ha.ratibu.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ik0ha.ratibu.MainActivity
import com.ik0ha.ratibu.viewmodel.AuthViewModel
import com.ik0ha.ratibu.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    currentViewRole: String? = null,
    mainViewModel: MainViewModel = viewModel(viewModelStoreOwner = LocalContext.current as MainActivity),
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: com.ik0ha.ratibu.viewmodel.HomeViewModel = viewModel()
) {
    val themePreference by mainViewModel.themePreference.collectAsState()
    val actualUserRole by homeViewModel.userRole.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account?") },
            text = { Text("This will permanently remove all your data. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { 
                        authViewModel.deleteAccount {
                            navController.navigate("login") { popUpTo(0) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "App Preferences",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme Mode", fontWeight = FontWeight.Bold)
                    Text("Choose how the app looks on your device", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = themePreference == null,
                            onClick = { mainViewModel.updateTheme(null) },
                            label = { Text("System") }
                        )
                        FilterChip(
                            selected = themePreference == false,
                            onClick = { mainViewModel.updateTheme(false) },
                            label = { Text("Light") }
                        )
                        FilterChip(
                            selected = themePreference == true,
                            onClick = { mainViewModel.updateTheme(true) },
                            label = { Text("Dark") }
                        )
                    }
                }
            }
            
            if (actualUserRole == com.ik0ha.ratibu.data.UserRole.PROVIDER && currentViewRole != com.ik0ha.ratibu.data.UserRole.PROVIDER) {
                Button(
                    onClick = { 
                        navController.navigate("main_provider") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Dashboard, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Switch to Provider View")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    authViewModel.logout {
                        navController.navigate("login") { popUpTo(0) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Logout")
            }

            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Permanently Delete Account", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
