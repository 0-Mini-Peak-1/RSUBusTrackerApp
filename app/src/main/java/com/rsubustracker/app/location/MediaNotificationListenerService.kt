package com.rsubustracker.app.location

import android.app.Notification
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Context
import android.util.Log

class MediaNotificationListenerService : NotificationListenerService() {
    
    companion object {
        var currentTrack: String = "No track playing"
        var currentArtist: String = "Unknown Artist"
        var isPlaying: Boolean = false
        var albumArtUri: String? = null
        
        private val listeners = mutableListOf<() -> Unit>()
        
        fun addListener(listener: () -> Unit) {
            listeners.add(listener)
            listener() // trigger immediately
        }
        
        fun removeListener(listener: () -> Unit) {
            listeners.remove(listener)
        }
        
        private fun notifyListeners() {
            listeners.forEach { it() }
        }
    }

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeControllers: List<MediaController> = emptyList()

    private val activeSessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        activeControllers = controllers ?: emptyList()
        setupControllerCallbacks()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("MediaListener", "Listener Connected!")
        
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        
        try {
            // Need to pass a ComponentName if we want to be strict, but null works for NotificationListenerService 
            // if we have the BIND_NOTIFICATION_LISTENER_SERVICE permission granted.
            val componentName = android.content.ComponentName(this, MediaNotificationListenerService::class.java)
            activeControllers = mediaSessionManager?.getActiveSessions(componentName) ?: emptyList()
            mediaSessionManager?.addOnActiveSessionsChangedListener(activeSessionsChangedListener, componentName)
            setupControllerCallbacks()
        } catch (e: SecurityException) {
            Log.e("MediaListener", "SecurityException: Cannot access active media sessions.", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
    }

    private fun setupControllerCallbacks() {
        if (activeControllers.isEmpty()) {
            currentTrack = "No track playing"
            currentArtist = "Unknown Artist"
            isPlaying = false
            albumArtUri = null
            notifyListeners()
            return
        }

        // Just take the first active controller for now
        val controller = activeControllers.firstOrNull() ?: return
        
        updateFromController(controller)

        controller.registerCallback(object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                updateFromController(controller)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                updateFromController(controller)
            }
        })
    }

    private fun updateFromController(controller: MediaController) {
        val metadata = controller.metadata
        val state = controller.playbackState

        currentTrack = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Track"
        currentArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        
        // Android provides album art as a bitmap in metadata, but we can't easily pass bitmaps around statics safely.
        // For simplicity, we just won't render the bitmap for now unless we do proper state hoisting.
        // But we have the track name and artist!
        
        isPlaying = state?.state == PlaybackState.STATE_PLAYING
        
        notifyListeners()
    }
}
