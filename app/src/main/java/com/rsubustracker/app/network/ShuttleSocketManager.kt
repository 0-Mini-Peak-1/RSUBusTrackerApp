package com.rsubustracker.app.network

import android.content.Context
import android.util.Log
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import com.rsubustracker.app.BuildConfig

private const val BASE_URL = BuildConfig.BASE_URL

class ShuttleSocketManager(private val context: Context) {
    private var socket: Socket? = null
    var onLocationUpdateListener: ((JSONObject) -> Unit)? = null
    var onLoraUpdateListener: ((JSONObject) -> Unit)? = null
    var currentSourceId: String? = null
    var currentVehicleId: String? = null



    fun connect(token: String) {
        try {
            val sharedPref = context.getSharedPreferences("BusTrackerPrefs", Context.MODE_PRIVATE)
            currentVehicleId = sharedPref.getString("CURRENT_VEHICLE_ID", null)
            currentSourceId = sharedPref.getString("CURRENT_SOURCE_ID", null)

            val options = IO.Options()
            options.transports = arrayOf("websocket") // Force WebSocket transport
            options.auth = mapOf("token" to token)
            options.extraHeaders = mapOf("Authorization" to listOf("Bearer $token"))
            options.forceNew = true

            socket = IO.socket(BASE_URL, options)

            // Listen for successful connection
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketIO", "Connected to the tracking server")
            }

            // Listen for disconnections
            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("SocketIO", "Disconnected from server")
            }

            // Listen for canonical location updates from the backend
            socket?.on("location-update") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    
                    // Only process updates for our current vehicle!
                    if (data.optString("vehicleId") == currentVehicleId) {
                        onLoraUpdateListener?.invoke(data)
                    }
                }
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("SocketIO", "Connect Error: ${args.contentToString()}")
            }
            
            socket?.on("error-response") { args ->
                Log.e("SocketIO", "Backend Error Response: ${args.contentToString()}")
            }

            socket?.connect()

        } catch (e: Exception) {
            Log.e("SocketIO", "Error connecting to Socket.IO server: ${e.message}")
        }
    }

    fun sendLocationUpdate(sourceId: String, tripId: String?, busId: String, lat: Double, lng: Double, speed: Float, bearing: Float, accuracy: Float, station: String) {
        if (socket?.connected() == true) {
            val data = JSONObject().apply {
                put("sourceId", currentSourceId)
                put("lat", lat)
                put("lng", lng)
                put("speed", speed)
                put("bearing", bearing)
                put("accuracy", accuracy)
                if (tripId != null) {
                    put("tripId", tripId)
                }
            }
            
            // Update the mobile UI immediately so it's always in sync with our GPS
            onLocationUpdateListener?.invoke(data)

            socket?.emit("send-location", data, Ack { args ->
                // Acknowledgment from server
            })
        } else {
            Log.w("SocketIO", "Attempted to send location, but socket is not connected.")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off() // Remove all listeners
        socket = null
    }
}