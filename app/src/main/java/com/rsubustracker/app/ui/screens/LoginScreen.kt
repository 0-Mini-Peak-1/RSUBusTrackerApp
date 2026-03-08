package com.rsubustracker.app.ui.screens

import android.content.Context
import android.widget.Toast
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
import com.rsubustracker.app.network.RetrofitClient
import com.rsubustracker.app.network.LoginRequest
import com.rsubustracker.app.network.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                text = "Shuttle Tracker System",
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
                    text = "Enter the Vehicle ID",
                    fontSize = 25.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = busId,
                    onValueChange = { busId = it },
                    label = { Text("Vehicle ID") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Home,
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
                            val request = LoginRequest(vehicleId = busId)

                            RetrofitClient.instance.login(request).enqueue(object : Callback<LoginResponse> {
                                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        // Get vehicle name
                                        val vehicleName = response.body()?.vehicle?.name ?: busId

                                        // SAVE Vehicle ID
                                        val sharedPref = context.getSharedPreferences("BusTrackerPrefs", Context.MODE_PRIVATE)
                                        with (sharedPref.edit()) {
                                            putString("CURRENT_VEHICLE_ID", busId)
                                            putString("CURRENT_VEHICLE_NAME", vehicleName)
                                            apply()
                                        }

                                        Toast.makeText(context, "Welcome ${response.body()?.vehicle?.name}", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        Toast.makeText(context, "Vehicle ID not found!", Toast.LENGTH_LONG).show()
                                    }
                                }

                                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                    Toast.makeText(context, "Connection Error: ${t.message}", Toast.LENGTH_LONG).show()
                                }
                            })
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