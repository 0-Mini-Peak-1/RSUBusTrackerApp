package com.example.rsubustrackerapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for saved data (Auto-Login)
        val sharedPref = getSharedPreferences("BusTrackerPrefs", Context.MODE_PRIVATE)
        val savedVehicleId = sharedPref.getString("CURRENT_VEHICLE_ID", null)
        val initialLoginState = savedVehicleId != null // True if ID was found

        setContent {
            MaterialTheme {
                // Use saved state as the starting point
                var isLoggedIn by remember { mutableStateOf(initialLoginState) }

                AnimatedContent(
                    targetState = isLoggedIn,
                    label = "Screen Transition",
                    transitionSpec = {
                        if (targetState) {
                            // Login -> Tracker
                            slideInHorizontally { width -> width } togetherWith
                                    slideOutHorizontally { width -> -width }
                        } else {
                            // Tracker -> Login
                            slideInHorizontally { width -> -width } togetherWith
                                    slideOutHorizontally { width -> width }
                        }
                    }
                ) { targetState ->
                    if (targetState) {
                        TrackerScreen(
                            onBackClick = {
                                // Logout Logic
                                with(sharedPref.edit()) {
                                    clear()
                                    apply()
                                }
                                isLoggedIn = false
                            }
                        )
                    } else {
                        LoginScreen(
                            onLoginSuccess = {
                                isLoggedIn = true
                            }
                        )
                    }
                }
            }
        }
    }
}