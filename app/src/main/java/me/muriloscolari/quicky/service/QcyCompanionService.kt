package me.muriloscolari.quicky.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import me.muriloscolari.quicky.ble.QcyBleManager

@RequiresApi(Build.VERSION_CODES.S)
class QcyCompanionService : CompanionDeviceService() {

    @Deprecated("Deprecated in Java")
    override fun onDeviceAppeared(address: String) {
        handleAppeared(address)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        handleAppeared(associationInfo.deviceMacAddress?.toString())
    }

    @Deprecated("Deprecated in Java")
    override fun onDeviceDisappeared(address: String) {
        handleDisappeared()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        handleDisappeared()
    }

    private fun handleAppeared(macAddress: String?) {
        val ctx = applicationContext
        QcyBleManager.init(ctx)
        QcyBleManager.onAudioConnected(ctx)
        if (QcyBleManager.state.value.notificationsEnabled) {
            QcyControlService.start(ctx, macAddress)
        }
        QcyBleManager.connectOrAutoScan(macAddress)
    }

    @SuppressLint("MissingPermission")
    private fun handleDisappeared() {
        val ctx = applicationContext
        QcyBleManager.init(ctx)
        val btAdapter = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        val a2dpConn = runCatching {
            btAdapter?.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
        }.getOrDefault(false)
        val hfpConn = runCatching {
            btAdapter?.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
        }.getOrDefault(false)
        if (!a2dpConn && !hfpConn) {
            QcyBleManager.onAudioDisconnected(ctx)
        }
    }
}
