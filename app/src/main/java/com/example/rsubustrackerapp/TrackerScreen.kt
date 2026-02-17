package com.example.rsubustrackerapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun TrackerScreen(onBackClick: () -> Unit) {
    // Background Colors
    val gradientTop = Color(0xFFC85D8D)
    val gradientBottom = Color(0xFF6CAADC)

    // Context & Prefs
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("BusTrackerPrefs", Context.MODE_PRIVATE)
    val vehicleName = sharedPref.getString("CURRENT_VEHICLE_NAME", "Bus") ?: "Bus"
    val vehicleId = sharedPref.getString("CURRENT_VEHICLE_ID", "") ?: ""

    // Live data states
    var stopsList by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var currentStation by remember { mutableStateOf("En Route") }
    var isTracking by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf("0.0") }
    var longitude by remember { mutableStateOf("0.0") }
    var speed by remember { mutableStateOf("0 km/h") }
    var bearing by remember { mutableStateOf("0.0°") }
    var accuracy by remember { mutableStateOf("0 m") }
    var lastUpdate by remember { mutableStateOf("-") }
    var timeElapsed by remember { mutableStateOf("0 s") }

    // Timer state
    var timeElapsedSeconds by remember { mutableLongStateOf(0L) }

    // Timer logic
    LaunchedEffect(isTracking) {
        if (isTracking) {
            val startTime = System.currentTimeMillis() - (timeElapsedSeconds * 1000)
            while(true) {
                // Update timer every second
                val currentTime = System.currentTimeMillis()
                timeElapsedSeconds = (currentTime - startTime) / 1000
                delay(1000L)
            }
        }
    }

    // Fetch Stops
    LaunchedEffect(Unit) {
        RetrofitClient.instance.getStops().enqueue(object : Callback<List<Stop>> {
            override fun onResponse(call: Call<List<Stop>>, response: Response<List<Stop>>) {
                if (response.isSuccessful) {
                    stopsList = response.body() ?: emptyList()
                    println("Loaded ${stopsList.size} stops")
                }
            }
            override fun onFailure(call: Call<List<Stop>>, t: Throwable) {
                println("Failed to load stops: ${t.message}")
            }
        })
    }

    // Stops Helper
    fun checkNearestStop(currentLat: Double, currentLng: Double) {
        val detectionRadius = 50.0 // meters (Threshold)
        var foundStop = "En Route" // Default if no stop matches

        for (stop in stopsList) {
            val results = FloatArray(1)
            // Android's built-in math to calculate distance in meters
            Location.distanceBetween(currentLat, currentLng, stop.lat, stop.lng, results)
            val distanceInMeters = results[0]

            if (distanceInMeters <= detectionRadius) {
                foundStop = stop.nameEn // Or stop.nameTh
                break // Found one, stop searching
            }
        }
        currentStation = foundStop
    }

    // Backend Helper
    fun setVehicleStatus(status: String) {
        if (vehicleId.isNotBlank()) {
            val request = StatusRequest(status = status)
            RetrofitClient.instance.updateStatus(vehicleId, request).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        println("Status updated to: $status")
                    } else {
                        println("Failed to update status")
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    println("Error: ${t.message}")
                }
            })
        }
    }

    // Helper to format seconds -> HH:MM:SS
    fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    // Engine (LocationClient)
    val locationClient = remember { LocationClient(context) }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            // Permission Granted: Start the Engine immediately
            isTracking = true
            locationClient.startLocationUpdates { location ->
                // Update UI variables
                latitude = location.latitude.toString()
                longitude = location.longitude.toString()
                speed = "${(location.speed * 3.6).toInt()} km/h" // Convert m/s to km/h
                bearing = "${location.bearing}°"
                accuracy = "${location.accuracy} m"
                lastUpdate = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                // Load stops
                checkNearestStop(location.latitude, location.longitude)

            }
        } else {
            // Permission Denied
            isTracking = false
        }
    }

    // Animation State
    val cardOffsetY = remember { Animatable(100f) }

    // Trigger Animation
    LaunchedEffect(Unit) {
        cardOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(gradientTop, gradientBottom)
                )
            )
    ) {
        // Back Button
        IconButton(
            onClick = {
                if (isTracking) {
                    locationClient.stopLocationUpdates()
                    setVehicleStatus("inactive")
                }
                onBackClick()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 24.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = vehicleName,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        )

        // Moving Card Container
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
                .padding(top = 120.dp)
                .offset(y = cardOffsetY.value.dp)
        ) {
            // 1. The Main White Card (The Body)
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White
            ) {
                // 2. SCROLLABLE LIST
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Item 1: The Buttons Row
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            // ACTIVATE BUTTON
                            Button(
                                onClick = {
                                    if (!isTracking) {
                                        // Check Permission
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                            isTracking = true
                                            setVehicleStatus("active")
                                            locationClient.startLocationUpdates { location ->
                                                latitude = location.latitude.toString()
                                                longitude = location.longitude.toString()
                                                speed = "${(location.speed * 3.6).toInt()} km/h"
                                                bearing = "${location.bearing}°"
                                                accuracy = "${location.accuracy} m"
                                                lastUpdate = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                                // Load stops
                                                checkNearestStop(location.latitude, location.longitude)

                                            }
                                        } else {
                                            // Ask for Permission
                                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTracking) Color(0xFFE8F5E9) else Color(0xFFF3E5F5)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Text(
                                    if (isTracking) "Active" else "Activate GPS",
                                    color = if (isTracking) Color(0xFF2E7D32) else Color(0xFF6A1B9A)
                                )
                            }

                            // DEACTIVATE BUTTON
                            Button(
                                onClick = {
                                    isTracking = false
                                    timeElapsedSeconds = 0 // Reset timer
                                    setVehicleStatus("inactive")
                                    locationClient.stopLocationUpdates()
                                    // Optional: Reset values or keep last known location
                                    speed = "0 km/h"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            ) {
                                Text("Deactivate GPS", color = Color(0xFFC62828))
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Live data Rows (All Linked)
                    item { InfoRow(Icons.Default.DirectionsBus, "Station", currentStation) } // 🟢 Linked
                    item { InfoRow(Icons.Default.Navigation, "Heading Direction", bearing) } // 🟢 Linked
                    item { InfoRow(Icons.Default.LocationSearching, "Accuracy", accuracy) } // 🟢 Linked
                    item { InfoRow(Icons.Default.Schedule, "Last Update", lastUpdate) } // 🟢 Linked
                    item { InfoRow(Icons.Default.Info, "Ongoing Speed", speed) } // 🟢 Linked
                    item { InfoRow(Icons.Default.Place, "Latitude", latitude) } // 🟢 Linked
                    item { InfoRow(Icons.Default.Place, "Longitude", longitude) } // 🟢 Linked
                    item {
                        InfoRow(Icons.Default.DateRange, "Time Elapsed", formatTime(timeElapsedSeconds))
                        Spacer(modifier = Modifier.height(50.dp))
                    }
                }
            }

            // Safety Skirt (prevents gaps when bouncing)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(500.dp)
                    .offset(y = 500.dp)
                    .background(Color.White)
            )

            // 3. The "Tracker Menu" Label
            Surface(
                modifier = Modifier
                    .padding(start = 24.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.Black),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = if (isTracking) "Tracking Activated" else "Tracking Deactivated",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF455A64),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontSize = 16.sp, color = Color.Black)
            Text(text = value, fontSize = 12.sp, color = Color.Gray)
        }
    }
    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
}