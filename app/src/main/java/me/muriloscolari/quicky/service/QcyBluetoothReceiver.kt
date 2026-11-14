package me.muriloscolari.quicky.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import me.muriloscolari.quicky.ble.QcyBleManager

class QcyBluetoothReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appCtx = context.applicationContext
        QcyBleManager.init(appCtx)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                QcyBleManager.ensureObservingCompanionPresence(appCtx)
                QcyBleManager.warmUpIfAudioConnected(appCtx)
                return
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (btState == BluetoothAdapter.STATE_ON) {
                    QcyBleManager.ensureObservingCompanionPresence(appCtx)
                    QcyBleManager.warmUpIfAudioConnected(appCtx)
                } else if (btState == BluetoothAdapter.STATE_OFF || btState == BluetoothAdapter.STATE_TURNING_OFF) {
                    QcyBleManager.disconnect()
                }
                return
            }
        }

        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        val deviceName = runCatching { device?.name.orEmpty() }.getOrDefault("")
        val address = device?.address.orEmpty()
        val lastSaved = QcyBleManager.state.value.lastSavedAddress
        val matchesQcy = QcyBleManager.matchesAnySavedOrQcyName(deviceName, address) ||
            (lastSaved.isNotBlank() && deviceName.isBlank())

        val shouldConnect = when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> matchesQcy
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                val profileState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                profileState == BluetoothProfile.STATE_CONNECTED && matchesQcy
            }
            else -> false
        }

        if (!shouldConnect) return

        // Keep receiver process alive until BLE handshake + Auto Sleep Mode complete
        val pendingResult = goAsync()
        var finished = false
        val finishOnce = {
            if (!finished) {
                finished = true
                runCatching { pendingResult.finish() }
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({ finishOnce() }, 8_500L)

        if (QcyBleManager.state.value.notificationsEnabled) {
            QcyControlService.start(appCtx, lastSaved.ifBlank { null })
        }
        QcyBleManager.connectOrAutoScan(lastSaved.ifBlank { null }) {
            finishOnce()
        }
    }
}
