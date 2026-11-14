package me.muriloscolari.quicky.service

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import me.muriloscolari.quicky.ble.AncCycleMode
import me.muriloscolari.quicky.ble.QcyBleManager
import me.muriloscolari.quicky.ble.QcyProtocol

class QcyAncTileService : TileService() {

    companion object {
        fun requestTileUpdate(context: Context) {
            runCatching {
                requestListeningState(
                    context.applicationContext,
                    ComponentName(context.applicationContext, QcyAncTileService::class.java)
                )
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        QcyBleManager.init(applicationContext)
        updateTile()
        QcyBleManager.warmUpIfAudioConnected(applicationContext)
    }

    override fun onClick() {
        super.onClick()
        QcyBleManager.init(applicationContext)
        QcyBleManager.cycleAncModeAndEnsureConnected {
            updateTile()
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val s = QcyBleManager.state.value
        val mode = s.ancCycleMode
        val pendingOrConnecting = QcyBleManager.hasPendingAncCommand() || s.connecting || s.scanning

        tile.state = when (mode) {
            AncCycleMode.ANC, AncCycleMode.AMBIENT -> Tile.STATE_ACTIVE
            AncCycleMode.OFF -> Tile.STATE_INACTIVE
        }

        val shortLabel = QcyProtocol.formatAncShortLabel(s.ancCmdId)
        val subtitleText = if (pendingOrConnecting && !s.connected) {
            "$shortLabel (conectando...)"
        } else {
            shortLabel
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.label = "QCY Ruído"
            tile.subtitle = subtitleText
        } else {
            tile.label = "QCY: $subtitleText"
        }
        tile.updateTile()
    }
}
