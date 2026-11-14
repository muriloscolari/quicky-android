package me.muriloscolari.quicky.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.muriloscolari.quicky.BuildConfig
import me.muriloscolari.quicky.ble.AncCycleMode
import me.muriloscolari.quicky.ble.AncSubScene
import me.muriloscolari.quicky.ble.FindEarbudSide
import me.muriloscolari.quicky.ble.QcyKeyAction
import me.muriloscolari.quicky.ble.QcyKeyId
import me.muriloscolari.quicky.ble.QcyProtocol
import me.muriloscolari.quicky.ble.QcyUiState
import me.muriloscolari.quicky.ble.UiStyle
import me.muriloscolari.quicky.update.ReleaseInfo
import me.muriloscolari.quicky.update.UpdateSource
import kotlin.math.roundToInt

private val AccentBlue = Color(0xFF3478F6)
private val AccentGreen = Color(0xFF28C76F)
private val TextPrimary = Color(0xFFF2F4F8)
private val TextSecondary = Color(0xFF8C95A6)
private val DividerSubtle = Color(0x14FFFFFF)

@Composable
fun QuickyScreen(
    state: QcyUiState,
    checkingUpdate: Boolean,
    downloadingPercent: Int?,
    availableRelease: ReleaseInfo?,
    updateStatusText: String,
    batteryUnrestricted: Boolean,
    companionLinked: Boolean,
    isXiaomiDevice: Boolean,
    companionStatusNote: String?,
    onScanClick: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnectClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onCycleAncClick: () -> Unit,
    onSelectAncMode: (AncCycleMode) -> Unit,
    onSelectAncCmdId: (Int) -> Unit,
    onToggleSleepMode: (Boolean) -> Unit,
    onToggleAutoSleepOnConnect: (Boolean) -> Unit,
    onToggleLowLatency: (Boolean) -> Unit,
    onToggleLdac: (Boolean) -> Unit,
    onToggleDualConnect: (Boolean) -> Unit,
    onSetKeyMapping: (Int, Int) -> Unit,
    onStartFindEarbuds: (FindEarbudSide) -> Unit,
    onStopFindEarbuds: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onRequestBatteryUnrestricted: () -> Unit,
    onRequestCompanionLink: () -> Unit,
    onOpenXiaomiAutostart: () -> Unit,
    onSelectUiStyle: (UiStyle) -> Unit,
    onSelectUpdateSource: (UpdateSource) -> Unit,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckUpdateNow: () -> Unit,
    onDownloadAndInstallUpdate: (ReleaseInfo) -> Unit
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val isXiaomi = state.uiStyle == UiStyle.XIAOMI_HYPEROS

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isXiaomi) Color(0xFF0A0C12) else Color(0xFF121316))
    ) {
        // Refined, subtle HyperOS atmospheric top glow (no visual clutter)
        if (isXiaomi) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(110.dp)
            ) {
                drawCircle(
                    color = AccentBlue.copy(alpha = 0.15f),
                    radius = size.minDimension * 0.58f,
                    center = Offset(size.width * 0.5f, size.height * 0.08f)
                )
            }
        }

        AnimatedContent(
            targetState = showSettings,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(150)) },
            label = "pageTransition"
        ) { inSettings ->
            if (inSettings) {
                SettingsPage(
                    state = state,
                    isXiaomi = isXiaomi,
                    checkingUpdate = checkingUpdate,
                    downloadingPercent = downloadingPercent,
                    availableRelease = availableRelease,
                    updateStatusText = updateStatusText,
                    batteryUnrestricted = batteryUnrestricted,
                    companionLinked = companionLinked,
                    isXiaomiDevice = isXiaomiDevice,
                    companionStatusNote = companionStatusNote,
                    onBackClick = { showSettings = false },
                    onToggleNotifications = onToggleNotifications,
                    onRequestBatteryUnrestricted = onRequestBatteryUnrestricted,
                    onRequestCompanionLink = onRequestCompanionLink,
                    onOpenXiaomiAutostart = onOpenXiaomiAutostart,
                    onSelectUiStyle = onSelectUiStyle,
                    onSelectUpdateSource = onSelectUpdateSource,
                    onToggleAutoCheckUpdates = onToggleAutoCheckUpdates,
                    onCheckUpdateNow = onCheckUpdateNow,
                    onDownloadAndInstallUpdate = onDownloadAndInstallUpdate
                )
            } else {
                MainControlsPage(
                    state = state,
                    isXiaomi = isXiaomi,
                    hasUpdateBadge = availableRelease != null,
                    onOpenSettings = { showSettings = true },
                    onScanClick = onScanClick,
                    onConnectDevice = onConnectDevice,
                    onDisconnectClick = onDisconnectClick,
                    onRefreshClick = onRefreshClick,
                    onCycleAncClick = onCycleAncClick,
                    onSelectAncMode = onSelectAncMode,
                    onSelectAncCmdId = onSelectAncCmdId,
                    onToggleSleepMode = onToggleSleepMode,
                    onToggleAutoSleepOnConnect = onToggleAutoSleepOnConnect,
                    onToggleLowLatency = onToggleLowLatency,
                    onToggleLdac = onToggleLdac,
                    onToggleDualConnect = onToggleDualConnect,
                    onSetKeyMapping = onSetKeyMapping,
                    onStartFindEarbuds = onStartFindEarbuds,
                    onStopFindEarbuds = onStopFindEarbuds
                )
            }
        }
    }
}

