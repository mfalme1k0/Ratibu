package com.ik0ha.ratibu.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ik0ha.ratibu.data.UserRole
import com.ik0ha.ratibu.viewmodel.AuthEvent
import com.ik0ha.ratibu.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    navController: NavHostController,
    onBackToLogin: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.CLIENT) }
    
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        authViewModel.events.collect { event ->
            when (event) {
                is AuthEvent.LoginSuccess -> {
                    navController.navigate("main_client") {
                        popUpTo("login") { inclusive = true }
                    }
                }
                is AuthEvent.ProviderLoginSuccess -> {
                    navController.navigate("main_provider") {
                        popUpTo("login") { inclusive = true }
                    }
                }
                is AuthEvent.PasswordResetSent -> {
                    Toast.makeText(context, "Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show()
                }
                is AuthEvent.AccountDeleted -> {
                    Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                }
                is AuthEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(all = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "REGISTER",
            fontSize = 32.sp,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            ),
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = "Name", tint = MaterialTheme.colorScheme.primary)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.primary)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password", tint = MaterialTheme.colorScheme.primary)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("I am a:", color = MaterialTheme.colorScheme.secondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = role == UserRole.CLIENT, onClick = { role = UserRole.CLIENT })
            Text("Client", color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.width(20.dp))
            RadioButton(selected = role == UserRole.PROVIDER, onClick = { role = UserRole.PROVIDER })
            Text("Service Provider", color = MaterialTheme.colorScheme.onBackground)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                authViewModel.register(email, password, name, role)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()
        ) {
            Text("CREATE ACCOUNT", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onBackToLogin) {
            Text(
                "Already have an account? Sign In",
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
