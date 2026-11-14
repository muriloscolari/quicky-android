package me.muriloscolari.quicky.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.companion.CompanionDeviceManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.muriloscolari.quicky.service.QcyAncTileService
import me.muriloscolari.quicky.service.QcyControlService
import me.muriloscolari.quicky.update.UpdateSource
import kotlin.math.PI
import kotlin.math.sin

enum class UiStyle(val label: String, val subtitle: String) {
    XIAOMI_HYPEROS("Xiaomi HyperOS", "Blur translúcido e visual glassmorphism"),
    MATERIAL_LIGHTWEIGHT("Material 3 (Leve)", "Superfícies sólidas para celulares mais simples")
}

data class ScannedQcyDevice(
    val bleAddress: String,
    val name: String,
    val modelTitle: String,
    val vendorId: Int,
    val rssi: Int,
    val battery: QcyBattery,
    val controlMac: String,
    val otherMac: String = ""
)

data class QcyUiState(
    val scanning: Boolean = false,
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val connectedAddress: String = "",
    val modelTitle: String = "QCY MeloBuds Pro",
    val vendorId: Int = QcyProtocol.VENDOR_MELOBUDS_PRO,
    val firmware: String = "--",
    val battery: QcyBattery = QcyBattery(),
    val ancCycleMode: AncCycleMode = AncCycleMode.OFF,
    val ancCmdId: Int = AncCycleMode.OFF.meloBudsCmdId,
    val sleepModeEnabled: Boolean = false,
    val autoSleepOnConnect: Boolean = true,
    val lowLatencyEnabled: Boolean = false,
    val ldacEnabled: Boolean = false,
    val dualConnectEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val findingSide: FindEarbudSide? = null,
    val uiStyle: UiStyle = UiStyle.XIAOMI_HYPEROS,
    val updateSource: UpdateSource = UpdateSource.FORGEJO,
    val autoCheckUpdates: Boolean = true,
    val lastSavedAddress: String = "",
    val lastControlMac: String = "",
    val lastOtherMac: String = "",
    val devices: List<ScannedQcyDevice> = emptyList(),
    val statusMessage: String = "Pronto para conectar"
)

@SuppressLint("MissingPermission")
object QcyBleManager {
    private const val PREFS_NAME = "quicky_prefs"
    private const val KEY_AUTO_SLEEP = "auto_sleep_on_connect"
    private const val KEY_UI_STYLE = "ui_style"
    private const val KEY_UPDATE_SOURCE = "update_source"
    private const val KEY_AUTO_UPDATE = "auto_check_updates"
    private const val KEY_LAST_ADDRESS = "last_ble_address"
    private const val KEY_LAST_CONTROL_MAC = "last_control_mac"
    private const val KEY_LAST_OTHER_MAC = "last_other_mac"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_ANC_CYCLE_MODE = "last_anc_cycle_mode"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(QcyUiState())
    val state: StateFlow<QcyUiState> = _state.asStateFlow()

    private var appContext: Context? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var commandChar: BluetoothGattCharacteristic? = null
    private var batteryChar: BluetoothGattCharacteristic? = null
    private var versionChar: BluetoothGattCharacteristic? = null
    private var isScanning = false
    private val scannedMap = LinkedHashMap<String, ScannedQcyDevice>()

    private var fastAutoConnectTarget: String? = null
    private var pendingAncTargetMode: AncCycleMode? = null
    private var gattRetryAttempted = false
    private var wakeLock: PowerManager.WakeLock? = null
    private val pendingConnectCallbacks = mutableListOf<() -> Unit>()
    private val scanTimeoutRunnable = Runnable { onScanTimeoutReached() }
    private val directFallbackRunnable = Runnable { onDirectFallbackReached() }
    private val connectWatchdogRunnable = Runnable { onConnectWatchdogTimeout() }

    @Volatile
    private var findAudioThread: Thread? = null
    @Volatile
    private var activeFindTrack: AudioTrack? = null
    private val stopFindRunnable = Runnable { stopFindingEarbuds() }

