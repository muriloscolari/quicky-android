package me.muriloscolari.quicky.service

import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
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
        QcyBleManager.init(applicationContext)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        QcyBleManager.init(applicationContext)
    }

    private fun handleAppeared(macAddress: String?) {
        val ctx = applicationContext
        QcyBleManager.init(ctx)
        if (QcyBleManager.state.value.notificationsEnabled) {
            QcyControlService.start(ctx, macAddress)
        }
        QcyBleManager.connectOrAutoScan(macAddress)
    }
}
