package me.muriloscolari.quicky.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.actions.FloatAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.RangeTemplate
import android.service.controls.templates.ToggleRangeTemplate
import android.service.controls.templates.ToggleTemplate
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.muriloscolari.quicky.MainActivity
import me.muriloscolari.quicky.R
import me.muriloscolari.quicky.ble.AncCycleMode
import me.muriloscolari.quicky.ble.AncSubScene
import me.muriloscolari.quicky.ble.QcyBattery
import me.muriloscolari.quicky.ble.QcyBleManager
import me.muriloscolari.quicky.ble.QcyProtocol
import me.muriloscolari.quicky.ble.QcyUiState
import java.util.concurrent.Flow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.roundToInt

/**
 * Pure JVM helper for formatting and resolving Device Controls state & slider mappings.
 * Shared by QcyDeviceControlsService and unit tests.
 */
object QcyDeviceControlsLogic {
    const val ID_ANC = "qcy_anc"
    const val ID_AMBIENT = "qcy_ambient"
    const val ID_SLEEP_MODE = "qcy_sleep_mode"
    const val ID_GAME_MODE = "qcy_game_mode"
    const val ID_ANC_CYCLE = "qcy_anc_cycle"
    const val ID_BATTERY = "qcy_battery"

    val SUGGESTED_CONTROL_IDS = listOf(
        ID_ANC,
        ID_AMBIENT,
        ID_SLEEP_MODE,
        ID_GAME_MODE
    )

    val ALL_CONTROL_IDS = listOf(
        ID_ANC,
        ID_AMBIENT,
        ID_SLEEP_MODE,
        ID_GAME_MODE,
        ID_ANC_CYCLE,
        ID_BATTERY
    )

    fun formatBatterySummary(battery: QcyBattery, fallback: String): String {
        val parts = buildList {
            if (battery.left in 0..100) add("E ${battery.left}%")
            if (battery.right in 0..100) add("D ${battery.right}%")
            if (battery.caseLevel > 0 || battery.caseCharging) add("Estojo ${battery.caseLevel}%")
        }
        return if (parts.isNotEmpty()) parts.joinToString(" · ") else fallback
    }

    /**
     * Resolves the 1..3 slider position shown on the ANC control tile.
     * Single-level scenes (Adaptativo, Anti-vento) report level 3 (full).
     */
    fun ancSliderLevel(cmdId: Int, preferredAncCmdId: Int): Int {
        val sourceCmd = if (QcyProtocol.modeFromCmdId(cmdId) == AncCycleMode.ANC) cmdId else preferredAncCmdId
        val scene = QcyProtocol.subSceneForCmdId(sourceCmd) ?: return 3
        return if (scene.levelCount > 1) {
            (scene.levelFromCmdId(sourceCmd) ?: scene.levelCount).coerceIn(1, 3)
        } else {
            3
        }
    }

    /**
     * Resolves which ANC cmdId to send when the user slides the ANC tile to [level] (1..3).
     * Preserves the active multi-level scene (Interno / Transporte / Ruidoso) or falls back to Interno.
     */
    fun resolveAncCmdIdForSliderLevel(level: Int, currentCmdId: Int, preferredAncCmdId: Int): Int {
        val clamped = level.coerceIn(1, 3)
        val activeScene = QcyProtocol.subSceneForCmdId(currentCmdId)
            ?.takeIf { it.levelCount > 1 }
            ?: QcyProtocol.subSceneForCmdId(preferredAncCmdId)?.takeIf { it.levelCount > 1 }
            ?: AncSubScene.INDOOR
        return activeScene.cmdIdForLevel(clamped)
    }

    fun resolveAmbientLevel(cmdId: Int, preferredAmbientCmdId: Int): Int {
        val sourceCmd = if (QcyProtocol.modeFromCmdId(cmdId) == AncCycleMode.AMBIENT) {
            cmdId
        } else {
            preferredAmbientCmdId
        }
        return QcyProtocol.ambientLevelFromCmdId(sourceCmd)
    }
}

/**
 * Android 11+ (API 30+) Device Controls provider.
 * Works natively in:
 * - Xiaomi HyperOS Control Center ("Cartões adicionais" -> "Casa inteligente")
 * - Samsung OneUI Quick Panel ("Controle de dispositivos")
 * - Pixel / AOSP / Motorola Device Controls & Lock Screen
 */
