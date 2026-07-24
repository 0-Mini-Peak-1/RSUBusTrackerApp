package com.rsubustracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.rsubustracker.app.ui.screens.LoginScreen
import com.rsubustracker.app.ui.screens.TrackerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for saved data (Auto-Login)
        val sharedPref = getSharedPreferences("BusTrackerPrefs", MODE_PRIVATE)
        val savedToken = sharedPref.getString("SENDER_TOKEN", null)
        val initialLoginState = savedToken != null // True if token was found

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
                                    remove("SENDER_TOKEN")
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