package com.ik0ha.ratibu.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.ik0ha.ratibu.R
import com.ik0ha.ratibu.data.Review
import com.ik0ha.ratibu.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailScreen(
    navController: NavHostController,
    providerId: String,
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val providers by homeViewModel.providers.collectAsState()
    val provider = providers.find { it.uid == providerId }
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }
    val isOwnProfile = currentUserId == providerId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(provider?.name ?: "Provider Details") },
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
        if (provider == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ... (rest of the profile sections)
                // Profile Header
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = if (provider.imageUrl.isNotEmpty()) provider.imageUrl else R.drawable.company_logo,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = provider.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = provider.category,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (provider.location.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = provider.location,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${provider.rating} Rating",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Bio
                item {
                    Column {
                        Text(
                            text = "About",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = provider.bio.ifEmpty { "No bio available." },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Work Samples
                item {
                    Column {
                        Text(
                            text = "Work Samples",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (provider.workSamples.isEmpty()) {
                            Text("No work samples available.", color = MaterialTheme.colorScheme.secondary)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(provider.workSamples) { sample ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AsyncImage(
                                            model = sample.imageUrl,
                                            contentDescription = sample.description,
                                            modifier = Modifier
                                                .size(150.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (sample.description.isNotEmpty()) {
                                            Text(
                                                text = sample.description,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(top = 4.dp).width(150.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Reviews
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reviews",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!isOwnProfile) {
                            var showReviewDialog by remember { mutableStateOf(false) }
                            TextButton(onClick = { showReviewDialog = true }) {
                                Text("Add Review")
                            }
                            
                            if (showReviewDialog) {
                                AddReviewDialog(
                                    onDismiss = { showReviewDialog = false },
                                    onConfirm = { rating, comment ->
                                        homeViewModel.submitReview(providerId, rating, comment)
                                        showReviewDialog = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (provider.reviews.isEmpty()) {
                    item {
                        Text("No reviews yet.", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    val displayedReviews = provider.reviews.sortedByDescending { it.timestamp }.take(3)
                    items(displayedReviews) { review ->
                        ReviewCard(review = review)
                    }
                    
                    if (provider.reviews.size > 3) {
                        item {
                            var showAllReviews by remember { mutableStateOf(false) }
                            TextButton(
                                onClick = { showAllReviews = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("See all ${provider.reviews.size} reviews")
                            }

                            if (showAllReviews) {
                                AlertDialog(
                                    onDismissRequest = { showAllReviews = false },
                                    title = { Text("All Reviews") },
                                    text = {
                                        LazyColumn(
                                            modifier = Modifier.heightIn(max = 400.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(provider.reviews.sortedByDescending { it.timestamp }) { review ->
                                                ReviewCard(review = review)
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showAllReviews = false }) { Text("Close") }
                                    }
                                )
                            }
                        }
                    }
                }

                // Action Buttons (Call & Chat)
                if (!isOwnProfile) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Call Button - Only if phone exists
                            if (provider.phoneNumber.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phoneNumber}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Call")
                                }
                            }
                            
                            // Chat Button - Always available
                            Button(
                                onClick = {
                                    navController.navigate("chat/${provider.uid}")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chat")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Get Directions Button
                        if (provider.latitude != 0.0 && provider.longitude != 0.0) {
                            Button(
                                onClick = {
                                    val gmmIntentUri = Uri.parse("google.navigation:q=${provider.latitude},${provider.longitude}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    context.startActivity(mapIntent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Get Directions")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Button(
                            onClick = { navController.navigate("booking/${provider.uid}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("BOOK NOW", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddReviewDialog(onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var rating by remember { mutableStateOf(5f) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leave a Review") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Rating: ${rating.toInt()} / 5", fontWeight = FontWeight.Bold)
                    Slider(
                        value = rating,
                        onValueChange = { rating = it },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Your comment") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(rating.toDouble(), comment) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReviewCard(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.reviewerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = review.rating.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = review.comment,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
