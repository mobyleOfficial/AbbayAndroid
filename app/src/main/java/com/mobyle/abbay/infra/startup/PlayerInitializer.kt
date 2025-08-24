package com.mobyle.abbay.infra.startup

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mobyle.abbay.presentation.common.service.PlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlayerInitializer : Initializer<ListenableFuture<MediaController>> {
    
    companion object {
        private const val TAG = "PlayerInitializer"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun create(context: Context): ListenableFuture<MediaController> {
        Log.d(TAG, "Initializing MediaController during app startup")
        
        val controller = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlayerService::class.java))
        ).buildAsync()

        // Pre-warm the controller in background to reduce startup time
        scope.launch {
            try {
                // Wait for the controller to be ready
                controller.get()
                Log.d(TAG, "MediaController initialized successfully during startup")
                // The controller is now ready and can be used immediately
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize MediaController during startup: ${e.message}")
                // Handle error silently during startup
                // The error will be handled when the controller is actually used
            }
        }

        return controller
    }
    
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
