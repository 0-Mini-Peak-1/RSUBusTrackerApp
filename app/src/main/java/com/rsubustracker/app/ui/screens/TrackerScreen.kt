package com.rsubustracker.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rsubustracker.app.network.RetrofitClient
import com.rsubustracker.app.network.StartTripRequest
import com.rsubustracker.app.network.StartTripResponse
import com.rsubustracker.app.network.Stop
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Intent
import com.rsubustracker.app.location.TrackingService
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape

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
    val sourceId = sharedPref.getString("CURRENT_SOURCE_ID", "") ?: ""
    val sourceType = sharedPref.getString("CURRENT_SOURCE_TYPE", "mobile") ?: "mobile"
    val token = sharedPref.getString("SENDER_TOKEN", "") ?: ""
    val authHeader = "Bearer $token"

    // Live data states
    var stopsList by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var currentStation by remember { mutableStateOf("En Route") }
    var latitude by remember { mutableStateOf("0.0") }
    var longitude by remember { mutableStateOf("0.0") }
    var speed by remember { mutableStateOf("0 km/h") }
    var bearing by remember { mutableStateOf("0.0°") }
    var accuracy by remember { mutableStateOf("0 m") }
    var lastUpdate by remember { mutableStateOf("-") }

    // LoRa Data States (Remote)
    var loraLat by remember { mutableStateOf("Waiting...") }
    var loraLng by remember { mutableStateOf("Waiting...") }
    var loraLastUpdate by remember { mutableStateOf("-") }
    var loraSourceId by remember { mutableStateOf("-") }

    // We don't use TrackingModeSelector anymore, the mode is driven by the backend source type
    var trackingMode by remember { mutableStateOf("phone") } 
    trackingMode = if (sourceType == "mobile") "phone" else "lora"

    // Check if we have an active trip that survived process death
    val savedTripId = sharedPref.getString("ACTIVE_TRIP_ID", "") ?: ""
    var activeTripId by remember { mutableStateOf(savedTripId) }
    var isTracking by remember { mutableStateOf(activeTripId.isNotBlank()) }

    // Timer state
    var timeElapsedSeconds by rememberSaveable { mutableLongStateOf(0L) }

    // This listens for the data from the TrackingService
    DisposableEffect(context) {
        val locationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "LOCATION_UPDATE") {
                    // ... (existing local GPS logic)
                    val lat = intent.getDoubleExtra("lat", 0.0)
                    val lng = intent.getDoubleExtra("lng", 0.0)
                    val spd = intent.getFloatExtra("speed", 0f)
                    val brg = intent.getFloatExtra("bearing", 0f)
                    val acc = intent.getFloatExtra("accuracy", 0f)
                    val incomingStationId = intent.getStringExtra("stationId") ?: ""

                    latitude = lat.toString()
                    longitude = lng.toString()
                    speed = "${(spd * 3.6).toInt()} km/h"
                    bearing = "${brg}°"
                    accuracy = "${acc} m"
                    lastUpdate = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                    if (incomingStationId.isNotBlank()) {
                        val matchedStop = stopsList.find { it.id == incomingStationId }
                        currentStation = matchedStop?.nameEn ?: "En Route"
                    } else {
                        currentStation = "En Route"
                    }
                }

                if (intent?.action == "LORA_LOCATION_UPDATE") {
                    val lat = intent.getDoubleExtra("lat", 0.0)
                    val lng = intent.getDoubleExtra("lng", 0.0)
                    val timestamp = intent.getStringExtra("recordedAt") ?: ""
                    val sourceId = intent.getStringExtra("sourceId") ?: "-"

                    loraLat = lat.toString()
                    loraLng = lng.toString()
                    loraSourceId = sourceId
                    loraLastUpdate = if (timestamp.isNotBlank()) {
                        // Extract time from ISO string or just use current time for simplicity if backend format is weird
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    } else {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    }
                }

                if (intent?.action == "ACTION_TRACKING_STOPPED") {
                    isTracking = false
                    timeElapsedSeconds = 0
                    speed = "0 km/h"
                    sharedPref.edit().remove("ACTIVE_TRIP_ID").apply()
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction("LOCATION_UPDATE")
            addAction("LORA_LOCATION_UPDATE")
            addAction("ACTION_TRACKING_STOPPED")
        }

        // Register the receiver
        ContextCompat.registerReceiver(
            context,
            locationReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Clean up the listener when the screen is destroyed
        onDispose {
            context.unregisterReceiver(locationReceiver)
        }
    }

    // Crash recovery
    LaunchedEffect(Unit) {
        // If we open the app and there is an ID, it means the system crashed us!
        if (activeTripId.isNotBlank() && vehicleId.isNotBlank()) {
            println("Recovered from system kill! Resuming service for trip: $activeTripId")

            // Instantly start the background service back up
            val intent = Intent(context, TrackingService::class.java).apply {
                action = "ACTION_START"
                putExtra("EXTRA_TRIP_ID", activeTripId)
                putExtra("EXTRA_VEHICLE_ID", vehicleId)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

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

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if they granted location
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            // Start service preparation
            val prepIntent = Intent(context, TrackingService::class.java).apply {
                action = "ACTION_PREPARE"
            }
            ContextCompat.startForegroundService(context, prepIntent)

            val request = StartTripRequest(vehicleId = vehicleId, trackingMode = trackingMode)
            RetrofitClient.instance.startTrip(authHeader, request).enqueue(object : Callback<StartTripResponse> {
                override fun onResponse(call: Call<StartTripResponse>, response: Response<StartTripResponse>) {

                    val newTripId = response.body()?.trip?.id
                    if (response.isSuccessful && !newTripId.isNullOrBlank()) {
                        activeTripId = newTripId
                        isTracking = true

                        // Save to storage to survive screen rotation
                        sharedPref.edit().putString("ACTIVE_TRIP_ID", activeTripId).apply()

                        // Tell the service to start
                        val intent = Intent(context, TrackingService::class.java).apply {
                            action = "ACTION_START"
                            putExtra("EXTRA_TRIP_ID", activeTripId)
                            putExtra("EXTRA_VEHICLE_ID", vehicleId)
                            putExtra("EXTRA_TRACKING_MODE", trackingMode)
                            putExtra("EXTRA_SOURCE_ID", sourceId)
                            putExtra("EXTRA_TOKEN", token)
                        }
                        ContextCompat.startForegroundService(context, intent)

                    } else if (response.code() == 401 || response.code() == 403) {
                        println("Token expired! Attempting auto-relogin...")
                        val secret = sharedPref.getString("CURRENT_SECRET", "") ?: ""
                        val loginReq = com.rsubustracker.app.network.LoginRequest(vehicleId, sourceId, secret)
                        RetrofitClient.instance.login(loginReq).enqueue(object : Callback<com.rsubustracker.app.network.LoginResponse> {
                            override fun onResponse(call: Call<com.rsubustracker.app.network.LoginResponse>, loginRes: Response<com.rsubustracker.app.network.LoginResponse>) {
                                if (loginRes.isSuccessful && loginRes.body()?.success == true) {
                                    val newToken = loginRes.body()?.token ?: ""
                                    sharedPref.edit().putString("SENDER_TOKEN", newToken).apply()
                                    // Retry startTrip with new token
                                    val newAuth = "Bearer $newToken"
                                    RetrofitClient.instance.startTrip(newAuth, request).enqueue(object : Callback<StartTripResponse> {
                                        override fun onResponse(call: Call<StartTripResponse>, retryRes: Response<StartTripResponse>) {
                                            val retryTripId = retryRes.body()?.trip?.id
                                            if (retryRes.isSuccessful && !retryTripId.isNullOrBlank()) {
                                                activeTripId = retryTripId
                                                isTracking = true
                                                sharedPref.edit().putString("ACTIVE_TRIP_ID", activeTripId).apply()
                                                val intent = Intent(context, TrackingService::class.java).apply {
                                                    action = "ACTION_START"
                                                    putExtra("EXTRA_TRIP_ID", activeTripId)
                                                    putExtra("EXTRA_VEHICLE_ID", vehicleId)
                                                    putExtra("EXTRA_TRACKING_MODE", trackingMode)
                                                    putExtra("EXTRA_SOURCE_ID", sourceId)
                                                    putExtra("EXTRA_TOKEN", newToken)
                                                }
                                                ContextCompat.startForegroundService(context, intent)
                                            } else {
                                                val stopIntent = Intent(context, TrackingService::class.java).apply { action = "ACTION_STOP" }
                                                context.startService(stopIntent)
                                            }
                                        }
                                        override fun onFailure(call: Call<StartTripResponse>, t: Throwable) {
                                            val stopIntent = Intent(context, TrackingService::class.java).apply { action = "ACTION_STOP" }
                                            context.startService(stopIntent)
                                        }
                                    })
                                } else {
                                    val stopIntent = Intent(context, TrackingService::class.java).apply { action = "ACTION_STOP" }
                                    context.startService(stopIntent)
                                }
                            }
                            override fun onFailure(call: Call<com.rsubustracker.app.network.LoginResponse>, t: Throwable) {
                                val stopIntent = Intent(context, TrackingService::class.java).apply { action = "ACTION_STOP" }
                                context.startService(stopIntent)
                            }
                        })
                    } else {
                        println("Failed to parse trip ID. Response was: ${response.code()}")
                        val stopIntent = Intent(context, TrackingService::class.java).apply { action = "ACTION_STOP" }
                        context.startService(stopIntent)
                    }
                }
                override fun onFailure(call: Call<StartTripResponse>, t: Throwable) {
                    println("Failed to start trip: ${t.message}")
                    val stopIntent = Intent(context, TrackingService::class.java).apply { action = "ACTION_STOP" }
                    context.startService(stopIntent)
                }
            })
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
                    // Kill the zombie service
                    val intent = Intent(context, TrackingService::class.java).apply {
                        action = "ACTION_STOP"
                    }
                    context.startService(intent)

                    // End the trip in the database
                    if (activeTripId.isNotBlank()) {
                        RetrofitClient.instance.endTrip(authHeader, activeTripId).enqueue(object : Callback<Void> {
                            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
                            override fun onFailure(call: Call<Void>, t: Throwable) {}
                        })
                    }
                }
                onBackClick() // Navigate back
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
            // The Main White Card (The Body)
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    // 1. SCROLLABLE LIST
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 60.dp, start = 24.dp, end = 24.dp),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. TRIP SUMMARY (Always at the top)
                        item {
                            SectionHeader("Time", Icons.Default.History)
                            InfoRow(Icons.Default.Timer, "Time Elapsed", formatTime(timeElapsedSeconds))
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // --- DATA SECTIONS ---
                        // Only display the data section for our current device type
                        
                        if (sourceType == "mobile") {
                            item { SectionHeader("Phone Sensor (Local GPS)", Icons.Default.Smartphone) }
                            item { InfoRow(Icons.Default.DirectionsBus, "Station", currentStation) }
                            item { InfoRow(Icons.Default.Navigation, "Heading Direction", bearing) }
                            item { InfoRow(Icons.Default.LocationSearching, "Accuracy", accuracy) }
                            item { InfoRow(Icons.Default.Schedule, "Last Update", lastUpdate) }
                            item { InfoRow(Icons.Default.Info, "Ongoing Speed", speed) }
                            item { InfoRow(Icons.Default.Place, "Latitude", latitude) }
                            item { InfoRow(Icons.Default.Place, "Longitude", longitude) }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        } else if (sourceType == "lorawan") {
                            item { SectionHeader("LoRaWAN Sensor (Remote)", Icons.Default.Sensors) }
                            item { InfoRow(Icons.Default.Info, "Sensor ID", loraSourceId) }
                            item { InfoRow(Icons.Default.Place, "LoRa Latitude", loraLat) }
                            item { InfoRow(Icons.Default.Place, "LoRa Longitude", loraLng) }
                            item { InfoRow(Icons.Default.Schedule, "Last LoRa Update", loraLastUpdate) }
                        } else if (sourceType == "esp32") {
                            item { SectionHeader("ESP32 Sensor (Remote)", Icons.Default.Memory) }
                            item { InfoRow(Icons.Default.Info, "Sensor ID", loraSourceId) }
                            item { InfoRow(Icons.Default.Place, "ESP32 Latitude", loraLat) }
                            item { InfoRow(Icons.Default.Place, "ESP32 Longitude", loraLng) }
                            item { InfoRow(Icons.Default.Schedule, "Last ESP32 Update", loraLastUpdate) }
                        }


                    }

                    // 2. THE FLOATING MORPHING BUTTON
                    val buttonColor by animateColorAsState(
                        targetValue = if (isTracking) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        animationSpec = tween(durationMillis = 300)
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isTracking) Color(0xFFC62828) else Color(0xFF2E7D32),
                        animationSpec = tween(durationMillis = 300)
                    )
                    val buttonWidth by animateDpAsState(
                        targetValue = if (isTracking) 190.dp else 200.dp,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
                    )

                    // Pin the button to the bottom center of the screen
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 32.dp ),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                if (!isTracking) {
                                    // --- THE START LOGIC ---
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        val prepIntent = Intent(context, TrackingService::class.java).apply { action = "ACTION_PREPARE" }
                                        ContextCompat.startForegroundService(context, prepIntent)

                                        val request = StartTripRequest(vehicleId = vehicleId, trackingMode = trackingMode)
                                        RetrofitClient.instance.startTrip(authHeader, request).enqueue(object : Callback<StartTripResponse> {
                                            override fun onResponse(call: Call<StartTripResponse>, response: Response<StartTripResponse>) {
                                                val newTripId = response.body()?.trip?.id
                                                if (response.isSuccessful && !newTripId.isNullOrBlank()) {
                                                    activeTripId = newTripId
                                                    isTracking = true
                                                    sharedPref.edit().putString("ACTIVE_TRIP_ID", activeTripId).apply()

                                                    val intent = Intent(context, TrackingService::class.java).apply {
                                                        action = "ACTION_START"
                                                        putExtra("EXTRA_TRIP_ID", activeTripId)
                                                        putExtra("EXTRA_VEHICLE_ID", vehicleId)
                                                        putExtra("EXTRA_TRACKING_MODE", trackingMode)
                                                        putExtra("EXTRA_SOURCE_ID", sourceId)
                                                        putExtra("EXTRA_TOKEN", token)
                                                    }
                                                    ContextCompat.startForegroundService(context, intent)
                                                } else {
                                                    val stopIntent = Intent(context, TrackingService::class.java).apply { action = "ACTION_STOP" }
                                                    context.startService(stopIntent)
                                                }
                                            }
                                            override fun onFailure(call: Call<StartTripResponse>, t: Throwable) {
                                                val stopIntent = Intent(context, TrackingService::class.java).apply { action = "ACTION_STOP" }
                                                context.startService(stopIntent)
                                            }
                                        })
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS))
                                        } else {
                                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                        }
                                    }
                                } else {
                                    // --- THE STOP LOGIC ---
                                    if (activeTripId.isNotBlank()) {
                                        RetrofitClient.instance.endTrip(authHeader, activeTripId).enqueue(object : Callback<Void> {
                                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                                if (response.isSuccessful) activeTripId = ""
                                            }
                                            override fun onFailure(call: Call<Void>, t: Throwable) {}
                                        })
                                    }

                                    val intent = Intent(context, TrackingService::class.java).apply { action = "ACTION_STOP" }
                                    context.startService(intent)

                                    isTracking = false
                                    timeElapsedSeconds = 0
                                    speed = "0 km/h"
                                    sharedPref.edit().remove("ACTIVE_TRIP_ID").apply()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = if (isTracking) 2.dp else 8.dp // High shadow so it looks like it's floating!
                            ),
                            modifier = Modifier
                                .width(buttonWidth)
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = if (isTracking) Icons.Default.StopCircle else Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isTracking) "Stop Tracking" else "Start Tracking",
                                textAlign = TextAlign.Left,
                                color = contentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
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

            // The "Tracker Status" Label
            val statusColor by animateColorAsState(
                targetValue = if (isTracking) Color(0xFF2E7D32) else Color(0xFF757575), // Deep Green vs Soft Grey
                animationSpec = tween(durationMillis = 300)
            )

            Surface(
                modifier = Modifier.padding(start = 24.dp),
                shape = RoundedCornerShape(percent = 50), // Makes it a perfect pill shape
                color = Color.White,
                shadowElevation = 8.dp // Keeps the nice floating shadow
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 25.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color = statusColor, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTracking) "Tracking Activated" else "Tracking Deactivated",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp // Slightly scaled down for a more refined look
                    )
                    if (isTracking) {
                        Text(
                            text = " • ${formatTime(timeElapsedSeconds)}",
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFC43C62), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFC43C62)
        )
    }
}

@Composable
fun TrackingModeSelector(selectedMode: String, onModeSelected: (String) -> Unit) {
    Column {
        Text(
            text = "Select Tracking Source",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeButton("Phone", "phone", selectedMode, onModeSelected, Modifier.weight(1f))
            ModeButton("LoRa", "lora", selectedMode, onModeSelected, Modifier.weight(1f))
            ModeButton("Both", "both", selectedMode, onModeSelected, Modifier.weight(1f))
        }
    }
}

@Composable
fun ModeButton(
    label: String,
    mode: String,
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = mode == selectedMode
    OutlinedButton(
        onClick = { onModeSelected(mode) },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) Color(0xFFE91E63).copy(alpha = 0.1f) else Color.Transparent,
            contentColor = if (isSelected) Color(0xFFE91E63) else Color.Gray
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFFE91E63) else Color.LightGray
        ),
        modifier = modifier.height(44.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
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