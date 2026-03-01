package com.c0dev0id.androlyze

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.c0dev0id.androlyze.data.UsbEvent
import com.c0dev0id.androlyze.databinding.ItemUsbEventBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsbEventsAdapter : ListAdapter<UsbEvent, UsbEventsAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemUsbEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: UsbEvent) {
            binding.textAction.text = event.action
            binding.textTimestamp.text = dateFormat.format(Date(event.timestamp))
            binding.textDescription.text = event.description
            if (event.vendorId != 0 || event.productId != 0) {
                binding.textVendorProduct.text = binding.root.context.getString(
                    R.string.vendor_product_format, event.vendorId, event.productId
                )
                binding.textVendorProduct.visibility = android.view.View.VISIBLE
            } else {
                binding.textVendorProduct.visibility = android.view.View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUsbEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UsbEvent>() {
            override fun areItemsTheSame(oldItem: UsbEvent, newItem: UsbEvent) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: UsbEvent, newItem: UsbEvent) =
                oldItem == newItem
        }
    }
}
