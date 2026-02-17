package com.example.rsubustrackerapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

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

// Request status
data class StatusRequest(
    val status: String
)

// Stop data class
data class Stop(
    val id: String,
    val nameTh: String,
    val nameEn: String,
    val lat: Double,
    val lng: Double
)

interface ApiService {
    // Point to the routes
    @POST("/api/auth/vehicle-login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @PUT("api/admin/vehicles/{id}")
    fun updateStatus(
        @Path("id") id: String,
        @Body request: StatusRequest
    ): Call<Void>

    @GET("api/admin/stops")
    fun getStops(): Call<List<Stop>>
}