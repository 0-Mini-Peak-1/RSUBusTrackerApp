package com.rsubustracker.app.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import com.rsubustracker.app.location.MediaNotificationListenerService
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// --- Design Tokens ---
val ColorPink = Color(0xFFC94D7A)
val ColorPinkDark = Color(0xFFA83464)
val ColorPinkLight = Color(0xFFF9EEF4)
val ColorSurface = Color(0xFFF7F4F6)
val ColorBorder = Color(0xFFB47896).copy(alpha = 0.18f)
val ColorText = Color(0xFF1E1220)
val ColorTextMuted = Color(0xFF8A7580)
val ColorSuccess = Color(0xFF6EFFA0)
val ColorSuccessDark = Color(0xFF2ECB6A)
val ColorSuccessBg = Color(0xFFEDF9F1)
val ColorSuccessBorder = Color(0xFF90E0A8)

@Composable
fun EntertainmentScreen(
    isTracking: Boolean,
    isStarting: Boolean,
    timeElapsedSeconds: Int,
    stopsList: List<com.rsubustracker.app.network.Stop>,
    currentStation: String,
    vehicleName: String,
    speed: String,
    accuracy: String,
    sensorType: String,
    onToggleTracking: () -> Unit
) {
    var selectedMode by remember { mutableStateOf("ads") }
    var currentTime by remember { mutableStateOf(getCurrentTimeString()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = getCurrentTimeString()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorSurface)
    ) {
        TrackingSidebar(
            isTracking = isTracking,
            isStarting = isStarting,
            timeElapsedSeconds = timeElapsedSeconds,
            stopsList = stopsList,
            currentStation = currentStation,
            vehicleName = vehicleName,
            speed = speed,
            accuracy = accuracy,
            sensorType = sensorType,
            onToggleTracking = onToggleTracking
        )
        Column(modifier = Modifier.weight(1f)) {
            TopStatsBar(
                timeElapsedSeconds = timeElapsedSeconds,
                currentStation = currentStation,
                speed = speed,
                accuracy = accuracy,
                sensorType = sensorType
            )
            MainContentArea(
                mode = selectedMode,
                onModeSelected = { selectedMode = it },
                currentTime = currentTime,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

fun getCurrentTimeString(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

fun formatElapsed(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

@Composable
fun TrackingSidebar(
    isTracking: Boolean,
    isStarting: Boolean,
    timeElapsedSeconds: Int,
    stopsList: List<com.rsubustracker.app.network.Stop>,
    currentStation: String,
    vehicleName: String,
    speed: String,
    accuracy: String,
    sensorType: String,
    onToggleTracking: () -> Unit
) {
    var lastKnownStationIdx by remember { mutableIntStateOf(0) }
    val currentStopIdx = stopsList.indexOfFirst { it.nameEn == currentStation }.takeIf { it >= 0 } ?: lastKnownStationIdx
    
    LaunchedEffect(currentStation) {
        val idx = stopsList.indexOfFirst { it.nameEn == currentStation }
        if (idx >= 0) {
            lastKnownStationIdx = idx
        }
    }

    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(ColorPinkDark, ColorPink, Color(0xFFE06090))
                )
            )
    ) {
        // Vehicle Header
        Column(
            modifier = Modifier
                .padding(top = 20.dp, start = 18.dp, end = 18.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(vehicleName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("RSU Bus Tracker", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            // Start/Stop Tracking Button
            val btnColor = if (isTracking) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
            val txtColor = if (isTracking) Color(0xFFC62828) else Color(0xFF2E7D32)
            Button(
                onClick = onToggleTracking,
                enabled = !isStarting,
                colors = ButtonDefaults.buttonColors(containerColor = btnColor, disabledContainerColor = btnColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                if (isStarting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = txtColor, strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(txtColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isTracking) "STOP TRACKING" else "START TRACKING", color = txtColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }
        }

        // Route Progress
        Column(modifier = Modifier.weight(1f).padding(12.dp, 18.dp)) {
            Text("ROUTE PROGRESS", color = Color.White.copy(alpha = 0.45f), fontSize = 9.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
            if (stopsList.isEmpty()) {
                Text("No route data", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            } else {
                // Show up to 8 stops around the current stop to utilize the new space
                val startIdx = maxOf(0, currentStopIdx - 2)
                val displayStops = stopsList.drop(startIdx).take(8)
                
                displayStops.forEachIndexed { i, stop ->
                    val actualIdx = startIdx + i
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (actualIdx < currentStopIdx) ColorSuccess else if (actualIdx == currentStopIdx) Color.White else Color.White.copy(alpha = 0.25f),
                                    shape = CircleShape
                                )
                                .border(
                                    width = if (actualIdx == currentStopIdx) 2.dp else 0.dp,
                                    color = if (actualIdx == currentStopIdx) Color.White.copy(alpha = 0.35f) else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stop.nameEn,
                            color = if (actualIdx <= currentStopIdx) Color.White else Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = if (actualIdx == currentStopIdx) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopStatsBar(
    timeElapsedSeconds: Int,
    currentStation: String,
    speed: String,
    accuracy: String,
    sensorType: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, ColorBorder.copy(alpha = 0.05f))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopStatItem(Icons.Default.Timer, "TIME ELAPSED", formatElapsed(timeElapsedSeconds))
        Divider(modifier = Modifier.height(24.dp).width(1.dp), color = ColorBorder.copy(alpha = 0.3f))
        TopStatItem(Icons.Default.LocationOn, "STATION", currentStation, highlight = true)
        Divider(modifier = Modifier.height(24.dp).width(1.dp), color = ColorBorder.copy(alpha = 0.3f))
        TopStatItem(Icons.Default.Speed, "SPEED", speed)
        Divider(modifier = Modifier.height(24.dp).width(1.dp), color = ColorBorder.copy(alpha = 0.3f))
        TopStatItem(Icons.Default.GpsFixed, "GPS ACCURACY", accuracy)
        Divider(modifier = Modifier.height(24.dp).width(1.dp), color = ColorBorder.copy(alpha = 0.3f))
        TopStatItem(Icons.Default.Sensors, "SENSOR", sensorType)
    }
}

@Composable
fun TopStatItem(icon: ImageVector, label: String, value: String, highlight: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(if (highlight) ColorPink.copy(alpha = 0.1f) else ColorSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (highlight) ColorPink else ColorTextMuted, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, color = ColorTextMuted, fontSize = 9.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold)
            Text(value, color = if (highlight) ColorPink else ColorText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun StatRow(icon: ImageVector, label: String, value: String, highlight: Boolean = false) {
    val color = if (highlight) Color(0xFFFFC0D8) else Color.White
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (highlight) Color(0xFFFFC0D8) else Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label.uppercase(), color = Color.White.copy(alpha = 0.45f), fontSize = 9.sp, letterSpacing = 1.sp)
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun MainContentArea(mode: String, onModeSelected: (String) -> Unit, currentTime: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color.White)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("LANDSCAPE MODE", color = ColorPink, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            
            // Tabs
            Row(
                modifier = Modifier
                    .background(ColorSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                TabButton("ads", "Ads", Icons.Default.VideoLabel, mode, onModeSelected)
                TabButton("music", "Music", Icons.Default.MusicNote, mode, onModeSelected)
                TabButton("browser", "Browser", Icons.Default.Language, mode, onModeSelected)
            }
            
            Text(currentTime, color = ColorTextMuted, fontSize = 18.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp)
        }
        
        Divider(color = ColorBorder, thickness = 1.dp)

        // Panel Content
        Box(modifier = Modifier.fillMaxSize().padding(16.dp, 18.dp)) {
            when (mode) {
                "ads" -> AdsPanelContent()
                "music" -> MusicPanelContent()
                "browser" -> BrowserPanelContent()
            }
        }
    }
}

@Composable
fun TabButton(key: String, label: String, icon: ImageVector, currentMode: String, onSelected: (String) -> Unit) {
    val active = key == currentMode
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onSelected(key) }
            .background(if (active) ColorPink else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(horizontal = 15.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (active) Color.White else ColorTextMuted, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = if (active) Color.White else ColorTextMuted, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
    }
}

data class Ad(val id: Int, val brand: String, val tagline: String, val accent: Color, val initial: String)

val ADS_DATA = listOf(
    Ad(1, "Nike", "Just Do It", Color(0xFFFF6B35), "N"),
    Ad(2, "Apple", "Think Different", Color(0xFF555565), "A"),
    Ad(3, "Tesla", "Accelerate the World", Color(0xFFE82127), "T"),
    Ad(4, "Spotify", "Sound On. World Off.", Color(0xFF1DB954), "S"),
    Ad(5, "McDonald's", "I'm Lovin' It", Color(0xFFFFC72C), "M"),
    Ad(6, "Coca-Cola", "Open Happiness", Color(0xFFF40009), "C")
)

@Composable
fun AdsPanelContent() {
    var currentIndex by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            currentIndex = (currentIndex + 1) % ADS_DATA.size
        }
    }
    
    val currentAd = ADS_DATA[currentIndex]

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .background(currentAd.accent.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .border(2.dp, currentAd.accent.copy(alpha = 0.44f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(currentAd.accent.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .border(2.dp, currentAd.accent.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(currentAd.initial, color = currentAd.accent, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(currentAd.brand, color = ColorText, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(currentAd.tagline, color = ColorTextMuted, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun MusicPanelContent() {
    val context = LocalContext.current
    var hasPermission by remember { 
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        )
    }

    var trackName by remember { mutableStateOf(MediaNotificationListenerService.currentTrack) }
    var artistName by remember { mutableStateOf(MediaNotificationListenerService.currentArtist) }
    var isPlaying by remember { mutableStateOf(MediaNotificationListenerService.isPlaying) }

    DisposableEffect(Unit) {
        val listener = {
            trackName = MediaNotificationListenerService.currentTrack
            artistName = MediaNotificationListenerService.currentArtist
            isPlaying = MediaNotificationListenerService.isPlaying
        }
        MediaNotificationListenerService.addListener(listener)
        onDispose {
            MediaNotificationListenerService.removeListener(listener)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(bottom = 14.dp)) {
            Text("Music Player", color = ColorText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Control background music directly from the tracker", color = ColorTextMuted, fontSize = 12.sp)
        }

        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .border(1.5.dp, ColorBorder, RoundedCornerShape(18.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = ColorPink, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Notification Access Required", color = ColorText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("To display track names and artists,", color = ColorTextMuted, fontSize = 13.sp)
                Text("please grant Notification Access.", color = ColorTextMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { 
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPink)
                ) {
                    Text("Open Settings")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { 
                    hasPermission = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                }) {
                    Text("I've granted it", color = ColorPinkDark)
                }
            }
        } else {
            // Actual Player UI inspired by the prototype's cards
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Info Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .border(1.5.dp, ColorBorder, RoundedCornerShape(18.dp))
                        .padding(26.dp, 24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(ColorPinkLight, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = ColorPink, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(trackName, color = ColorText, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(artistName, color = ColorTextMuted, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
                }
                
                // Controls Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(ColorPinkLight.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .border(1.5.dp, ColorPink.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                        .padding(26.dp, 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { dispatchMediaKey(audioManager, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
                            modifier = Modifier.background(Color.White, CircleShape).border(1.dp, ColorBorder, CircleShape)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = ColorText)
                        }
                        IconButton(
                            onClick = { dispatchMediaKey(audioManager, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
                            modifier = Modifier.size(64.dp).background(ColorPink, CircleShape)
                        ) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        IconButton(
                            onClick = { dispatchMediaKey(audioManager, android.view.KeyEvent.KEYCODE_MEDIA_NEXT) },
                            modifier = Modifier.background(Color.White, CircleShape).border(1.dp, ColorBorder, CircleShape)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = ColorText)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(ColorPinkLight, RoundedCornerShape(10.dp))
                .border(1.dp, ColorBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 11.dp)
                .fillMaxWidth()
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = ColorPink, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Controls interact with the currently active media session (Spotify, YouTube, etc.)", color = ColorTextMuted, fontSize = 12.sp)
        }
    }
}

fun dispatchMediaKey(audioManager: android.media.AudioManager, keycode: Int) {
    val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
        putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keycode))
    }
    val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
        putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keycode))
    }
    audioManager.dispatchMediaKeyEvent(downIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)!!)
    audioManager.dispatchMediaKeyEvent(upIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)!!)
}

@Composable
fun BrowserPanelContent() {
    var url by remember { mutableStateOf("https://www.youtube.com") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Browser", color = ColorText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Quick links to media platforms", color = ColorTextMuted, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                BrowserBookmark("YouTube", "https://www.youtube.com", url, { url = it })
                BrowserBookmark("Spotify", "https://open.spotify.com", url, { url = it })
                BrowserBookmark("Google", "https://www.google.com", url, { url = it })
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(12.dp))
                .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                factory = { context ->
                    android.webkit.WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = android.webkit.WebViewClient()
                        webChromeClient = android.webkit.WebChromeClient()
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    if (webView.url != url && webView.url?.startsWith(url) != true) {
                        webView.loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun BrowserBookmark(label: String, targetUrl: String, currentUrl: String, onSelect: (String) -> Unit) {
    val active = currentUrl.startsWith(targetUrl)
    Text(
        text = label,
        color = if (active) ColorPink else ColorTextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clickable { onSelect(targetUrl) }
            .background(if (active) ColorPinkLight else Color.Transparent, RoundedCornerShape(20.dp))
            .border(1.5.dp, if (active) ColorPink else ColorBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}
