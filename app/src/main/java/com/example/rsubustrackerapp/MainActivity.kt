package com.example.rsubustrackerapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.*

// 1. Ensure this extends ComponentActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // STATE: Keeps track of which screen we are on
                var isLoggedIn by remember { mutableStateOf(false) }

                if (isLoggedIn) {
                    TrackerScreen(onBackClick = { isLoggedIn = false })
                } else {
                    LoginScreen(onLoginSuccess = { isLoggedIn = true })
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var busId by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Colors
    val gradientTop = Color(0xFFC85D8D)
    val gradientBottom = Color(0xFF6CAADC)
    val buttonColor = Color(0xFFC43C62)
    val primaryBlue = Color(0xFF5D8ADE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(gradientTop, gradientBottom)
                )
            )
    ) {
        // Top Pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp),
            shape = RoundedCornerShape(50.dp),
            color = Color.White
        ) {
            Text(
                text = "Bus Tracker System",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        // Login Card
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter the Bus ID",
                    fontSize = 25.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = busId,
                    onValueChange = { busId = it },
                    label = { Text("Bus ID") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Home, // Or Icons.Default.Info
                            contentDescription = "Bus Icon",
                            tint = Color.Gray
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = primaryBlue,
                        unfocusedBorderColor = primaryBlue,
                        focusedLabelColor = primaryBlue,
                        unfocusedLabelColor = primaryBlue,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (busId.isNotBlank()) {
                            // Call the function to switch screens!
                            onLoginSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .width(150.dp)
                ) {
                    Text(text = "LOGIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Footer
        Text(
            text = "Made in Rangsit University",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 24.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The Icon Circle
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF455A64), // Dark Grey
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // The Text Column
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.Black
            )
            Text(
                text = value,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
    // The Divider Line
    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)}

@Composable
fun TrackerScreen(onBackClick: () -> Unit) {
    // Colors from your design
    val gradientTop = Color(0xFFC85D8D)
    val gradientBottom = Color(0xFF6CAADC)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(gradientTop, gradientBottom)
                )
            )
    ) {
        // We use a Box with padding to create the "Layered" effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp) // Leave space at top for background
        ) {
            // 1. The Main White Card (The Body)
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp), // Push it down slightly so the "Tab" sticks out
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 60.dp, start = 24.dp, end = 24.dp) // Content padding
                        .fillMaxSize()
                ) {
                    // Two Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { /* TODO: Start GPS */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3E5F5)), // Light Purple
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Text("Activate GPS", color = Color(0xFF6A1B9A))
                        }

                        Button(
                            onClick = { /* TODO: Stop GPS */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3E5F5)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        ) {
                            Text("Deactivate GPS", color = Color(0xFF6A1B9A))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // The Stats List (Scrollable)
                    // Note: Using standard icons to prevent errors.
                    InfoRow(Icons.Default.DirectionsBus, "Station", "Building 1")
                    InfoRow(Icons.Default.Navigation, "Heading Direction", "-0.0")
                    InfoRow(Icons.Default.LocationSearching, "Accuracy", "5.0")
                    InfoRow(Icons.Default.Schedule, "Last Update", "2025-11-08 18:32:33")
                    InfoRow(Icons.Default.Info, "Ongoing Speed", "120 km/h") // Placeholder icon
                    InfoRow(Icons.Default.Place, "Latitude", "13.9669517")
                    InfoRow(Icons.Default.Place, "Longitude", "100.5834")
                    InfoRow(Icons.Default.DateRange, "Time Elapsed", "290")
                }
            }

            // 2. The "Tracker Menu" Tab (Floating on top left)
            Surface(
                modifier = Modifier
                    .padding(start = 24.dp), // Align left
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.Black),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "Tracker Menu",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

        }

        IconButton(
            onClick = onBackClick, // Call the function when clicked
            modifier = Modifier
                .align(Alignment.TopStart) // Stick to top left
                .padding(top = 40.dp, start = 16.dp) // Leave space for Status Bar
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White, // White icon to look good on the gradient
                modifier = Modifier.size(32.dp) // Make it slightly larger
            )
        }
    }
}