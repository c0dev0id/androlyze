package com.c0dev0id.androlyze.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.c0dev0id.androlyze.MainActivity
import com.c0dev0id.androlyze.R
import com.c0dev0id.androlyze.data.AppDatabase
import com.c0dev0id.androlyze.rules.UsbRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

class LogcatDaemonService : LifecycleService() {

    companion object {
        private const val TAG = "LogcatDaemonService"
        private const val CHANNEL_ID = "androlyze_daemon"
        private const val NOTIFICATION_ID = 1
        const val ACTION_UPDATE_RULES = "com.c0dev0id.androlyze.UPDATE_RULES"
        const val PREFS_NAME = "rule_prefs"

        fun buildIntent(context: Context): Intent =
            Intent(context, LogcatDaemonService::class.java)
    }

    private lateinit var db: AppDatabase
    private lateinit var prefs: SharedPreferences
    @Volatile private var usbRule: UsbRule? = null
    private var logcatJob: Job? = null
    private var logcatProcess: Process? = null

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(this)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startLogcatMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_UPDATE_RULES) {
            updateRules()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        logcatProcess?.destroy()
        logcatJob?.cancel()
    }

    private fun updateRules() {
        val usbEnabled = prefs.getBoolean(UsbRule.RULE_ID, false)
        usbRule = if (usbEnabled) UsbRule(db, lifecycleScope) else null
        Log.d(TAG, "Rules updated — USB: $usbEnabled")
    }

    private fun startLogcatMonitoring() {
        updateRules()
        logcatJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf("logcat", "-v", "time", "*:D")
                )
                logcatProcess = process
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    processLine(line)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Logcat monitoring stopped: ${e.message}")
            }
        }
    }

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    private suspend fun processLine(line: String) = withContext(Dispatchers.Default) {
        // Parse logcat timestamp from the line (format: MM-DD HH:MM:SS.mmm)
        val timestamp = parseTimestamp(line)
        usbRule?.processLine(line, timestamp)
    }

    private fun parseTimestamp(line: String): Long {
        // Logcat "time" format: "MM-DD HH:MM:SS.mmm  PID  TID TAG: message"
        if (line.length < 18) return System.currentTimeMillis()
        return try {
            val dateStr = line.substring(0, 18).trim()
            dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
