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
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.muriloscolari.quicky.update.UpdateSource

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
    val controlMac: String
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
    val uiStyle: UiStyle = UiStyle.XIAOMI_HYPEROS,
    val updateSource: UpdateSource = UpdateSource.FORGEJO,
    val autoCheckUpdates: Boolean = true,
    val lastSavedAddress: String = "",
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

        val style = runCatching { UiStyle.valueOf(styleName) }.getOrDefault(UiStyle.XIAOMI_HYPEROS)
        val source = runCatching { UpdateSource.valueOf(sourceName) }.getOrDefault(UpdateSource.FORGEJO)

        _state.update {
            it.copy(
                autoSleepOnConnect = autoSleep,
                uiStyle = style,
                updateSource = source,
                autoCheckUpdates = autoUpdate,
                lastSavedAddress = lastAddr
            )
        }
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

    fun startScan(durationMs: Long = 10_000L, autoConnectTarget: String? = null) {
        val btAdapter = adapter()
        if (btAdapter == null || !btAdapter.isEnabled) {
            _state.update { it.copy(statusMessage = "Ative o Bluetooth do celular") }
            return
        }
        val scanner = btAdapter.bluetoothLeScanner ?: return
        if (isScanning) stopScan()

        scannedMap.clear()
        isScanning = true
        _state.update { it.copy(scanning = true, statusMessage = "Procurando fones QCY...") }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
        mainHandler.postDelayed({
            if (isScanning) {
                stopScan()
                if (autoConnectTarget != null) {
                    val match = _state.value.devices.firstOrNull {
                        it.bleAddress.equals(autoConnectTarget, ignoreCase = true) ||
                            it.controlMac.equals(autoConnectTarget, ignoreCase = true)
                    } ?: _state.value.devices.firstOrNull()
                    if (match != null && !_state.value.connected && !_state.value.connecting) {
                        connect(match.bleAddress)
                    }
                }
            }
        }, durationMs)
    }

    fun stopScan() {
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
                controlMac = parsed.controlMac
            )
            scannedMap[address] = dev
            val sorted = scannedMap.values.sortedByDescending { it.rssi }
            _state.update { it.copy(devices = sorted) }
        }
    }

    fun connectOrAutoScan(preferredAddress: String? = null) {
        if (_state.value.connected || _state.value.connecting) return
        val target = preferredAddress?.takeIf { it.isNotBlank() }
            ?: _state.value.lastSavedAddress.takeIf { it.isNotBlank() }
        if (target != null) {
            startScan(durationMs = 4_500L, autoConnectTarget = target)
        } else {
            startScan(durationMs = 8_000L, autoConnectTarget = "")
        }
    }

    fun connect(address: String) {
        val ctx = appContext ?: return
        val btAdapter = adapter() ?: return
        stopScan()
        disconnect()

        val scanned = scannedMap[address]
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
            return
        }

        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(ctx, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(ctx, false, gattCallback)
        }
    }

    fun disconnect() {
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
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _state.update { it.copy(connecting = false, statusMessage = "Falha ao descobrir serviços BLE") }
                return
            }
            val service = gatt.getService(QcyProtocol.SERVICE_UUID)
            if (service == null) {
                _state.update { it.copy(connecting = false, statusMessage = "Serviço QCY não encontrado") }
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

            // Read battery after descriptor write settles
            mainHandler.postDelayed({ refreshBatteryAndVersion() }, 250L)

            // CRITICAL FEATURE: Auto-enable Sleep Mode (ignore lateral touch controls) on connect
            if (_state.value.autoSleepOnConnect) {
                mainHandler.postDelayed({
                    setSleepMode(true, isAuto = true)
                }, 550L)
            }
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

    fun setAncCmdId(cmdId: Int) {
        val packet = QcyProtocol.buildAncCommand(cmdId)
        writeCommand(packet)
        val cycleMode = QcyProtocol.modeFromCmdId(cmdId)
        _state.update {
            it.copy(
                ancCmdId = cmdId,
                ancCycleMode = cycleMode,
                statusMessage = "Modo de ruído: ${cycleMode.label}"
            )
        }
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
}
