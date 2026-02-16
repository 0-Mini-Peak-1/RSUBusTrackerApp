package com.example.rsubustrackerapp

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*

class LocationClient(context: Context) {

    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Config: Update every 3 seconds, high accuracy
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
        .setMinUpdateIntervalMillis(1000)
        .build()

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationUpdate: (Location) -> Unit) {

        // Define what happens when we get a new location
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // Pass the location data back to the UI
                    onLocationUpdate(location)
                }
            }
        }

        // Start the engine!
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