package com.c0dev0id.androlyze.rules

import com.c0dev0id.androlyze.data.AppDatabase
import com.c0dev0id.androlyze.data.UsbEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Parses logcat lines for USB device attach/detach events in Android's UsbHostManager logs.
 *
 * Example logcat lines this rule handles:
 *   UsbHostManager: Usb device attached: UsbDevice[mName=/dev/bus/usb/001/002,mVendorId=1234,mProductId=5678,...]
 *   UsbHostManager: Usb device detached: /dev/bus/usb/001/002
 *   UsbDeviceManager: USB connected
 *   kernel: usb 1-1: new full-speed USB device number 2 using xhci_hcd
 *   kernel: usb 1-1: New USB device found, idVendor=0951, idProduct=1666, bcdDevice= 1.00
 */
class UsbRule(
    private val db: AppDatabase,
    private val scope: CoroutineScope
) {
    companion object {
        const val RULE_ID = "usb_devices"

        // Matches UsbHostManager attach lines that include vendor/product IDs
        private val ATTACH_PATTERN = Regex(
            """UsbHostManager.*[Aa]ttach.*UsbDevice\[mName=([^,]+),mVendorId=(\d+),mProductId=(\d+)""",
            RegexOption.IGNORE_CASE
        )

        // Matches kernel USB "New USB device found" lines with idVendor and idProduct
        private val KERNEL_NEW_DEVICE_PATTERN = Regex(
            """usb\s+([\d-]+): New USB device found, idVendor=([0-9a-fA-F]+), idProduct=([0-9a-fA-F]+)""",
            RegexOption.IGNORE_CASE
        )

        // Matches UsbHostManager detach lines
        private val DETACH_PATTERN = Regex(
            """UsbHostManager.*[Dd]etach.*?(/dev/bus/usb/[\d/]+|USB device)""",
            RegexOption.IGNORE_CASE
        )

        // Matches kernel USB disconnect lines
        private val KERNEL_DISCONNECT_PATTERN = Regex(
            """usb\s+([\d-]+):\s+USB disconnect""",
            RegexOption.IGNORE_CASE
        )
    }

    /**
     * Processes a single logcat line and stores a UsbEvent if the line is relevant.
     * @return true if the line was consumed by this rule
     */
    fun processLine(line: String, timestampMs: Long): Boolean {
        ATTACH_PATTERN.find(line)?.let { match ->
            val deviceName = match.groupValues[1]
            val vendorId = match.groupValues[2].toIntOrNull() ?: 0
            val productId = match.groupValues[3].toIntOrNull() ?: 0
            storeEvent(
                timestampMs, "ATTACHED", deviceName, vendorId, productId,
                "USB device attached — name: $deviceName, vendorId: $vendorId, productId: $productId"
            )
            return true
        }

        KERNEL_NEW_DEVICE_PATTERN.find(line)?.let { match ->
            val port = match.groupValues[1]
            val vendorId = match.groupValues[2].toIntOrNull(16) ?: 0
            val productId = match.groupValues[3].toIntOrNull(16) ?: 0
            storeEvent(
                timestampMs, "ATTACHED", "usb $port", vendorId, productId,
                "USB device attached — port: $port, vendorId: 0x${match.groupValues[2]}, productId: 0x${match.groupValues[3]}"
            )
            return true
        }

        DETACH_PATTERN.find(line)?.let { match ->
            val deviceName = match.groupValues[1]
            storeEvent(
                timestampMs, "DETACHED", deviceName, 0, 0,
                "USB device detached — $deviceName"
            )
            return true
        }

        KERNEL_DISCONNECT_PATTERN.find(line)?.let { match ->
            val port = match.groupValues[1]
            storeEvent(
                timestampMs, "DETACHED", "usb $port", 0, 0,
                "USB device disconnected — port: $port"
            )
            return true
        }

        return false
    }

    private fun storeEvent(
        timestamp: Long,
        action: String,
        deviceName: String,
        vendorId: Int,
        productId: Int,
        description: String
    ) {
        scope.launch {
            db.usbEventDao().insert(
                UsbEvent(
                    timestamp = timestamp,
                    action = action,
                    deviceName = deviceName,
                    vendorId = vendorId,
                    productId = productId,
                    description = description
                )
            )
        }
    }
}
