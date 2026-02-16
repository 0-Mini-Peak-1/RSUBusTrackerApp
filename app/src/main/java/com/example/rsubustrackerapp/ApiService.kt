package com.example.rsubustrackerapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// Send the vehicleId
data class LoginRequest(
    val vehicleId: String
)

// Expect a Vehicle object back
data class VehicleData(
    val id: String,
    val name: String,
    val type: String,
    val status: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val vehicle: VehicleData? = null
)

interface ApiService {
    // Point to the route
    @POST("/api/auth/vehicle-login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}