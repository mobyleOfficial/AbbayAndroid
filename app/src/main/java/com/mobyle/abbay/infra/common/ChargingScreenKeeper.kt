package com.mobyle.abbay.infra.common

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class ChargingScreenKeeper(private val activity: Activity) : DefaultLifecycleObserver {
    private var isEnabled = false
    private var isCharging = false
    private var isRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            isCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            apply()
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        apply()
    }

    override fun onStart(owner: LifecycleOwner) {
        ContextCompat.registerReceiver(
            activity,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        isRegistered = true
    }

    override fun onStop(owner: LifecycleOwner) {
        if (isRegistered) activity.unregisterReceiver(batteryReceiver)
        isRegistered = false
        isCharging = false
        apply()
    }

    private fun apply() {
        if (isEnabled && isCharging) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
