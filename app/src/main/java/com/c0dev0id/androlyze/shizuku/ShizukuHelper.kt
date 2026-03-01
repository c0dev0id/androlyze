package com.c0dev0id.androlyze.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku

private const val TAG = "ShizukuHelper"
private const val SHIZUKU_REQUEST_CODE = 1001

/**
 * Helper object for Shizuku-related operations.
 *
 * Shizuku is used to grant the READ_LOGS permission which requires a privileged context.
 */
object ShizukuHelper {

    /**
     * Returns true if Shizuku is available and its binder is alive.
     */
    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Exception) {
        Log.w(TAG, "Shizuku not available: ${e.message}")
        false
    }

    /**
     * Returns true if READ_LOGS permission is already granted to this app.
     */
    fun hasReadLogsPermission(context: Context): Boolean {
        return context.checkSelfPermission("android.permission.READ_LOGS") ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests READ_LOGS permission via Shizuku's shell execution.
     * The result is delivered through [Shizuku.OnRequestPermissionResultListener].
     */
    fun requestPermission(requestCode: Int = SHIZUKU_REQUEST_CODE) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request Shizuku permission: ${e.message}")
        }
    }

    /**
     * Returns true if the app has been granted Shizuku permission.
     */
    fun isShizukuPermissionGranted(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
        false
    }
}