@Composable
private fun MainControlsPage(
    state: QcyUiState,
    isXiaomi: Boolean,
    hasUpdateBadge: Boolean,
    onOpenSettings: () -> Unit,
    onScanClick: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnectClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onCycleAncClick: () -> Unit,
    onSelectAncMode: (AncCycleMode) -> Unit,
    onSelectAncCmdId: (Int) -> Unit,
    onToggleSleepMode: (Boolean) -> Unit,
    onToggleAutoSleepOnConnect: (Boolean) -> Unit,
    onToggleLowLatency: (Boolean) -> Unit,
    onToggleLdac: (Boolean) -> Unit,
    onToggleDualConnect: (Boolean) -> Unit,
    onSetKeyMapping: (Int, Int) -> Unit,
    onStartFindEarbuds: (FindEarbudSide) -> Unit,
    onStopFindEarbuds: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Minimal Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.modelTitle,
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (state.connected) AccentGreen else Color(0xFF5E6675))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.statusMessage,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.connected) {
                    SmallCircleButton(isXiaomi = isXiaomi, onClick = onRefreshClick) {
                        Text("↻", color = TextPrimary, fontSize = 16.sp)
                    }
                }
                Box {
                    SmallCircleButton(isXiaomi = isXiaomi, onClick = onOpenSettings) {
                        SettingsGearIcon()
                    }
                    if (hasUpdateBadge) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(AccentBlue)
                        )
                    }
                }
            }
        }

        // Unified Battery Card
        RefinedSurfaceCard(isXiaomi = isXiaomi) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BatteryColumn(
                    label = "Esquerdo",
                    level = state.battery.left,
                    charging = state.battery.leftCharging,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(DividerSubtle)
                )
                BatteryColumn(
                    label = "Direito",
                    level = state.battery.right,
                    charging = state.battery.rightCharging,
                    modifier = Modifier.weight(1f)
                )
                if (state.battery.caseLevel > 0 || state.battery.caseCharging) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(44.dp)
                            .background(DividerSubtle)
                    )
                    BatteryColumn(
                        label = "Estojo",
                        level = state.battery.caseLevel,
                        charging = state.battery.caseCharging,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Connection / Discovery Card (only when disconnected)
        if (!state.connected) {
            RefinedSurfaceCard(isXiaomi = isXiaomi) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.scanning) "Procurando fones..." else "Fone desconectado",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Retire os fones do estojo para conectar",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    SoftPillButton(
                        text = if (state.scanning) "Buscando" else "Procurar",
                        active = true,
                        onClick = onScanClick
                    )
                }

                if (state.devices.isNotEmpty()) {
                    HorizontalDivider(color = DividerSubtle)
                    state.devices.forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dev.modelTitle,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${dev.bleAddress} · L ${dev.battery.left}% R ${dev.battery.right}%",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            SoftPillButton(
                                text = "Conectar",
                                active = false,
                                onClick = { onConnectDevice(dev.bleAddress) }
                            )
                        }
                    }
                }
            }
        }

        // Noise Control Card (ANC / Ambiente / OFF)
        NoiseControlSection(
            state = state,
            isXiaomi = isXiaomi,
            onCycleAncClick = onCycleAncClick,
            onSelectAncMode = onSelectAncMode,
            onSelectAncCmdId = onSelectAncCmdId
        )

        // Touch & Audio Preferences Card (Sleep Mode + Auto-Sleep on Connect + Low Latency + Dual Connect + LDAC)
        RefinedSurfaceCard(isXiaomi = isXiaomi) {
            SettingToggleRow(
                title = "Ignorar toques na lateral",
                subtitle = "Modo Sono · desativa os comandos de toque do fone",
                checked = state.sleepModeEnabled,
                onCheckedChange = onToggleSleepMode
            )

            HorizontalDivider(color = DividerSubtle)

            SettingToggleRow(
                title = "Bloquear toques ao conectar",
                subtitle = "Ativa o Modo Sono sozinho sempre que conectar no celular",
                checked = state.autoSleepOnConnect,
                onCheckedChange = onToggleAutoSleepOnConnect
            )

            HorizontalDivider(color = DividerSubtle)

            SettingToggleRow(
                title = "Modo Jogo",
                subtitle = "Baixa latência para jogos e vídeos",
                checked = state.lowLatencyEnabled,
                onCheckedChange = onToggleLowLatency
            )

            HorizontalDivider(color = DividerSubtle)

            SettingToggleRow(
                title = "Conexão Dupla (Multiponto)",
                subtitle = "Conectar em 2 aparelhos · desativa LDAC e reinicia o fone",
                checked = state.dualConnectEnabled,
                onCheckedChange = onToggleDualConnect
            )

            HorizontalDivider(color = DividerSubtle)

            SettingToggleRow(
                title = "Áudio HD (LDAC)",
                subtitle = "Alta definição · desativa Conexão Dupla e reinicia o fone",
                checked = state.ldacEnabled,
                onCheckedChange = onToggleLdac
            )
        }

        // Key / Gesture Customization Card
        TouchCustomizationCard(
            state = state,
            isXiaomi = isXiaomi,
            onSetKeyMapping = onSetKeyMapping
        )

        // Find Earbuds Card
        FindEarbudsCard(
            state = state,
            isXiaomi = isXiaomi,
            onStartFindEarbuds = onStartFindEarbuds,
            onStopFindEarbuds = onStopFindEarbuds
        )

        if (state.connected) {
            Text(
                text = "Desconectar fone",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDisconnectClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoiseControlSection(
    state: QcyUiState,
    isXiaomi: Boolean,
    onCycleAncClick: () -> Unit,
    onSelectAncMode: (AncCycleMode) -> Unit,
    onSelectAncCmdId: (Int) -> Unit
) {
    RefinedSurfaceCard(isXiaomi = isXiaomi) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Controle de ruído",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Ciclar ↻",
                color = AccentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onCycleAncClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Clean Capsule Segmented Control (HyperOS / Material style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (isXiaomi) 18.dp else 12.dp))
                .background(Color.Black.copy(alpha = 0.28f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AncCycleMode.entries.forEach { mode ->
                val selected = state.ancCycleMode == mode
                val bg by animateColorAsState(
                    targetValue = if (selected) AccentBlue else Color.Transparent,
                    animationSpec = tween(180),
                    label = "ancSeg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(if (isXiaomi) 14.dp else 9.dp))
                        .background(bg)
                        .clickable { onSelectAncMode(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        color = if (selected) Color.White else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // Sub-scenes and intensity slider when ANC is active, or 7-step level slider for Ambient
        if (state.ancCycleMode == AncCycleMode.ANC) {
            val activeScene = QcyProtocol.subSceneForCmdId(state.ancCmdId)
            val currentAncLevel = activeScene?.levelFromCmdId(state.ancCmdId)

            HorizontalDivider(color = DividerSubtle)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AncSubScene.entries.forEach { scene ->
                    val active = state.ancCmdId in scene.startCmdId..scene.endCmdId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(if (isXiaomi) 14.dp else 8.dp))
                            .background(
                                if (active) AccentBlue.copy(alpha = 0.22f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (active) AccentBlue.copy(alpha = 0.6f) else Color.Transparent,
                                shape = RoundedCornerShape(if (isXiaomi) 14.dp else 8.dp)
                            )
                            .clickable {
                                val targetCmd = if (scene.levelCount > 1 && currentAncLevel != null) {
                                    scene.cmdIdForLevel(currentAncLevel)
                                } else {
                                    scene.defaultCmdId
                                }
                                onSelectAncCmdId(targetCmd)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = scene.label,
                            color = if (active) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            if (activeScene != null && activeScene.levelCount > 1 && currentAncLevel != null) {
                NoiseLevelSlider(
                    title = "Intensidade · ${activeScene.label}",
                    currentLevel = currentAncLevel,
                    levelCount = activeScene.levelCount,
                    ancCmdId = state.ancCmdId,
                    levelText = { lvl ->
                        when (lvl) {
                            1 -> "Suave (1/${activeScene.levelCount})"
                            2 -> "Médio (2/${activeScene.levelCount})"
                            else -> "Forte ($lvl/${activeScene.levelCount})"
                        }
                    },
                    onCommitLevel = { lvl -> onSelectAncCmdId(activeScene.cmdIdForLevel(lvl)) }
                )
            }
        } else if (state.ancCycleMode == AncCycleMode.AMBIENT) {
            HorizontalDivider(color = DividerSubtle)
            NoiseLevelSlider(
                title = "Nível de transparência",
                currentLevel = QcyProtocol.ambientLevelFromCmdId(state.ancCmdId),
                levelCount = QcyProtocol.AMBIENT_LEVEL_COUNT,
                ancCmdId = state.ancCmdId,
                levelText = { lvl -> "Nível $lvl/${QcyProtocol.AMBIENT_LEVEL_COUNT}" },
                onCommitLevel = { lvl -> onSelectAncCmdId(QcyProtocol.ambientCmdIdForLevel(lvl)) }
            )
        }
    }
}

@Composable
private fun NoiseLevelSlider(
    title: String,
    currentLevel: Int,
    levelCount: Int,
    ancCmdId: Int,
    levelText: (Int) -> String,
    onCommitLevel: (Int) -> Unit
) {
    var draggingLevel by remember(ancCmdId) { mutableStateOf<Float?>(null) }
    val displayLevel = (draggingLevel?.roundToInt() ?: currentLevel).coerceIn(1, levelCount)
    val sliderValue = draggingLevel ?: currentLevel.toFloat()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = levelText(displayLevel),
                color = AccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { draggingLevel = it },
            onValueChangeFinished = {
                val targetLevel = (draggingLevel?.roundToInt() ?: currentLevel).coerceIn(1, levelCount)
                draggingLevel = null
                if (targetLevel != currentLevel) {
                    onCommitLevel(targetLevel)
                }
            },
            valueRange = 1f..levelCount.toFloat(),
            steps = (levelCount - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = AccentBlue,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f),
                activeTickColor = Color.White.copy(alpha = 0.55f),
                inactiveTickColor = Color.White.copy(alpha = 0.25f)
            )
        )
    }
}

@Composable
private fun TouchCustomizationCard(
    state: QcyUiState,
    isXiaomi: Boolean,
    onSetKeyMapping: (Int, Int) -> Unit
) {
    var expandedTaps by rememberSaveable { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<QcyKeyId?>(null) }

    val leftLongDisabled = state.keyMappings[QcyKeyId.LEFT_LONG.keyId] == QcyKeyAction.NONE.funcId
    val rightLongDisabled = state.keyMappings[QcyKeyId.RIGHT_LONG.keyId] == QcyKeyAction.NONE.funcId

    if (editingKey != null) {
        val targetKey = editingKey!!
        val currentFunc = state.keyMappings[targetKey.keyId] ?: targetKey.defaultFuncId
        AlertDialog(
            onDismissRequest = { editingKey = null },
            containerColor = Color(0xFF191D26),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = targetKey.label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    QcyKeyAction.entries.forEach { action ->
                        val selected = currentFunc == action.funcId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) AccentBlue.copy(alpha = 0.16f) else Color.Transparent
                                )
                                .clickable {
                                    onSetKeyMapping(targetKey.keyId, action.funcId)
                                    editingKey = null
                                }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = action.label,
                                color = if (selected) AccentBlue else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            RadioDot(selected = selected)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingKey = null }) {
                    Text("Fechar", color = TextSecondary)
                }
            }
        )
    }

    RefinedSurfaceCard(isXiaomi = isXiaomi) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Personalizar toques",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (state.sleepModeEnabled) {
                        "Modo Sono ativo bloqueia todos os toques (incl. volume 3x)"
                    } else {
                        "Ajuste gestos sem bloquear o toque triplo de volume"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            SoftPillButton(
                text = if (expandedTaps) "Ocultar" else "1/2/3 toques",
                active = expandedTaps,
                onClick = { expandedTaps = !expandedTaps }
            )
        }

        HorizontalDivider(color = DividerSubtle)

        SettingToggleRow(
            title = "Desativar segurar Esquerdo",
            subtitle = "Remove Modo Jogo ao segurar · experimental (depende do firmware)",
            checked = leftLongDisabled,
            onCheckedChange = { disable ->
                onSetKeyMapping(
                    QcyKeyId.LEFT_LONG.keyId,
                    if (disable) QcyKeyAction.NONE.funcId else QcyKeyId.LEFT_LONG.defaultFuncId
                )
            }
        )

        HorizontalDivider(color = DividerSubtle)

        SettingToggleRow(
            title = "Desativar segurar Direito",
            subtitle = "Remove troca de ANC ao segurar · experimental (depende do firmware)",
            checked = rightLongDisabled,
            onCheckedChange = { disable ->
                onSetKeyMapping(
                    QcyKeyId.RIGHT_LONG.keyId,
                    if (disable) QcyKeyAction.NONE.funcId else QcyKeyId.RIGHT_LONG.defaultFuncId
                )
            }
        )

        if (expandedTaps) {
            val tapKeys = listOf(
                QcyKeyId.LEFT_SINGLE,
                QcyKeyId.RIGHT_SINGLE,
                QcyKeyId.LEFT_DOUBLE,
                QcyKeyId.RIGHT_DOUBLE,
                QcyKeyId.LEFT_TRIPLE,
                QcyKeyId.RIGHT_TRIPLE
            )
            tapKeys.forEach { key ->
                val currentFuncId = state.keyMappings[key.keyId] ?: key.defaultFuncId
                HorizontalDivider(color = DividerSubtle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingKey = key }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = key.label,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = QcyKeyAction.labelFor(currentFuncId),
                        color = AccentBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FindEarbudsCard(
    state: QcyUiState,
    isXiaomi: Boolean,
    onStartFindEarbuds: (FindEarbudSide) -> Unit,
    onStopFindEarbuds: () -> Unit
) {
    var selectedSide by rememberSaveable { mutableStateOf(FindEarbudSide.BOTH) }
    var pendingConfirmSide by remember { mutableStateOf<FindEarbudSide?>(null) }
    val isRinging = state.findingSide != null

    if (pendingConfirmSide != null) {
        AlertDialog(
            onDismissRequest = { pendingConfirmSide = null },
            containerColor = Color(0xFF191D26),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "Os fones estão fora do ouvido?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Para proteger sua audição, retire os fones do ouvido antes de tocar o alerta sonoro de localização.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val side = pendingConfirmSide ?: FindEarbudSide.BOTH
                        pendingConfirmSide = null
                        onStartFindEarbuds(side)
                    }
                ) {
                    Text("Emitir som", color = AccentBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmSide = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    RefinedSurfaceCard(isXiaomi = isXiaomi) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Encontrar fones",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isRinging) {
                        "Emitindo bip no lado ${state.findingSide?.label}..."
                    } else {
                        "Toca um bip agudo para localizar os fones por perto"
                    },
                    color = if (isRinging) AccentBlue else TextSecondary,
                    fontSize = 12.sp
                )
            }

            SoftPillButton(
                text = if (isRinging) "Parar som" else "Tocar som",
                active = isRinging,
                onClick = {
                    if (isRinging) {
                        onStopFindEarbuds()
                    } else {
                        pendingConfirmSide = selectedSide
                    }
                }
            )
        }

        // L / Ambos / R channel selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (isXiaomi) 16.dp else 10.dp))
                .background(Color.Black.copy(alpha = 0.26f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FindEarbudSide.entries.forEach { side ->
                val activeSide = state.findingSide ?: selectedSide
                val selected = activeSide == side
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(if (isXiaomi) 12.dp else 8.dp))
                        .background(if (selected) AccentBlue else Color.Transparent)
                        .clickable {
                            selectedSide = side
                            if (isRinging) {
                                onStartFindEarbuds(side)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = side.label,
                        color = if (selected) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    state: QcyUiState,
    isXiaomi: Boolean,
    checkingUpdate: Boolean,
    downloadingPercent: Int?,
    availableRelease: ReleaseInfo?,
    updateStatusText: String,
    batteryUnrestricted: Boolean,
    companionLinked: Boolean,
    isXiaomiDevice: Boolean,
    companionStatusNote: String?,
    onBackClick: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onRequestBatteryUnrestricted: () -> Unit,
    onRequestCompanionLink: () -> Unit,
    onOpenXiaomiAutostart: () -> Unit,
    onSelectUiStyle: (UiStyle) -> Unit,
    onSelectUpdateSource: (UpdateSource) -> Unit,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckUpdateNow: () -> Unit,
    onDownloadAndInstallUpdate: (ReleaseInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallCircleButton(isXiaomi = isXiaomi, onClick = onBackClick) {
                Text("←", color = TextPrimary, fontSize = 18.sp)
            }
            Text(
                text = "Configurações",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Background Reliability Card (important for HyperOS / Poco & Android 14+)
        RefinedSurfaceCard(isXiaomi = isXiaomi) {
            Text(
                text = "Execução em 2º plano",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Garante que o botão da barra e o Modo Sono automático funcionem mesmo após horas sem abrir o app.",
                color = TextSecondary,
                fontSize = 12.sp
            )

            // 1. Companion Device Association
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = "Vínculo de acessório (Sistema)",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (companionLinked) {
                            "Vinculado · Android acorda o app ao conectar o fone"
                        } else {
                            companionStatusNote ?: "Recomendado · permite despertar sem notificação fixa"
                        },
                        color = if (companionLinked) AccentGreen else TextSecondary,
                        fontSize = 12.sp
                    )
                }
                SoftPillButton(
                    text = if (companionLinked) "Vinculado" else "Vincular",
                    active = !companionLinked,
                    onClick = onRequestCompanionLink
                )
            }

            HorizontalDivider(color = DividerSubtle)

            // 2. Battery optimization exemption
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = "Bateria sem restrições",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (batteryUnrestricted) {
                            "Ativo · sistema não encerra o Bluetooth em espera"
                        } else {
                            "Restrito · pode impedir conexão automática em 2º plano"
                        },
                        color = if (batteryUnrestricted) AccentGreen else Color(0xFFFF9F43),
                        fontSize = 12.sp
                    )
                }
                SoftPillButton(
                    text = if (batteryUnrestricted) "Ativo" else "Liberar",
                    active = !batteryUnrestricted,
                    onClick = onRequestBatteryUnrestricted
                )
            }

            // 3. Xiaomi HyperOS / MIUI Autostart shortcut
            if (isXiaomiDevice) {
                HorizontalDivider(color = DividerSubtle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "Início Automático (HyperOS)",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Ative nas configurações da Xiaomi para iniciar ao conectar o áudio",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    SoftPillButton(
                        text = "Abrir",
                        active = false,
                        onClick = onOpenXiaomiAutostart
                    )
                }
            }
        }

        // Notifications & System Shortcut Card
        RefinedSurfaceCard(isXiaomi = isXiaomi) {
            SettingToggleRow(
                title = "Notificações do aplicativo",
                subtitle = "Exibir controles rápidos fixos na barra de notificações",
                checked = state.notificationsEnabled,
                onCheckedChange = onToggleNotifications
            )

            HorizontalDivider(color = DividerSubtle)

            Text(
                text = "Dica: toque no botão 'QCY Ruído' nas Configurações Rápidas do Android para ciclar ANC/Ambiente/OFF, ou segure o botão para abrir este aplicativo direto.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // UI Style Card
        RefinedSurfaceCard(isXiaomi = isXiaomi) {
            Text(
                text = "Aparência da interface",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            UiStyle.entries.forEachIndexed { idx, style ->
                if (idx > 0) HorizontalDivider(color = DividerSubtle)
                val selected = state.uiStyle == style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectUiStyle(style) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = style.label,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = style.subtitle,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    RadioDot(selected = selected)
                }
            }
        }

        // Updates Card
        RefinedSurfaceCard(isXiaomi = isXiaomi) {
            Text(
                text = "Atualizações do aplicativo",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Escolha de onde o aplicativo deve buscar novas versões",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(if (isXiaomi) 16.dp else 10.dp))
                    .background(Color.Black.copy(alpha = 0.26f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                UpdateSource.entries.forEach { source ->
                    val selected = state.updateSource == source
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(if (isXiaomi) 12.dp else 8.dp))
                            .background(if (selected) AccentBlue else Color.Transparent)
                            .clickable { onSelectUpdateSource(source) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${source.label} (${source.hostLabel})",
                            color = if (selected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(color = DividerSubtle)

            SettingToggleRow(
                title = "Verificar ao abrir o aplicativo",
                subtitle = "Versão atual: v${BuildConfig.VERSION_NAME} · $updateStatusText",
                checked = state.autoCheckUpdates,
                onCheckedChange = onToggleAutoCheckUpdates
            )

            if (downloadingPercent != null) {
                LinearProgressIndicator(
                    progress = { downloadingPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = AccentBlue,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCheckUpdateNow,
                    enabled = !checkingUpdate && downloadingPercent == null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(if (isXiaomi) 14.dp else 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = if (checkingUpdate) "Verificando..." else "Procurar atualização",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }

                if (availableRelease != null) {
                    Button(
                        onClick = { onDownloadAndInstallUpdate(availableRelease) },
                        enabled = downloadingPercent == null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(if (isXiaomi) 14.dp else 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text(
                            text = "Instalar ${availableRelease.tagName}",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RefinedSurfaceCard(
    isXiaomi: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(if (isXiaomi) 24.dp else 14.dp)
    if (isXiaomi) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color(0xFF171B24).copy(alpha = 0.78f))
                .border(0.7.dp, Color.White.copy(alpha = 0.08f), shape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = Color(0xFF1C1E24),
            border = BorderStroke(1.dp, Color(0xFF282C35))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}

@Composable
private fun BatteryColumn(
    label: String,
    level: Int,
    charging: Boolean,
    modifier: Modifier = Modifier
) {
    val valid = level in 0..100
    val barColor = when {
        !valid -> Color(0xFF3E4554)
        charging -> AccentBlue
        level <= 15 -> Color(0xFFEA5455)
        level <= 25 -> Color(0xFFFF9F43)
        else -> AccentGreen
    }

    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (valid) "$level%" else "--",
                color = TextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (charging) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "⚡", color = AccentBlue, fontSize = 12.sp)
            }
        }
        LinearProgressIndicator(
            progress = { if (valid) (level / 100f) else 0f },
            modifier = Modifier
                .width(56.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = barColor,
            trackColor = Color.White.copy(alpha = 0.08f)
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentBlue,
                uncheckedThumbColor = Color(0xFF9AA4B5),
                uncheckedTrackColor = Color(0xFF262B36)
            )
        )
    }
}

@Composable
private fun SmallCircleButton(
    isXiaomi: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (isXiaomi) Color(0xFF1A1F2B) else Color(0xFF222630))
            .border(0.7.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SoftPillButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (active) AccentBlue else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(
                width = if (selected) 6.dp else 1.5.dp,
                color = if (selected) AccentBlue else Color(0xFF555E6F),
                shape = CircleShape
            )
    )
}

@Composable
private fun SettingsGearIcon() {
    Canvas(modifier = Modifier.size(17.dp)) {
        val r = size.minDimension * 0.28f
        drawCircle(
            color = TextPrimary,
            radius = r,
            style = Stroke(width = 3.2f)
        )
        val outer = size.minDimension * 0.46f
        val inner = size.minDimension * 0.34f
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val cos = kotlin.math.cos(angle).toFloat()
            val sin = kotlin.math.sin(angle).toFloat()
            drawLine(
                color = TextPrimary,
                start = Offset(center.x + cos * inner, center.y + sin * inner),
                end = Offset(center.x + cos * outer, center.y + sin * outer),
                strokeWidth = 3.0f,
                cap = StrokeCap.Round
            )
        }
    }
}