    fun init(context: Context) {
        if (appContext != null) return
        val ctx = context.applicationContext
        appContext = ctx
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val autoSleep = prefs.getBoolean(KEY_AUTO_SLEEP, true)
        val styleName = prefs.getString(KEY_UI_STYLE, UiStyle.XIAOMI_HYPEROS.name) ?: UiStyle.XIAOMI_HYPEROS.name
        val sourceName = prefs.getString(KEY_UPDATE_SOURCE, UpdateSource.FORGEJO.name) ?: UpdateSource.FORGEJO.name
        val autoUpdate = prefs.getBoolean(KEY_AUTO_UPDATE, true)
        val lastAddr = prefs.getString(KEY_LAST_ADDRESS, "") ?: ""
        val lastControl = prefs.getString(KEY_LAST_CONTROL_MAC, "") ?: ""
        val lastOther = prefs.getString(KEY_LAST_OTHER_MAC, "") ?: ""
        val notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        val savedAncName = prefs.getString(KEY_ANC_CYCLE_MODE, AncCycleMode.OFF.name) ?: AncCycleMode.OFF.name

        val style = runCatching { UiStyle.valueOf(styleName) }.getOrDefault(UiStyle.XIAOMI_HYPEROS)
        val source = runCatching { UpdateSource.valueOf(sourceName) }.getOrDefault(UpdateSource.FORGEJO)
        val savedAnc = runCatching { AncCycleMode.valueOf(savedAncName) }.getOrDefault(AncCycleMode.OFF)

        _state.update {
            it.copy(
                autoSleepOnConnect = autoSleep,
                uiStyle = style,
                updateSource = source,
                autoCheckUpdates = autoUpdate,
                lastSavedAddress = lastAddr,
                lastControlMac = lastControl,
                lastOtherMac = lastOther,
                notificationsEnabled = notificationsEnabled,
                ancCycleMode = savedAnc,
                ancCmdId = savedAnc.meloBudsCmdId
            )
        }

        if (!notificationsEnabled) {
            QcyControlService.stop(ctx)
        }
        ensureObservingCompanionPresence(ctx)
    }

