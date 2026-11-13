package me.muriloscolari.quicky.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.muriloscolari.quicky.ble.QcyBleManager

class QcyBluetoothReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        QcyBleManager.init(context)

        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        val deviceName = runCatching { device?.name.orEmpty() }.getOrDefault("")
        val address = device?.address.orEmpty()
        val lastSaved = QcyBleManager.state.value.lastSavedAddress

        val looksLikeQcy = deviceName.contains("QCY", ignoreCase = true) ||
            deviceName.contains("MeloBuds", ignoreCase = true) ||
            (lastSaved.isNotBlank() && address.equals(lastSaved, ignoreCase = true))

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                if (looksLikeQcy || lastSaved.isNotBlank()) {
                    QcyControlService.start(context, lastSaved.ifBlank { null })
                }
            }
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                if (state == BluetoothProfile.STATE_CONNECTED && (looksLikeQcy || lastSaved.isNotBlank())) {
                    QcyControlService.start(context, lastSaved.ifBlank { null })
                }
            }
        }
    }
}
