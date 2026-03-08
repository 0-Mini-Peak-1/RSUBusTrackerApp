package com.rsubustracker.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rsubustracker.app.network.LocationUpdateRequest
import com.rsubustracker.app.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.rsubustracker.app.network.ShuttleSocketManager

class LocationClient(context: Context) {

    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Config: Update every 3 seconds, high accuracy
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
        .setMinUpdateIntervalMillis(1000)
        .build()

    private var locationCallback: LocationCallback? = null

    // Initialize the Socket Manager
    val socketManager = ShuttleSocketManager()

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(vehicleId: String, tripId: String, onLocationUpdate: (Location) -> Unit) {

        // Connect the real-time socket
        socketManager.connect()

        // Define what happens when we get a new location
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // Pass it back to TrackerScreen so it can calculate the station
                    // and fire the socket from there!
                    onLocationUpdate(location)
                }
            }
        }

        client.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            client.removeLocationUpdates(it)
        }
    }
}