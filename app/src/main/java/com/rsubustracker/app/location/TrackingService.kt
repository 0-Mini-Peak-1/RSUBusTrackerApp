package com.rsubustracker.app.location

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.rsubustracker.app.network.ShuttleSocketManager
import android.content.pm.ServiceInfo
import com.rsubustracker.app.network.RetrofitClient
import com.rsubustracker.app.network.Stop
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.app.PendingIntent
import com.rsubustracker.app.MainActivity

class TrackingService : Service() {

    private val CHANNEL_ID = "TrackingChannel"
    private val NOTIFICATION_ID = 1

    // Core Tracking Variables
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val socketManager = ShuttleSocketManager()

    private var tripId: String = ""
    private var vehicleId: String = ""
    private var stopsList: List<Stop> = emptyList()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        // Fetch stop since the service boot up
        RetrofitClient.instance.getStops().enqueue(object : Callback<List<Stop>> {
            override fun onResponse(call: Call<List<Stop>>, response: Response<List<Stop>>) {
                if (response.isSuccessful) {
                    stopsList = response.body() ?: emptyList()
                    Log.d("TrackingService", "Service loaded ${stopsList.size} stops")
                }
            }
            override fun onFailure(call: Call<List<Stop>>, t: Throwable) {}
        })
    }

    // Background math function
    private fun getNearestStationId(location: Location): String {
        // If we haven't loaded stops yet, or GPS is bad, return empty (En Route)
        if (stopsList.isEmpty() || location.accuracy > 25.0) return ""

        val detectionRadius = 20.0
        var foundStopId = ""
        var shortestDistance = Float.MAX_VALUE

        for (stop in stopsList) {
            val results = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, stop.lat, stop.lng, results)
            val distanceInMeters = results[0]

            if (distanceInMeters <= detectionRadius && distanceInMeters < shortestDistance) {
                if (location.speed <= 2.5) {
                    shortestDistance = distanceInMeters
                    foundStopId = stop.id // WE GRAB THE ID HERE!
                }
            }
        }
        return foundStopId
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        // Create a click action for the notification
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            // This flag brings the existing app to the front instead of making a new one
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // Android 12+ requires the FLAG_IMMUTABLE security tag
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        when (action) {
            "ACTION_PREPARE" -> {
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("RSU Shuttle Tracker")
                    .setContentText("Waking up server and connecting...")
                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .build()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }

            "ACTION_START" -> {
                // Grab the IDs sent from the UI
                tripId = intent.getStringExtra("EXTRA_TRIP_ID") ?: ""
                vehicleId = intent.getStringExtra("EXTRA_VEHICLE_ID") ?: ""

                // Start the un-swipeable foreground notification
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("RSU Shuttle Tracker")
                    .setContentText("Actively broadcasting GPS to the server...")
                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .build()

                // Tell Android exactly what kind of service this is so it doesn't hide it
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                // Connect Sockets and Start GPS!
                socketManager.connect()
                startLocationUpdates()
            }
            "ACTION_STOP" -> {
                // Clean everything up
                if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
                socketManager.disconnect()

                // Kill the zombie service
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(3000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->

                    val calculatedStationId = getNearestStationId(location)
                    // Backend handles the station maths
                    socketManager.sendLocationUpdate(
                        tripId = tripId,
                        busId = vehicleId,
                        lat = location.latitude,
                        lng = location.longitude,
                        speed = location.speed,
                        bearing = location.bearing,
                        accuracy = location.accuracy,
                        station = calculatedStationId
                    )

                    // Broadcast back to the UI so the screen updates
                    val updateIntent = Intent("LOCATION_UPDATE").apply {
                        setPackage(packageName)
                        putExtra("lat", location.latitude)
                        putExtra("lng", location.longitude)
                        putExtra("speed", location.speed)
                        putExtra("bearing", location.bearing)
                        putExtra("accuracy", location.accuracy)
                        putExtra("stationId", calculatedStationId)
                    }
                    sendBroadcast(updateIntent)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // This prevents killing errors
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("TrackingService", "App swiped away! Executing auto-deactivate...")

        // Wipe sharedPref if got killed
        val sharedPref = getSharedPreferences("BusTrackerPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().remove("ACTIVE_TRIP_ID").apply()

        // End trip
        if (tripId.isNotBlank()) {
            com.rsubustracker.app.network.RetrofitClient.instance.endTrip(tripId).enqueue(object : retrofit2.Callback<Void> {
                override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {}
                override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {}
            })
        }

        // Shut down the GPS and Socket
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        socketManager.disconnect()

        // Clear the notification and kill the zombie service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        socketManager.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Shuttle Tracking Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}