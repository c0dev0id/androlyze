package com.c0dev0id.androlyze

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.c0dev0id.androlyze.data.AppDatabase
import com.c0dev0id.androlyze.databinding.ActivityUsbEventsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UsbEventsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsbEventsBinding
    private lateinit var adapter: UsbEventsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsbEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.usb_events_title)

        adapter = UsbEventsAdapter()
        binding.recyclerViewUsbEvents.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewUsbEvents.adapter = adapter

        observeEvents()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun observeEvents() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            db.usbEventDao().getAllEvents().collectLatest { events ->
                adapter.submitList(events)
                binding.textEmpty.visibility =
                    if (events.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }
}
