package com.rsubustracker.app.network

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import com.rsubustracker.app.BuildConfig

private const val BASE_URL = BuildConfig.BASE_URL

class ShuttleSocketManager {
    private var socket: Socket? = null
    private var onLoraUpdateListener: ((JSONObject) -> Unit)? = null

    fun setOnLoraUpdateListener(listener: (JSONObject) -> Unit) {
        onLoraUpdateListener = listener
    }

    fun connect() {
        try {
            val options = IO.Options()
            options.transports = arrayOf("websocket") // Force WebSocket transport

            socket = IO.socket(BASE_URL, options)

            // Listen for successful connection
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketIO", "Connected to the tracking server")
            }

            // Listen for disconnections
            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("SocketIO", "Disconnected from server")
            }

            // Listen for LoRaWAN updates from the backend
            socket?.on("lora-update") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    onLoraUpdateListener?.invoke(data)
                }
            }

            socket?.connect()

        } catch (e: Exception) {
            Log.e("SocketIO", "Error connecting to Socket.IO server: ${e.message}")
        }
    }

    fun sendLocationUpdate(tripId: String, busId: String, lat: Double, lng: Double, speed: Float, bearing: Float, accuracy: Float, station: String) {
        if (socket?.connected() == true) {
            val json = JSONObject().apply {
                put("tripId", tripId)
                put("vehicleId", busId)
                put("lat", lat)
                put("lng", lng)
                put("speed", speed)
                put("bearing", bearing)
                put("accuracy", accuracy)
                put("station", station)
            }

            // This matches the socket.on('send-location') in server.ts
            socket?.emit("send-location", json)
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