package me.muriloscolari.quicky.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                    QcyBleManager.schedulePostBluetoothOnAudioCheck(appCtx)
                } else if (btState == BluetoothAdapter.STATE_OFF || btState == BluetoothAdapter.STATE_TURNING_OFF) {
                    QcyBleManager.cancelPostBluetoothOnAudioCheck()
                    QcyBleManager.onAudioDisconnected(appCtx)
                    QcyBleManager.disconnect()
                }
                return
            }
        }

        val shouldConnect = QcyBleManager.handleAudioBroadcast(appCtx, intent)
        if (!shouldConnect) return

        val lastSaved = QcyBleManager.state.value.lastSavedAddress

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
