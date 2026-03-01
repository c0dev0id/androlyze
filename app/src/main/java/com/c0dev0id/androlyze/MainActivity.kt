package com.c0dev0id.androlyze

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.c0dev0id.androlyze.databinding.ActivityMainBinding
import com.c0dev0id.androlyze.rules.Rule
import com.c0dev0id.androlyze.rules.UsbRule
import com.c0dev0id.androlyze.service.LogcatDaemonService
import com.c0dev0id.androlyze.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity(), Shizuku.OnRequestPermissionResultListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RulesAdapter
    private val rules = mutableListOf(
        Rule(
            id = UsbRule.RULE_ID,
            name = "USB Devices",
            description = "Monitor USB device attach and detach events (device name, vendor ID, product ID)"
        )
    )

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermissionIfNeeded()
        loadRuleStates()
        setupRecyclerView()
        setupShizuku()
        requestBatteryOptimizationExemption()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this)
    }

    // --- RecyclerView setup ---

    private fun setupRecyclerView() {
        adapter = RulesAdapter(
            onToggle = { rule, isEnabled -> onRuleToggled(rule, isEnabled) },
            onView = { rule -> onRuleView(rule) }
        )
        binding.recyclerViewRules.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRules.adapter = adapter
        adapter.submitList(rules.toList())
    }

    private fun onRuleToggled(rule: Rule, isEnabled: Boolean) {
        rule.isEnabled = isEnabled
        saveRuleState(rule)
        if (isEnabled) {
            ensureReadLogsPermission {
                startOrUpdateDaemon()
            }
        } else {
            startOrUpdateDaemon()
        }
    }

    private fun onRuleView(rule: Rule) {
        when (rule.id) {
            UsbRule.RULE_ID -> {
                val intent = Intent(this, UsbEventsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // --- SharedPreferences persistence ---

    private fun loadRuleStates() {
        val prefs = getSharedPreferences(LogcatDaemonService.PREFS_NAME, Context.MODE_PRIVATE)
        rules.forEach { rule ->
            rule.isEnabled = prefs.getBoolean(rule.id, false)
        }
    }

    private fun saveRuleState(rule: Rule) {
        getSharedPreferences(LogcatDaemonService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(rule.id, rule.isEnabled).apply()
    }

    // --- Daemon control ---

    private fun startOrUpdateDaemon() {
        val anyEnabled = rules.any { it.isEnabled }
        if (anyEnabled) {
            val intent = LogcatDaemonService.buildIntent(this).apply {
                action = LogcatDaemonService.ACTION_UPDATE_RULES
            }
            startForegroundService(intent)
        } else {
            stopService(LogcatDaemonService.buildIntent(this))
        }
    }

    // --- Shizuku / READ_LOGS ---

    private fun setupShizuku() {
        Shizuku.addRequestPermissionResultListener(this)
    }

    private fun ensureReadLogsPermission(onGranted: () -> Unit) {
        when {
            ShizukuHelper.hasReadLogsPermission(this) -> onGranted()
            !ShizukuHelper.isAvailable() -> {
                Toast.makeText(
                    this,
                    getString(R.string.shizuku_not_running),
                    Toast.LENGTH_LONG
                ).show()
            }
            !ShizukuHelper.isShizukuPermissionGranted() -> {
                ShizukuHelper.requestPermission()
            }
            else -> {
                // Shizuku permission granted, try to grant READ_LOGS via shell
                grantReadLogsViaShizuku(onGranted)
            }
        }
    }

    private fun grantReadLogsViaShizuku(onGranted: () -> Unit) {
        lifecycleScope.launch {
            val granted = withContext(Dispatchers.IO) {
                try {
                    val process = Shizuku.newProcess(
                        arrayOf("pm", "grant", packageName, "android.permission.READ_LOGS"),
                        null, null
                    )
                    process.waitFor()
                    checkSelfPermission("android.permission.READ_LOGS") ==
                            PackageManager.PERMISSION_GRANTED
                } catch (e: Exception) {
                    false
                }
            }
            if (granted) {
                onGranted()
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.read_logs_denied), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            // Shizuku app permission was granted; now grant READ_LOGS
            grantReadLogsViaShizuku { startOrUpdateDaemon() }
        } else {
            Toast.makeText(this, getString(R.string.shizuku_denied), Toast.LENGTH_LONG).show()
        }
    }

    // --- Battery optimization ---

    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
