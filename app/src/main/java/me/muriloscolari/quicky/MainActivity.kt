package me.muriloscolari.quicky

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import java.util.regex.Pattern
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.muriloscolari.quicky.ble.QcyBleManager
import me.muriloscolari.quicky.ble.QcyProtocol
import me.muriloscolari.quicky.service.QcyControlService
import me.muriloscolari.quicky.ui.QuickyScreen
import me.muriloscolari.quicky.update.AppUpdater
import me.muriloscolari.quicky.update.ReleaseInfo
import me.muriloscolari.quicky.update.UpdateSource

class MainActivity : ComponentActivity() {

    private var onBackgroundPermissionRefresh: (() -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            QcyControlService.start(this)
        }
    }

    private val companionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        QcyBleManager.ensureObservingCompanionPresence(applicationContext)
        onBackgroundPermissionRefresh?.invoke()
        if (!QcyBleManager.state.value.connected && !QcyBleManager.state.value.connecting) {
            QcyBleManager.connectOrAutoScan()
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

            var batteryUnrestricted by remember {
                mutableStateOf(QcyBleManager.isIgnoringBatteryOptimizations(this@MainActivity))
            }
            var companionLinked by remember {
                mutableStateOf(QcyBleManager.getCompanionAssociations(this@MainActivity).isNotEmpty())
            }
            var overlayPermissionGranted by remember {
                mutableStateOf(Settings.canDrawOverlays(this@MainActivity))
            }
            var companionStatusNote by remember { mutableStateOf<String?>(null) }

            val refreshBgState = {
                batteryUnrestricted = QcyBleManager.isIgnoringBatteryOptimizations(this@MainActivity)
                companionLinked = QcyBleManager.getCompanionAssociations(this@MainActivity).isNotEmpty()
                overlayPermissionGranted = Settings.canDrawOverlays(this@MainActivity)
                if (companionLinked) {
                    QcyBleManager.ensureObservingCompanionPresence(applicationContext)
                }
            }
            onBackgroundPermissionRefresh = refreshBgState

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        refreshBgState()
                        QcyBleManager.warmUpIfAudioConnected(applicationContext)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

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

            val isDarkUi = state.uiStyle != me.muriloscolari.quicky.ble.UiStyle.APPLE_LIQUID_GLASS || isSystemInDarkTheme()
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !isDarkUi
                    controller.isAppearanceLightNavigationBars = !isDarkUi
                }
            }

            MaterialTheme(colorScheme = if (isDarkUi) darkColorScheme() else lightColorScheme()) {
                QuickyScreen(
                    state = state,
                    checkingUpdate = checkingUpdate,
                    downloadingPercent = downloadingPercent,
                    availableRelease = availableRelease,
                    updateStatusText = updateStatusText,
                    batteryUnrestricted = batteryUnrestricted,
                    companionLinked = companionLinked,
                    overlayPermissionGranted = overlayPermissionGranted,
                    isXiaomiDevice = QcyBleManager.isXiaomiDevice(),
                    companionStatusNote = companionStatusNote,
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
                    onSelectEqPreset = { preset -> QcyBleManager.setEqPreset(preset) },
                    onSetCustomEqBands = { bands -> QcyBleManager.setCustomEqBands(bands) },
                    onSetSoundBalance = { value -> QcyBleManager.setSoundBalance(value) },
                    onSetKeyMapping = { keyId, funcId -> QcyBleManager.setKeyMapping(keyId, funcId) },
                    onStartFindEarbuds = { side -> QcyBleManager.startFindingEarbuds(side) },
                    onStopFindEarbuds = { QcyBleManager.stopFindingEarbuds() },
                    onToggleAirpodsPopup = { enabled -> QcyBleManager.setAirpodsPopupEnabled(enabled) },
                    onRequestOverlayPermission = { openOverlayPermissionSettings() },
                    onToggleNotifications = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }
                        QcyBleManager.setNotificationsEnabled(enabled)
                    },
                    onRequestBatteryUnrestricted = { openBatteryOptimizationSettings() },
                    onRequestCompanionLink = {
                        companionStatusNote = "Procurando fone QCY próximo (retire do estojo)..."
                        scope.launch {
                            requestCompanionAssociation { note ->
                                companionStatusNote = note
                                refreshBgState()
                            }
                        }
                    },
                    onOpenXiaomiAutostart = { openXiaomiAutostartSettings() },
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

    private fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        runCatching { startActivity(intent) }.onFailure {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
    }

    private fun openBatteryOptimizationSettings() {
        val reqIntent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName")
        )
        runCatching { startActivity(reqIntent) }.onFailure {
            runCatching {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    private fun openXiaomiAutostartSettings() {
        val candidates = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT)
        )
        for (intent in candidates) {
            if (runCatching { startActivity(intent) }.isSuccess) {
                return
            }
        }
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCompanionAssociation(onStatus: (String) -> Unit) {
        val cdm = getSystemService(Context.COMPANION_DEVICE_SERVICE) as? CompanionDeviceManager
        if (cdm == null) {
            onStatus("CompanionDeviceManager indisponível neste aparelho")
            return
        }

        // If we haven't seen the QCY BLE MAC yet, run a quick software BLE scan to capture it
        if (QcyBleManager.getKnownBleAddress() == null) {
            QcyBleManager.startScan(durationMs = 2000L, connectImmediatelyOnMatch = false)
            val startMs = System.currentTimeMillis()
            while (QcyBleManager.getKnownBleAddress() == null && System.currentTimeMillis() - startMs < 1800L) {
                delay(100L)
            }
            QcyBleManager.stopScan()
        }

        val bleMac = QcyBleManager.getKnownBleAddress()
        val primaryBonded = QcyBleManager.getPrimaryBondedQcyDevice(this)

        // Release active BLE GATT connection so the earbuds broadcast BLE advertisements
        // (required on Xiaomi HyperOS where CompanionDeviceManager scans live BLE packets).
        if (QcyBleManager.state.value.connected || QcyBleManager.state.value.connecting) {
            QcyBleManager.disconnect(resetPending = false)
            delay(350L)
        }

        val rawFriendly = primaryBonded?.let { runCatching { it.name }.getOrNull() }
            ?.takeIf { it.isNotBlank() }
            ?: QcyBleManager.state.value.modelTitle.ifBlank { "QCY MeloBuds Pro" }
        val cleanModel = rawFriendly
            .replace(Regex("^QCY\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifBlank { "MeloBuds Pro" }

        val requestBuilder = AssociationRequest.Builder()

        // 1. Classic Bluetooth filter for the primary bonded QCY device (matches immediately on Samsung)
        if (primaryBonded != null) {
            requestBuilder.addDeviceFilter(
                BluetoothDeviceFilter.Builder()
                    .setAddress(primaryBonded.address)
                    .build()
            )
        } else {
            requestBuilder.addDeviceFilter(
                BluetoothDeviceFilter.Builder()
                    .setNamePattern(Pattern.compile(".*(QCY|MeloBuds).*", Pattern.CASE_INSENSITIVE))
                    .build()
            )
        }

        // 2. BLE filter(s) with friendly display name ("QCY: MeloBuds Pro") so Xiaomi HyperOS
        // matches the live BLE advertisement and displays the model name instead of a raw MAC.
        if (!bleMac.isNullOrBlank()) {
            requestBuilder.addDeviceFilter(
                BluetoothLeDeviceFilter.Builder()
                    .setScanFilter(
                        ScanFilter.Builder()
                            .setDeviceAddress(bleMac)
                            .build()
                    )
                    .setRenameFromName("QCY", " $cleanModel", 2, 1)
                    .build()
            )
        }
        requestBuilder.addDeviceFilter(
            BluetoothLeDeviceFilter.Builder()
                .setScanFilter(
                    ScanFilter.Builder()
                        .setManufacturerData(QcyProtocol.COMPANY_ID_QCY, byteArrayOf())
                        .build()
                )
                .setRenameFromName("QCY", " $cleanModel", 2, 1)
                .build()
        )

        requestBuilder.setSingleDevice(true)

        val request = requestBuilder.build()

        val callback = object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) {
                runOnUiThread {
                    runCatching {
                        companionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }.onFailure {
                        onStatus("Falha ao abrir diálogo de vínculo: ${it.message}")
                        QcyBleManager.connectOrAutoScan()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onDeviceFound(chooserLauncher: IntentSender) {
                onAssociationPending(chooserLauncher)
            }

            override fun onFailure(error: CharSequence?) {
                runOnUiThread {
                    onStatus(error?.toString()?.takeIf { it.isNotBlank() } ?: "Nenhum fone QCY encontrado para vincular")
                    if (!QcyBleManager.state.value.connected && !QcyBleManager.state.value.connecting) {
                        QcyBleManager.connectOrAutoScan()
                    }
                }
            }
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                cdm.associate(request, mainExecutor, callback)
            } else {
                cdm.associate(request, callback, null)
            }
        }.onFailure {
            onStatus("Erro ao iniciar vínculo: ${it.message}")
            if (!QcyBleManager.state.value.connected && !QcyBleManager.state.value.connecting) {
                QcyBleManager.connectOrAutoScan()
            }
        }
    }
}
