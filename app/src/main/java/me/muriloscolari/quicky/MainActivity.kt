package me.muriloscolari.quicky

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import me.muriloscolari.quicky.ble.QcyBleManager
import me.muriloscolari.quicky.service.QcyControlService
import me.muriloscolari.quicky.ui.QuickyScreen
import me.muriloscolari.quicky.update.AppUpdater
import me.muriloscolari.quicky.update.ReleaseInfo
import me.muriloscolari.quicky.update.UpdateSource

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            QcyControlService.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        QcyBleManager.init(applicationContext)
        ensurePermissionsAndStartService()

        setContent {
            val state by QcyBleManager.state.collectAsState()
            val scope = rememberCoroutineScope()

            var checkingUpdate by remember { mutableStateOf(false) }
            var downloadingPercent by remember { mutableStateOf<Int?>(null) }
            var availableRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
            var updateStatusText by remember { mutableStateOf("Pronto") }

            fun checkForUpdates(source: UpdateSource) {
                if (checkingUpdate) return
                checkingUpdate = true
                updateStatusText = "Verificando em ${source.label}..."
                scope.launch {
                    val result = AppUpdater.checkLatestRelease(source)
                    checkingUpdate = false
                    result.onSuccess { release ->
                        if (AppUpdater.isNewerVersion(BuildConfig.VERSION_NAME, release.versionClean)) {
                            availableRelease = release
                            updateStatusText = "Nova versão ${release.tagName} disponível!"
                        } else {
                            availableRelease = null
                            updateStatusText = "App atualizado (${release.tagName})"
                        }
                    }.onFailure { err ->
                        updateStatusText = err.message ?: "Falha ao verificar"
                    }
                }
            }

            LaunchedEffect(Unit) {
                if (state.autoCheckUpdates) {
                    checkForUpdates(state.updateSource)
                }
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                QuickyScreen(
                    state = state,
                    checkingUpdate = checkingUpdate,
                    downloadingPercent = downloadingPercent,
                    availableRelease = availableRelease,
                    updateStatusText = updateStatusText,
                    onScanClick = { QcyBleManager.startScan() },
                    onConnectDevice = { address -> QcyBleManager.connect(address) },
                    onDisconnectClick = { QcyBleManager.disconnect() },
                    onRefreshClick = { QcyBleManager.refreshBatteryAndVersion() },
                    onCycleAncClick = { QcyBleManager.cycleAncMode() },
                    onSelectAncMode = { mode -> QcyBleManager.setAncCycleMode(mode) },
                    onSelectAncCmdId = { cmdId -> QcyBleManager.setAncCmdId(cmdId) },
                    onToggleSleepMode = { enabled -> QcyBleManager.setSleepMode(enabled) },
                    onToggleAutoSleepOnConnect = { enabled -> QcyBleManager.setAutoSleepOnConnect(enabled) },
                    onToggleLowLatency = { enabled -> QcyBleManager.setLowLatency(enabled) },
                    onToggleLdac = { enabled -> QcyBleManager.setLdacEnabled(enabled) },
                    onToggleDualConnect = { enabled -> QcyBleManager.setDualConnectEnabled(enabled) },
                    onStartFindEarbuds = { side -> QcyBleManager.startFindingEarbuds(side) },
                    onStopFindEarbuds = { QcyBleManager.stopFindingEarbuds() },
                    onToggleNotifications = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }
                        QcyBleManager.setNotificationsEnabled(enabled)
                    },
                    onSelectUiStyle = { style -> QcyBleManager.setUiStyle(style) },
                    onSelectUpdateSource = { source ->
                        QcyBleManager.setUpdateSource(source)
                        checkForUpdates(source)
                    },
                    onToggleAutoCheckUpdates = { enabled -> QcyBleManager.setAutoCheckUpdates(enabled) },
                    onCheckUpdateNow = { checkForUpdates(state.updateSource) },
                    onDownloadAndInstallUpdate = { release ->
                        downloadingPercent = 0
                        updateStatusText = "Baixando ${release.tagName}..."
                        scope.launch {
                            val res = AppUpdater.downloadAndInstallApk(
                                context = this@MainActivity,
                                release = release,
                                onProgress = { pct -> downloadingPercent = pct }
                            )
                            downloadingPercent = null
                            res.onSuccess {
                                updateStatusText = "Instalador aberto para ${release.tagName}"
                            }.onFailure { err ->
                                updateStatusText = err.message ?: "Erro no download"
                            }
                        }
                    }
                )
            }
        }
    }

    private fun ensurePermissionsAndStartService() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                needed += Manifest.permission.BLUETOOTH_SCAN
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                needed += Manifest.permission.BLUETOOTH_CONNECT
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                needed += Manifest.permission.ACCESS_FINE_LOCATION
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && QcyBleManager.state.value.notificationsEnabled) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                needed += Manifest.permission.POST_NOTIFICATIONS
            }
        }

        if (needed.isEmpty()) {
            QcyControlService.start(this)
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}