@RequiresApi(Build.VERSION_CODES.R)
class QcyDeviceControlsService : ControlsProviderService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        private const val ZONE_LABEL = "Fones de ouvido"

        fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

        /**
         * Prompts the Android system (API 30+) to add QuiCkY controls to Device Controls / Control Center
         * when the host launcher/SystemUI supports requestAddControl.
         */
        fun requestAddControlToSystem(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
            val appCtx = context.applicationContext
            QcyBleManager.init(appCtx)
            val component = ComponentName(appCtx, QcyDeviceControlsService::class.java)
            val control = buildStatelessControl(
                context = appCtx,
                controlId = QcyDeviceControlsLogic.ID_ANC,
                state = QcyBleManager.state.value
            )
            return runCatching {
                requestAddControl(appCtx, component, control)
                true
            }.getOrDefault(false)
        }

        private fun mainActivityPendingIntent(context: Context, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun controlMeta(controlId: String, state: QcyUiState): Triple<String, String, Int> {
            val model = state.modelTitle.ifBlank { "QCY MeloBuds Pro" }
            val batShort = QcyDeviceControlsLogic.formatBatterySummary(state.battery, model)
            return when (controlId) {
                QcyDeviceControlsLogic.ID_ANC -> Triple("Cancelamento (ANC)", batShort, R.drawable.ic_control_anc)
                QcyDeviceControlsLogic.ID_AMBIENT -> Triple("Modo Ambiente", "Transparência", R.drawable.ic_control_ambient)
                QcyDeviceControlsLogic.ID_SLEEP_MODE -> Triple("Modo Sono", "Toques na lateral", R.drawable.ic_control_sleep)
                QcyDeviceControlsLogic.ID_GAME_MODE -> Triple("Modo Jogo", "Baixa latência", R.drawable.ic_control_game)
                QcyDeviceControlsLogic.ID_ANC_CYCLE -> Triple("Ciclar Ruído", batShort, R.drawable.ic_control_cycle)
                QcyDeviceControlsLogic.ID_BATTERY -> Triple(model, batShort, R.drawable.ic_headphones)
                else -> Triple(model, ZONE_LABEL, R.drawable.ic_headphones)
            }
        }

        private fun deviceTypeFor(controlId: String): Int = when (controlId) {
            QcyDeviceControlsLogic.ID_ANC,
            QcyDeviceControlsLogic.ID_AMBIENT -> DeviceTypes.TYPE_AC_HEATER
            QcyDeviceControlsLogic.ID_SLEEP_MODE,
            QcyDeviceControlsLogic.ID_GAME_MODE,
            QcyDeviceControlsLogic.ID_ANC_CYCLE,
            QcyDeviceControlsLogic.ID_BATTERY -> DeviceTypes.TYPE_GENERIC_ON_OFF
            else -> DeviceTypes.TYPE_GENERIC_ON_OFF
        }

        internal fun buildStatelessControl(
            context: Context,
            controlId: String,
            state: QcyUiState
        ): Control {
            val (title, subtitle, iconRes) = controlMeta(controlId, state)
            val structure = state.modelTitle.ifBlank { "QCY MeloBuds Pro" }
            val reqCode = 200 + QcyDeviceControlsLogic.ALL_CONTROL_IDS.indexOf(controlId).coerceAtLeast(0)
            return Control.StatelessBuilder(controlId, mainActivityPendingIntent(context, reqCode))
                .setTitle(title)
                .setSubtitle(subtitle)
                .setStructure(structure)
                .setZone(ZONE_LABEL)
                .setDeviceType(deviceTypeFor(controlId))
                .setCustomIcon(Icon.createWithResource(context, iconRes))
                .build()
        }

        internal fun buildStatefulControl(
            context: Context,
            controlId: String,
            state: QcyUiState
        ): Control {
            val (title, defaultSubtitle, iconRes) = controlMeta(controlId, state)
            val structure = state.modelTitle.ifBlank { "QCY MeloBuds Pro" }
            val reqCode = 300 + QcyDeviceControlsLogic.ALL_CONTROL_IDS.indexOf(controlId).coerceAtLeast(0)
            val pendingLink = !state.connected && (state.connecting || state.scanning)

            var subtitle = defaultSubtitle
            val statusText: String
            val template = when (controlId) {
                QcyDeviceControlsLogic.ID_ANC -> {
                    val isAnc = state.ancCycleMode == AncCycleMode.ANC
                    val level = QcyDeviceControlsLogic.ancSliderLevel(state.ancCmdId, state.preferredAncCmdId)
                    statusText = when {
                        pendingLink && QcyBleManager.hasPendingAncCommand() -> "Conectando..."
                        isAnc -> QcyProtocol.formatAncShortLabel(state.ancCmdId)
                        state.ancCycleMode == AncCycleMode.AMBIENT -> "Em Ambiente"
                        else -> "Desligado"
                    }
                    ToggleRangeTemplate(
                        "tpl_anc",
                        ControlButton(isAnc, "Alternar ANC"),
                        RangeTemplate(
                            "rng_anc",
                            1f,
                            3f,
                            level.toFloat(),
                            1f,
                            "%.0f/3"
                        )
                    )
                }

                QcyDeviceControlsLogic.ID_AMBIENT -> {
                    val isAmbient = state.ancCycleMode == AncCycleMode.AMBIENT
                    val level = QcyDeviceControlsLogic.resolveAmbientLevel(state.ancCmdId, state.preferredAmbientCmdId)
                    statusText = when {
                        pendingLink && QcyBleManager.hasPendingAncCommand() -> "Conectando..."
                        isAmbient -> "Nível $level/${QcyProtocol.AMBIENT_LEVEL_COUNT}"
                        else -> "Desligado"
                    }
                    ToggleRangeTemplate(
                        "tpl_ambient",
                        ControlButton(isAmbient, "Alternar Modo Ambiente"),
                        RangeTemplate(
                            "rng_ambient",
                            1f,
                            QcyProtocol.AMBIENT_LEVEL_COUNT.toFloat(),
                            level.toFloat(),
                            1f,
                            "%.0f/${QcyProtocol.AMBIENT_LEVEL_COUNT}"
                        )
                    )
                }

                QcyDeviceControlsLogic.ID_SLEEP_MODE -> {
                    val enabled = state.sleepModeEnabled
                    statusText = when {
                        pendingLink -> "Conectando..."
                        enabled -> "Bloqueados"
                        else -> "Ativos"
                    }
                    ToggleTemplate(
                        "tpl_sleep",
                        ControlButton(enabled, "Alternar Modo Sono")
                    )
                }

                QcyDeviceControlsLogic.ID_GAME_MODE -> {
                    val enabled = state.lowLatencyEnabled
                    statusText = when {
                        pendingLink -> "Conectando..."
                        enabled -> "Ativado"
                        else -> "Desativado"
                    }
                    ToggleTemplate(
                        "tpl_game",
                        ControlButton(enabled, "Alternar Modo Jogo")
                    )
                }

                QcyDeviceControlsLogic.ID_ANC_CYCLE -> {
                    val active = state.ancCycleMode != AncCycleMode.OFF
                    statusText = if (pendingLink && QcyBleManager.hasPendingAncCommand()) {
                        "Conectando..."
                    } else {
                        QcyProtocol.formatAncShortLabel(state.ancCmdId)
                    }
                    ToggleTemplate(
                        "tpl_anc_cycle",
                        ControlButton(active, "Ciclar ANC / Ambiente / OFF")
                    )
                }

                QcyDeviceControlsLogic.ID_BATTERY -> {
                    val batText = QcyDeviceControlsLogic.formatBatterySummary(
                        state.battery,
                        if (state.connected) "Conectado" else "Toque para conectar"
                    )
                    subtitle = batText
                    statusText = when {
                        state.connected -> QcyProtocol.formatAncShortLabel(state.ancCmdId)
                        state.connecting || state.scanning -> "Conectando..."
                        state.audioConnected -> "Áudio ativo"
                        else -> "Desconectado"
                    }
                    ToggleTemplate(
                        "tpl_battery",
                        ControlButton(state.connected || state.audioConnected, "Conectar ou atualizar")
                    )
                }

                else -> {
                    statusText = state.statusMessage
                    ToggleTemplate("tpl_default", ControlButton(state.connected, "Alternar"))
                }
            }

            val builder = Control.StatefulBuilder(controlId, mainActivityPendingIntent(context, reqCode))
                .setTitle(title)
                .setSubtitle(subtitle)
                .setStructure(structure)
                .setZone(ZONE_LABEL)
                .setDeviceType(deviceTypeFor(controlId))
                .setCustomIcon(Icon.createWithResource(context, iconRes))
                .setStatus(Control.STATUS_OK)
                .setStatusText(statusText)
                .setControlTemplate(template)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                builder.setAuthRequired(false)
            }
            return builder.build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        QcyBleManager.init(applicationContext)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        QcyBleManager.init(applicationContext)
        return statelessPublisher(QcyDeviceControlsLogic.ALL_CONTROL_IDS)
    }

    override fun createPublisherForSuggested(): Flow.Publisher<Control> {
        QcyBleManager.init(applicationContext)
        return statelessPublisher(QcyDeviceControlsLogic.SUGGESTED_CONTROL_IDS)
    }

    private fun statelessPublisher(controlIds: List<String>): Flow.Publisher<Control> {
        val ctx = applicationContext
        val snapshot = QcyBleManager.state.value
        val controls = controlIds.map { id -> buildStatelessControl(ctx, id, snapshot) }
        return Flow.Publisher { subscriber ->
            val index = AtomicInteger(0)
            val cancelled = AtomicBoolean(false)
            val completed = AtomicBoolean(false)

            subscriber.onSubscribe(object : Flow.Subscription {
                override fun request(n: Long) {
                    if (cancelled.get() || completed.get() || n <= 0L) return
                    var remaining = n
                    while (remaining > 0L && !cancelled.get()) {
                        val idx = index.getAndIncrement()
                        if (idx >= controls.size) {
                            if (completed.compareAndSet(false, true)) {
                                subscriber.onComplete()
                            }
                            return
                        }
                        subscriber.onNext(controls[idx])
                        remaining--
                    }
                    if (index.get() >= controls.size && !cancelled.get() && completed.compareAndSet(false, true)) {
                        subscriber.onComplete()
                    }
                }

                override fun cancel() {
                    cancelled.set(true)
                }
            })
        }
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        val ctx = applicationContext
        QcyBleManager.init(ctx)
        val validIds = controlIds.filter { it in QcyDeviceControlsLogic.ALL_CONTROL_IDS }.distinct()

        return Flow.Publisher { subscriber ->
            val started = AtomicBoolean(false)
            val cancelled = AtomicBoolean(false)
            var collectJob: Job? = null

            subscriber.onSubscribe(object : Flow.Subscription {
                override fun request(n: Long) {
                    if (cancelled.get() || n <= 0L) return
                    if (!started.compareAndSet(false, true)) return

                    // Emit initial snapshot immediately so HyperOS / OneUI / AOSP render with zero delay
                    val initialState = QcyBleManager.state.value
                    validIds.forEach { id ->
                        if (!cancelled.get()) {
                            subscriber.onNext(buildStatefulControl(ctx, id, initialState))
                        }
                    }

                    // Wake BLE if earbuds are already connected via Bluetooth Audio
                    QcyBleManager.warmUpIfAudioConnected(ctx)

                    // Stream live state updates while the Control Center / Device Controls panel is open
                    collectJob = serviceScope.launch {
                        var lastState = initialState
                        QcyBleManager.state.collect { newState ->
                            if (cancelled.get()) return@collect
                            if (newState != lastState) {
                                lastState = newState
                                validIds.forEach { id ->
                                    if (!cancelled.get()) {
                                        subscriber.onNext(buildStatefulControl(ctx, id, newState))
                                    }
                                }
                            }
                        }
                    }
                }

                override fun cancel() {
                    cancelled.set(true)
                    collectJob?.cancel()
                    collectJob = null
                }
            })
        }
    }

    override fun performControlAction(
        controlId: String,
        action: ControlAction,
        consumer: Consumer<Int>
    ) {
        val ctx = applicationContext
        QcyBleManager.init(ctx)

        if (controlId !in QcyDeviceControlsLogic.ALL_CONTROL_IDS) {
            consumer.accept(ControlAction.RESPONSE_FAIL)
            return
        }

        val state = QcyBleManager.state.value
        when (controlId) {
            QcyDeviceControlsLogic.ID_ANC -> when (action) {
                is BooleanAction -> {
                    val targetMode = if (action.newState) AncCycleMode.ANC else AncCycleMode.OFF
                    QcyBleManager.setAncCycleMode(targetMode)
                }
                is FloatAction -> {
                    val lvl = action.newValue.roundToInt().coerceIn(1, 3)
                    val cmdId = QcyDeviceControlsLogic.resolveAncCmdIdForSliderLevel(
                        level = lvl,
                        currentCmdId = state.ancCmdId,
                        preferredAncCmdId = state.preferredAncCmdId
                    )
                    QcyBleManager.setAncCmdId(cmdId)
                }
            }

            QcyDeviceControlsLogic.ID_AMBIENT -> when (action) {
                is BooleanAction -> {
                    val targetMode = if (action.newState) AncCycleMode.AMBIENT else AncCycleMode.OFF
                    QcyBleManager.setAncCycleMode(targetMode)
                }
                is FloatAction -> {
                    val lvl = action.newValue.roundToInt().coerceIn(1, QcyProtocol.AMBIENT_LEVEL_COUNT)
                    QcyBleManager.setAncCmdId(QcyProtocol.ambientCmdIdForLevel(lvl))
                }
            }

            QcyDeviceControlsLogic.ID_SLEEP_MODE -> {
                val target = (action as? BooleanAction)?.newState ?: !state.sleepModeEnabled
                QcyBleManager.setSleepModeAndEnsureConnected(target)
            }

            QcyDeviceControlsLogic.ID_GAME_MODE -> {
                val target = (action as? BooleanAction)?.newState ?: !state.lowLatencyEnabled
                QcyBleManager.setLowLatencyAndEnsureConnected(target)
            }

            QcyDeviceControlsLogic.ID_ANC_CYCLE -> {
                QcyBleManager.cycleAncModeAndEnsureConnected()
            }

            QcyDeviceControlsLogic.ID_BATTERY -> {
                if (state.connected) {
                    QcyBleManager.refreshBatteryAndVersion()
                } else {
                    QcyBleManager.connectOrAutoScan()
                }
            }
        }

        consumer.accept(ControlAction.RESPONSE_OK)
    }
}