    private fun acquireShortWakeLock(durationMs: Long = 12_000L) {
        val ctx = appContext ?: return
        runCatching {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            val wl = wakeLock ?: pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Quicky:BleBackground").apply {
                setReferenceCounted(false)
                wakeLock = this
            }
            wl.acquire(durationMs)
        }
    }

    private fun releaseShortWakeLock() {
        runCatching {
            wakeLock?.let { wl ->
                if (wl.isHeld) wl.release()
            }
        }
    }

    private fun notifyConnectWaiters() {
        mainHandler.removeCallbacks(connectWatchdogRunnable)
        val callbacks = synchronized(pendingConnectCallbacks) {
            val copy = pendingConnectCallbacks.toList()
            pendingConnectCallbacks.clear()
            copy
        }
        callbacks.forEach { cb -> runCatching { cb() } }
        releaseShortWakeLock()
        appContext?.let { QcyAncTileService.requestTileUpdate(it) }
    }

    private fun onConnectWatchdogTimeout() {
        if (!_state.value.connected) {
            _state.update {
                it.copy(
                    connecting = false,
                    statusMessage = "Tempo esgotado ao conectar"
                )
            }
        }
        notifyConnectWaiters()
    }

    private fun adapter(): BluetoothAdapter? {
        val ctx = appContext ?: return null
        val manager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    fun setUiStyle(style: UiStyle) {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY_UI_STYLE, style.name)?.apply()
        _state.update { it.copy(uiStyle = style) }
    }

    fun setUpdateSource(source: UpdateSource) {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY_UPDATE_SOURCE, source.name)?.apply()
        _state.update { it.copy(updateSource = source) }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putBoolean(KEY_AUTO_UPDATE, enabled)?.apply()
        _state.update { it.copy(autoCheckUpdates = enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        val ctx = appContext
        ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)?.apply()
        _state.update {
            it.copy(
                notificationsEnabled = enabled,
                statusMessage = if (enabled) "Notificações ativadas" else "Notificações desativadas"
            )
        }
        if (ctx != null) {
            if (enabled) {
                QcyControlService.start(ctx)
            } else {
                QcyControlService.stop(ctx)
            }
        }
    }

    fun setAutoSleepOnConnect(enabled: Boolean) {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putBoolean(KEY_AUTO_SLEEP, enabled)?.apply()
        _state.update {
            it.copy(
                autoSleepOnConnect = enabled,
                statusMessage = if (enabled) "Auto Modo Sono ativado ao conectar" else "Auto Modo Sono desativado"
            )
        }
        if (enabled && _state.value.connected && !_state.value.sleepModeEnabled) {
            setSleepMode(true)
        }
    }

    private fun matchesKnownTarget(dev: ScannedQcyDevice, target: String?): Boolean {
        val t = target?.trim().orEmpty()
        if (t.isNotEmpty()) {
            return dev.bleAddress.equals(t, ignoreCase = true) ||
                dev.controlMac.equals(t, ignoreCase = true) ||
                dev.otherMac.equals(t, ignoreCase = true)
        }
        val s = _state.value
        if (s.lastSavedAddress.isBlank() && s.lastControlMac.isBlank() && s.lastOtherMac.isBlank()) {
            return true
        }
        return dev.bleAddress.equals(s.lastSavedAddress, ignoreCase = true) ||
            dev.controlMac.equals(s.lastControlMac, ignoreCase = true) ||
            dev.otherMac.equals(s.lastOtherMac, ignoreCase = true) ||
            dev.controlMac.equals(s.lastSavedAddress, ignoreCase = true)
    }

    private fun onScanTimeoutReached() {
        if (!isScanning) return
        val target = fastAutoConnectTarget
        stopScan()
        if (target != null && !_state.value.connected && !_state.value.connecting) {
            val match = _state.value.devices.firstOrNull { matchesKnownTarget(it, target) }
                ?: _state.value.devices.firstOrNull()
            if (match != null) {
                connect(match.bleAddress)
            } else if (target.isNotBlank()) {
                connect(target)
            } else {
                notifyConnectWaiters()
            }
        }
    }

    private fun onDirectFallbackReached() {
        val target = fastAutoConnectTarget?.takeIf { it.isNotBlank() }
            ?: _state.value.lastSavedAddress.takeIf { it.isNotBlank() }
            ?: return
        if (isScanning && !_state.value.connected && !_state.value.connecting) {
            stopScan()
            connect(target)
        }
    }

    fun startScan(
        durationMs: Long = 10_000L,
        autoConnectTarget: String? = null,
        connectImmediatelyOnMatch: Boolean = false
    ) {
        val btAdapter = adapter()
        if (btAdapter == null || !btAdapter.isEnabled) {
            _state.update { it.copy(statusMessage = "Ative o Bluetooth do celular") }
            notifyConnectWaiters()
            return
        }
        val scanner = btAdapter.bluetoothLeScanner
        if (scanner == null) {
            if (!autoConnectTarget.isNullOrBlank()) {
                connect(autoConnectTarget)
            } else {
                notifyConnectWaiters()
            }
            return
        }
        if (isScanning) stopScan()

        scannedMap.clear()
        isScanning = true
        fastAutoConnectTarget = if (connectImmediatelyOnMatch) (autoConnectTarget ?: "") else autoConnectTarget
        _state.update { it.copy(scanning = true, statusMessage = "Procurando fones QCY...") }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        runCatching { scanner.startScan(null, settings, scanCallback) }
            .onFailure {
                isScanning = false
                _state.update { it.copy(scanning = false, statusMessage = "Falha ao buscar BLE") }
                if (!autoConnectTarget.isNullOrBlank()) {
                    connect(autoConnectTarget)
                } else {
                    notifyConnectWaiters()
                }
                return
            }

        mainHandler.removeCallbacks(scanTimeoutRunnable)
        mainHandler.removeCallbacks(directFallbackRunnable)
        mainHandler.postDelayed(scanTimeoutRunnable, durationMs)

        if (connectImmediatelyOnMatch && (!autoConnectTarget.isNullOrBlank() || _state.value.lastSavedAddress.isNotBlank())) {
            mainHandler.postDelayed(directFallbackRunnable, 1_200L)
        }
    }

    fun stopScan() {
        mainHandler.removeCallbacks(scanTimeoutRunnable)
        mainHandler.removeCallbacks(directFallbackRunnable)
        if (!isScanning) return
        isScanning = false
        runCatching { adapter()?.bluetoothLeScanner?.stopScan(scanCallback) }
        _state.update {
            it.copy(
                scanning = false,
                statusMessage = if (it.connected) "Conectado" else "${it.devices.size} fone(s) encontrado(s)"
            )
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val record = result.scanRecord ?: return
            val qcyBytes = record.getManufacturerSpecificData(QcyProtocol.COMPANY_ID_QCY) ?: return
            val parsed = QcyProtocol.parseManufacturerData(qcyBytes) ?: return
            val address = result.device.address ?: return

            val dev = ScannedQcyDevice(
                bleAddress = address,
                name = result.device.name ?: parsed.modelTitle,
                modelTitle = parsed.modelTitle,
                vendorId = parsed.vendorId,
                rssi = result.rssi,
                battery = parsed.battery,
                controlMac = parsed.controlMac,
                otherMac = parsed.otherMac
            )
            scannedMap[address] = dev
            val sorted = scannedMap.values.sortedByDescending { it.rssi }
            _state.update { it.copy(devices = sorted) }

            val target = fastAutoConnectTarget
            if (target != null && !_state.value.connected && !_state.value.connecting) {
                if (matchesKnownTarget(dev, target)) {
                    fastAutoConnectTarget = null
                    connect(dev.bleAddress)
                }
            }
        }
    }

    fun connectOrAutoScan(preferredAddress: String? = null, onFinished: (() -> Unit)? = null) {
        if (onFinished != null) {
            synchronized(pendingConnectCallbacks) {
                pendingConnectCallbacks.add(onFinished)
            }
        }
        if (_state.value.connected && bluetoothGatt != null && commandChar != null) {
            notifyConnectWaiters()
            return
        }
        acquireShortWakeLock(12_000L)
        mainHandler.removeCallbacks(connectWatchdogRunnable)
        mainHandler.postDelayed(connectWatchdogRunnable, 10_000L)

        if (_state.value.connecting || isScanning) return

        val target = preferredAddress?.takeIf { it.isNotBlank() }
            ?: _state.value.lastSavedAddress.takeIf { it.isNotBlank() }
        if (target != null) {
            startScan(durationMs = 4_000L, autoConnectTarget = target, connectImmediatelyOnMatch = true)
        } else {
            startScan(durationMs = 6_000L, autoConnectTarget = "", connectImmediatelyOnMatch = true)
        }
    }

    fun connect(address: String) {
        val ctx = appContext ?: return
        val btAdapter = adapter() ?: return
        stopScan()
        disconnect(resetPending = false)
        gattRetryAttempted = false

        val scanned = scannedMap[address]
        if (scanned != null) {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
                putString(KEY_LAST_ADDRESS, scanned.bleAddress)
                if (scanned.controlMac.isNotBlank()) putString(KEY_LAST_CONTROL_MAC, scanned.controlMac)
                if (scanned.otherMac.isNotBlank()) putString(KEY_LAST_OTHER_MAC, scanned.otherMac)
                apply()
            }
            _state.update {
                it.copy(
                    lastSavedAddress = scanned.bleAddress,
                    lastControlMac = scanned.controlMac.ifBlank { it.lastControlMac },
                    lastOtherMac = scanned.otherMac.ifBlank { it.lastOtherMac }
                )
            }
        }

        _state.update {
            it.copy(
                connecting = true,
                modelTitle = scanned?.modelTitle ?: it.modelTitle,
                vendorId = scanned?.vendorId ?: it.vendorId,
                battery = scanned?.battery ?: it.battery,
                statusMessage = "Conectando a $address..."
            )
        }

        val device = runCatching { btAdapter.getRemoteDevice(address) }.getOrNull()
        if (device == null) {
            _state.update { it.copy(connecting = false, statusMessage = "Endereço Bluetooth inválido") }
            notifyConnectWaiters()
            return
        }

        mainHandler.removeCallbacks(connectWatchdogRunnable)
        mainHandler.postDelayed(connectWatchdogRunnable, 10_000L)

        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(ctx, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(ctx, false, gattCallback)
        }
        if (bluetoothGatt == null) {
            _state.update { it.copy(connecting = false, statusMessage = "Erro ao abrir GATT") }
            notifyConnectWaiters()
        }
    }

    fun disconnect(resetPending: Boolean = true) {
        if (resetPending) {
            pendingAncTargetMode = null
        }
        stopFindingEarbuds(sendBleStop = false)
        runCatching {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        }
        bluetoothGatt = null
        commandChar = null
        batteryChar = null
        versionChar = null
        _state.update {
            it.copy(
                connecting = false,
                connected = false,
                connectedAddress = "",
                statusMessage = "Desconectado"
            )
        }
        if (resetPending) {
            notifyConnectWaiters()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gattRetryAttempted = false
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                val addr = gatt.device?.address
                val shouldRetry = status == 133 && !gattRetryAttempted && !addr.isNullOrBlank()
                if (shouldRetry) {
                    gattRetryAttempted = true
                    runCatching { gatt.close() }
                    bluetoothGatt = null
                    mainHandler.postDelayed({
                        val ctx = appContext
                        val btAdapter = adapter()
                        val dev = if (ctx != null && btAdapter != null && !addr.isNullOrBlank()) {
                            runCatching { btAdapter.getRemoteDevice(addr) }.getOrNull()
                        } else null
                        if (dev != null && ctx != null) {
                            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                dev.connectGatt(ctx, false, this, BluetoothDevice.TRANSPORT_LE)
                            } else {
                                dev.connectGatt(ctx, false, this)
                            }
                        } else {
                            disconnect()
                        }
                    }, 350L)
                } else {
                    disconnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _state.update { it.copy(connecting = false, statusMessage = "Falha ao descobrir serviços BLE") }
                notifyConnectWaiters()
                return
            }
            val service = gatt.getService(QcyProtocol.SERVICE_UUID)
            if (service == null) {
                _state.update { it.copy(connecting = false, statusMessage = "Serviço QCY não encontrado") }
                notifyConnectWaiters()
                return
            }

            commandChar = service.getCharacteristic(QcyProtocol.COMMAND_UUID)
            batteryChar = service.getCharacteristic(QcyProtocol.BATTERY_UUID)
            versionChar = service.getCharacteristic(QcyProtocol.VERSION_UUID)
            val notifyChar = service.getCharacteristic(QcyProtocol.NOTIFY_UUID)

            if (notifyChar != null) {
                gatt.setCharacteristicNotification(notifyChar, true)
                val desc = notifyChar.getDescriptor(QcyProtocol.CCCD_UUID)
                if (desc != null) {
                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(desc)
                }
            }

            val addr = gatt.device.address ?: ""
            appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()?.putString(KEY_LAST_ADDRESS, addr)?.apply()

            _state.update {
                it.copy(
                    connecting = false,
                    connected = true,
                    connectedAddress = addr,
                    lastSavedAddress = addr,
                    statusMessage = "Conectado · ${it.modelTitle}"
                )
            }

            appContext?.let { ensureObservingCompanionPresence(it, addr) }

            // Read battery after descriptor write settles
            mainHandler.postDelayed({ refreshBatteryAndVersion() }, 250L)

            // CRITICAL FEATURE: Auto-enable Sleep Mode (ignore lateral touch controls) on connect
            val willAutoSleep = _state.value.autoSleepOnConnect
            if (willAutoSleep) {
                mainHandler.postDelayed({
                    setSleepMode(true, isAuto = true)
                }, 480L)
            }

            // Execute any queued ANC mode triggered from Quick Settings Tile while disconnected
            val pendingAnc = pendingAncTargetMode
            if (pendingAnc != null) {
                pendingAncTargetMode = null
                val ancDelay = if (willAutoSleep) 760L else 420L
                mainHandler.postDelayed({
                    setAncCycleMode(pendingAnc)
                    notifyConnectWaiters()
                }, ancDelay)
            } else {
                mainHandler.postDelayed({
                    notifyConnectWaiters()
                }, if (willAutoSleep) 680L else 360L)
            }

            // Query current state of LDAC and Dual Connect
            mainHandler.postDelayed({
                writeCommand(QcyProtocol.buildRequestDataCommand(0x23))
            }, 980L)
            mainHandler.postDelayed({
                writeCommand(QcyProtocol.buildRequestDataCommand(0x24))
            }, 1180L)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicBytes(characteristic.uuid, characteristic.value)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicBytes(characteristic.uuid, value)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            handleCharacteristicBytes(characteristic.uuid, characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicBytes(characteristic.uuid, value)
        }
    }

    private fun handleCharacteristicBytes(uuid: java.util.UUID, bytes: ByteArray?) {
        when (uuid) {
            QcyProtocol.BATTERY_UUID -> {
                val parsed = QcyProtocol.parseBatteryBytes(bytes) ?: return
                _state.update { it.copy(battery = parsed) }
                mainHandler.postDelayed({
                    val vChar = versionChar
                    val gatt = bluetoothGatt
                    if (vChar != null && gatt != null) {
                        gatt.readCharacteristic(vChar)
                    }
                }, 150L)
            }
            QcyProtocol.VERSION_UUID -> {
                val ver = QcyProtocol.parseVersionBytes(bytes) ?: return
                _state.update { it.copy(firmware = ver) }
            }
            QcyProtocol.NOTIFY_UUID -> {
                parseNotifyPacket(bytes)
            }
        }
    }

    private fun parseNotifyPacket(bytes: ByteArray?) {
        if (bytes == null || bytes.size < 4 || bytes[0] != 0xFF.toByte()) return
        val bodyLen = bytes[1].toInt() and 0xFF
        val end = minOf(bytes.size, 2 + bodyLen)
        var offset = 2
        while (offset + 2 <= end) {
            val cmdId = bytes[offset].toInt() and 0xFF
            val paramLen = bytes[offset + 1].toInt() and 0xFF
            val paramStart = offset + 2
            if (paramStart + paramLen > end) break
            when (cmdId) {
                0x09 -> if (paramLen >= 1) {
                    val enabled = (bytes[paramStart].toInt() and 0xFF) == 0x01
                    _state.update { it.copy(lowLatencyEnabled = enabled) }
                }
                0x10 -> if (paramLen >= 1) {
                    val enabled = (bytes[paramStart].toInt() and 0xFF) == 0x01
                    _state.update { it.copy(sleepModeEnabled = enabled) }
                }
                0x17 -> if (paramLen >= 3) {
                    val mode = bytes[paramStart].toInt() and 0xFF
                    val sub = bytes[paramStart + 1].toInt() and 0xFF
                    val noise = bytes[paramStart + 2].toInt() and 0xFF
                    val packed = (mode shl 16) or (sub shl 8) or noise
                    val cycle = QcyProtocol.modeFromCmdId(packed)
                    _state.update { it.copy(ancCmdId = packed, ancCycleMode = cycle) }
                }
                0x23 -> if (paramLen >= 1) {
                    val enabled = (bytes[paramStart].toInt() and 0xFF) == 0x01
                    _state.update { it.copy(ldacEnabled = enabled) }
                }
                0x24 -> if (paramLen >= 1) {
                    val enabled = (bytes[paramStart].toInt() and 0xFF) == 0x01
                    _state.update { it.copy(dualConnectEnabled = enabled) }
                }
                0x2F -> if (paramLen >= 3) {
                    val bat = QcyProtocol.parseBatteryBytes(bytes.copyOfRange(paramStart, paramStart + 3))
                    if (bat != null) {
                        _state.update { it.copy(battery = bat) }
                    }
                }
            }
            offset = paramStart + paramLen
        }
    }

    fun refreshBatteryAndVersion() {
        val gatt = bluetoothGatt ?: return
        val bChar = batteryChar ?: return
        runCatching { gatt.readCharacteristic(bChar) }
    }

    private fun writeCommand(packet: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val char = commandChar ?: return false
        return runCatching {
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            char.value = packet
            gatt.writeCharacteristic(char)
        }.getOrDefault(false)
    }

    fun setSleepMode(enabled: Boolean, isAuto: Boolean = false) {
        val packet = QcyProtocol.buildSleepModeCommand(enabled)
        val sent = writeCommand(packet)
        _state.update {
            it.copy(
                sleepModeEnabled = enabled,
                statusMessage = when {
                    !it.connected -> "Conecte o fone para enviar Modo Sono"
                    isAuto && enabled -> "Toques na lateral bloqueados automaticamente (Modo Sono)"
                    enabled && sent -> "Modo Sono ativado (toques bloqueados)"
                    !enabled && sent -> "Modo Sono desativado (toques liberados)"
                    else -> "Comando Modo Sono preparado"
                }
            )
        }
    }

    fun setLowLatency(enabled: Boolean) {
        val packet = QcyProtocol.buildLowLatencyCommand(enabled)
        writeCommand(packet)
        _state.update {
            it.copy(
                lowLatencyEnabled = enabled,
                statusMessage = if (enabled) "Modo Jogo (baixa latência) ativado" else "Modo Jogo desativado"
            )
        }
    }

    fun setLdacEnabled(enabled: Boolean) {
        val packet = QcyProtocol.buildLdacCommand(enabled)
        writeCommand(packet)
        _state.update {
            it.copy(
                ldacEnabled = enabled,
                dualConnectEnabled = if (enabled) false else it.dualConnectEnabled,
                statusMessage = if (enabled) {
                    "LDAC ativado (fones reiniciarão e Conexão Dupla será desativada)"
                } else {
                    "LDAC desativado (fones reiniciarão)"
                }
            )
        }
    }

    fun setDualConnectEnabled(enabled: Boolean) {
        val packet = QcyProtocol.buildDualConnectionCommand(enabled)
        writeCommand(packet)
        _state.update {
            it.copy(
                dualConnectEnabled = enabled,
                ldacEnabled = if (enabled) false else it.ldacEnabled,
                statusMessage = if (enabled) {
                    "Conexão Dupla ativada (fones reiniciarão e LDAC será desativado)"
                } else {
                    "Conexão Dupla desativada (fones reiniciarão)"
                }
            )
        }
    }

    fun startFindingEarbuds(side: FindEarbudSide) {
        mainHandler.removeCallbacks(stopFindRunnable)
        writeCommand(QcyProtocol.buildLightFlashCommand(true))
        mainHandler.postDelayed({
            writeCommand(QcyProtocol.buildTonePlayCommand(1))
        }, 120L)

        _state.update {
            it.copy(
                findingSide = side,
                statusMessage = "Tocando alerta sonoro (${side.label})..."
            )
        }

        // Update stereo gain immediately if already playing
        val existingTrack = activeFindTrack
        if (existingTrack != null && findAudioThread?.isAlive == true) {
            runCatching { existingTrack.setStereoVolume(side.leftGain, side.rightGain) }
            mainHandler.postDelayed(stopFindRunnable, 60_000L)
            return
        }

        val worker = Thread {
            val sampleRate = 44100
            // 320ms cycle: 140ms @ 2900Hz + 140ms @ 3500Hz + 40ms silence
            val totalSamples = (sampleRate * 0.32).toInt()
            val pcm = ShortArray(totalSamples * 2) // Stereo interleaved
            val split1 = (sampleRate * 0.14).toInt()
            val split2 = (sampleRate * 0.28).toInt()

            for (i in 0 until totalSamples) {
                val sample: Short = when {
                    i < split1 -> (sin(2.0 * PI * 2900.0 * i / sampleRate) * 30000).toInt().toShort()
                    i < split2 -> (sin(2.0 * PI * 3500.0 * i / sampleRate) * 30000).toInt().toShort()
                    else -> 0
                }
                pcm[i * 2] = sample
                pcm[i * 2 + 1] = sample
            }

            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(pcm.size * 2)

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: return@Thread

            activeFindTrack = track
            runCatching {
                val curSide = _state.value.findingSide ?: side
                track.setStereoVolume(curSide.leftGain, curSide.rightGain)
                track.play()
                while (!Thread.currentThread().isInterrupted && _state.value.findingSide != null) {
                    val s = _state.value.findingSide ?: break
                    track.setStereoVolume(s.leftGain, s.rightGain)
                    track.write(pcm, 0, pcm.size)
                    Thread.sleep(160L)
                }
            }
            runCatching {
                track.stop()
                track.release()
            }
            if (activeFindTrack === track) {
                activeFindTrack = null
            }
        }
        findAudioThread = worker
        worker.start()
        mainHandler.postDelayed(stopFindRunnable, 60_000L)
    }

    fun stopFindingEarbuds(sendBleStop: Boolean = true) {
        mainHandler.removeCallbacks(stopFindRunnable)
        val wasFinding = _state.value.findingSide != null
        _state.update {
            it.copy(
                findingSide = null,
                statusMessage = if (wasFinding && it.connected) "Busca sonora encerrada" else it.statusMessage
            )
        }
        findAudioThread?.interrupt()
        findAudioThread = null
        runCatching {
            activeFindTrack?.pause()
            activeFindTrack?.flush()
        }
        if (sendBleStop && _state.value.connected) {
            writeCommand(QcyProtocol.buildLightFlashCommand(false))
        }
    }

    fun setAncCmdId(cmdId: Int) {
        val packet = QcyProtocol.buildAncCommand(cmdId)
        val sent = writeCommand(packet)
        val cycleMode = QcyProtocol.modeFromCmdId(cmdId)
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY_ANC_CYCLE_MODE, cycleMode.name)?.apply()
        _state.update {
            it.copy(
                ancCmdId = cmdId,
                ancCycleMode = cycleMode,
                statusMessage = if (sent) "Modo de ruído: ${cycleMode.label}" else "Conectando para ${cycleMode.label}..."
            )
        }
        appContext?.let { QcyAncTileService.requestTileUpdate(it) }
    }

    fun setAncCycleMode(mode: AncCycleMode) {
        val cmdId = mode.meloBudsCmdId
        setAncCmdId(cmdId)
    }

    /**
     * Cycles ANC -> Ambiente (Transparency) -> OFF -> ANC with 1 tap.
     * Used by the Android Quick Settings Tile, Notification Action, and in-app cycle button.
     */
    fun cycleAncMode(): AncCycleMode {
        val nextMode = _state.value.ancCycleMode.next()
        setAncCycleMode(nextMode)
        return nextMode
    }

    /**
     * Cycles ANC and guarantees delivery even if the app was frozen/disconnected in background:
     * if GATT isn't connected yet, queues the desired ANC mode, connects immediately, and applies it.
     */
    fun cycleAncModeAndEnsureConnected(onFinished: (() -> Unit)? = null): AncCycleMode {
        val baseMode = pendingAncTargetMode ?: _state.value.ancCycleMode
        val nextMode = baseMode.next()

        if (_state.value.connected && bluetoothGatt != null && commandChar != null) {
            val sent = writeCommand(QcyProtocol.buildAncCommand(nextMode.meloBudsCmdId))
            if (sent) {
                appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    ?.edit()?.putString(KEY_ANC_CYCLE_MODE, nextMode.name)?.apply()
                _state.update {
                    it.copy(
                        ancCmdId = nextMode.meloBudsCmdId,
                        ancCycleMode = nextMode,
                        statusMessage = "Modo de ruído: ${nextMode.label}"
                    )
                }
                onFinished?.invoke()
                return nextMode
            }
            // Stale GATT link; reconnect and apply
            disconnect(resetPending = false)
        }

        pendingAncTargetMode = nextMode
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY_ANC_CYCLE_MODE, nextMode.name)?.apply()
        _state.update {
            it.copy(
                ancCmdId = nextMode.meloBudsCmdId,
                ancCycleMode = nextMode,
                statusMessage = "Conectando para ${nextMode.label}..."
            )
        }
        connectOrAutoScan(onFinished = onFinished)
        return nextMode
    }

    fun hasPendingAncCommand(): Boolean = pendingAncTargetMode != null

    fun warmUpIfAudioConnected(context: Context) {
        init(context)
        if (_state.value.connected || _state.value.connecting || isScanning) return
        val btAdapter = adapter() ?: return
        if (!btAdapter.isEnabled) return
        if (btAdapter.getProfileConnectionState(BluetoothProfile.A2DP) != BluetoothProfile.STATE_CONNECTED) return

        btAdapter.getProfileProxy(context.applicationContext, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                val devices = runCatching { proxy.connectedDevices }.getOrDefault(emptyList())
                val qcyConnected = devices.any { dev -> matchesAnySavedOrQcyName(dev.name, dev.address) }
                runCatching { btAdapter.closeProfileProxy(profile, proxy) }
                if (qcyConnected && !_state.value.connected && !_state.value.connecting) {
                    connectOrAutoScan()
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.A2DP)
    }

    fun matchesAnySavedOrQcyName(name: String?, address: String?): Boolean {
        val devName = name.orEmpty()
        val addr = address.orEmpty()
        val s = _state.value
        val byName = devName.contains("QCY", ignoreCase = true) ||
            devName.contains("MeloBuds", ignoreCase = true)
        val byAddr = addr.isNotBlank() && (
            addr.equals(s.lastSavedAddress, ignoreCase = true) ||
                addr.equals(s.lastControlMac, ignoreCase = true) ||
                addr.equals(s.lastOtherMac, ignoreCase = true)
            )
        return byName || byAddr
    }

    fun isXiaomiDevice(): Boolean =
        Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
            Build.BRAND.equals("Xiaomi", ignoreCase = true) ||
            Build.BRAND.equals("POCO", ignoreCase = true) ||
            Build.BRAND.equals("Redmi", ignoreCase = true)

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun getCompanionAssociations(context: Context): List<String> {
        val cdm = context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as? CompanionDeviceManager
            ?: return emptyList()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                cdm.myAssociations.mapNotNull { it.deviceMacAddress?.toString()?.uppercase() }
            }.getOrDefault(emptyList())
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                cdm.associations.map { it.uppercase() }
            }.getOrDefault(emptyList())
        }
    }

    fun ensureObservingCompanionPresence(context: Context, extraMac: String? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val cdm = context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as? CompanionDeviceManager
            ?: return
        val associated = getCompanionAssociations(context).toMutableSet()
        if (!extraMac.isNullOrBlank()) {
            val normalized = extraMac.uppercase()
            if (normalized !in associated) {
                // Only observe addresses already associated with CompanionDeviceManager
                // (calling startObservingDevicePresence on unassociated MAC throws SecurityException)
            }
        }
        associated.forEach { mac ->
            runCatching { cdm.startObservingDevicePresence(mac) }
        }
    }
}
