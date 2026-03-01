package com.c0dev0id.androlyze

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.c0dev0id.androlyze.service.LogcatDaemonService

/**
 * Restarts the LogcatDaemonService after device reboot if any rule was previously enabled.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(
                LogcatDaemonService.PREFS_NAME, Context.MODE_PRIVATE
            )
            val anyEnabled = prefs.all.values.any { it == true }
            if (anyEnabled) {
                context.startForegroundService(LogcatDaemonService.buildIntent(context))
            }
        }
    }
}
