package com.rsubustracker.app.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.google.gson.annotations.SerializedName

// Tracking request & response
data class StartTripRequest(
    val vehicleId: String,
    val trackingMode: String = "both" // "phone", "lora", or "both"
)
data class StartTripResponse(
    @SerializedName("message") val message: String,
    @SerializedName("trip") val trip: TripData
)
// The inner Prisma object
data class TripData(
    @SerializedName("id") val id: String
)
// Location Update
data class LocationUpdateRequest(
    val tripId: String,
    val vehicleId: String,
    val lat: Double,
    val lng: Double,
    val speed: Float,
    val bearing: Float,
    val accuracy: Float
)
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
    // Start the trip
    @POST("/api/trips/start")
    fun startTrip(@Body request: StartTripRequest): Call<StartTripResponse>
    // End the trip
    @PUT("/api/trips/{tripId}/end")
    fun endTrip(@Path("tripId") tripId: String): Call<Void>
    // Tracking (Legacy)
    @POST("/api/tracking/location")
    fun updateLocation(@Body request: LocationUpdateRequest): Call<Void>
    // Point to the routes
    @POST("/api/auth/vehicle-login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // update active status (Legacy)
    @PUT("api/admin/vehicles/{id}")
    fun updateStatus(
        @Path("id") id: String,
        @Body request: StatusRequest
    ): Call<Void>

    @GET("api/public/stops")
    fun getStops(): Call<List<Stop>>
}