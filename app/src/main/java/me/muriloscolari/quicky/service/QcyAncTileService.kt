package me.muriloscolari.quicky.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import me.muriloscolari.quicky.ble.AncCycleMode
import me.muriloscolari.quicky.ble.QcyBleManager

class QcyAncTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        QcyBleManager.init(applicationContext)
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        QcyBleManager.init(applicationContext)
        val currentState = QcyBleManager.state.value
        if (!currentState.connected && !currentState.connecting) {
            QcyBleManager.connectOrAutoScan()
        }
        QcyBleManager.cycleAncMode()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val s = QcyBleManager.state.value
        val mode = s.ancCycleMode

        tile.state = when (mode) {
            AncCycleMode.ANC, AncCycleMode.AMBIENT -> Tile.STATE_ACTIVE
            AncCycleMode.OFF -> Tile.STATE_INACTIVE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.label = "QCY Ruído"
            tile.subtitle = mode.label
        } else {
            tile.label = "QCY: ${mode.label}"
        }
        tile.updateTile()
    }
}
