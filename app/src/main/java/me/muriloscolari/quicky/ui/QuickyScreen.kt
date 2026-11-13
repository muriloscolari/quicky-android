package me.muriloscolari.quicky.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.muriloscolari.quicky.BuildConfig
import me.muriloscolari.quicky.ble.AncCycleMode
import me.muriloscolari.quicky.ble.AncSubScene
import me.muriloscolari.quicky.ble.QcyUiState
import me.muriloscolari.quicky.ble.UiStyle
import me.muriloscolari.quicky.update.ReleaseInfo
import me.muriloscolari.quicky.update.UpdateSource

private val XiaomiBlue = Color(0xFF2878FF)
private val XiaomiCyan = Color(0xFF00D2FF)
private val XiaomiViolet = Color(0xFF7B42F6)
private val XiaomiGreen = Color(0xFF00C48C)
private val TextPrimary = Color(0xFFF5F7FA)
private val TextSecondary = Color(0xFF9AA4B5)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickyScreen(
    state: QcyUiState,
    checkingUpdate: Boolean,
    downloadingPercent: Int?,
    availableRelease: ReleaseInfo?,
    updateStatusText: String,
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
    onSelectUiStyle: (UiStyle) -> Unit,
    onSelectUpdateSource: (UpdateSource) -> Unit,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckUpdateNow: () -> Unit,
    onDownloadAndInstallUpdate: (ReleaseInfo) -> Unit
) {
    val isXiaomi = state.uiStyle == UiStyle.XIAOMI_HYPEROS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isXiaomi) Color(0xFF080B12) else Color(0xFF111318))
    ) {
        // Xiaomi HyperOS Ambient Blurred Orbs Background
        if (isXiaomi) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(88.dp)
            ) {
                drawCircle(
                    color = XiaomiBlue.copy(alpha = 0.36f),
                    radius = size.minDimension * 0.48f,
                    center = Offset(size.width * 0.18f, size.height * 0.14f)
                )
                drawCircle(
                    color = XiaomiCyan.copy(alpha = 0.24f),
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.85f, size.height * 0.30f)
                )
                drawCircle(
                    color = XiaomiViolet.copy(alpha = 0.26f),
                    radius = size.minDimension * 0.50f,
                    center = Offset(size.width * 0.50f, size.height * 0.82f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header
            HeaderSection(
                state = state,
                isXiaomi = isXiaomi,
                onScanClick = onScanClick,
                onDisconnectClick = onDisconnectClick,
                onRefreshClick = onRefreshClick
            )

            // Quick Cycle Banner + Battery Pods
            BatterySection(state = state, isXiaomi = isXiaomi)

            // CRITICAL CARD: Sleep Mode (Ignore Touch Controls on Connect)
            SleepModeTouchLockCard(
                state = state,
                isXiaomi = isXiaomi,
                onToggleSleepMode = onToggleSleepMode,
                onToggleAutoSleepOnConnect = onToggleAutoSleepOnConnect,
                onToggleLowLatency = onToggleLowLatency
            )

            // Noise Control (ANC / Ambiente / OFF + Cycle Button + Sub-scenes)
            NoiseControlCard(
                state = state,
                isXiaomi = isXiaomi,
                onCycleAncClick = onCycleAncClick,
                onSelectAncMode = onSelectAncMode,
                onSelectAncCmdId = onSelectAncCmdId
            )

            // Discovered Devices (when disconnected or scanning)
            if (!state.connected) {
                DiscoveredDevicesCard(
                    state = state,
                    isXiaomi = isXiaomi,
                    onScanClick = onScanClick,
                    onConnectDevice = onConnectDevice
                )
            }

            // Settings: UI Style (Xiaomi Blur vs Material 3 Leve) & Auto-Updater (Forgejo vs GitLab)
            SettingsAndUpdaterCard(
                state = state,
                isXiaomi = isXiaomi,
                checkingUpdate = checkingUpdate,
                downloadingPercent = downloadingPercent,
                availableRelease = availableRelease,
                updateStatusText = updateStatusText,
                onSelectUiStyle = onSelectUiStyle,
                onSelectUpdateSource = onSelectUpdateSource,
                onToggleAutoCheckUpdates = onToggleAutoCheckUpdates,
                onCheckUpdateNow = onCheckUpdateNow,
                onDownloadAndInstallUpdate = onDownloadAndInstallUpdate
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AdaptiveGlassCard(
    isXiaomi: Boolean,
    modifier: Modifier = Modifier,
    highlightColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val corner = if (isXiaomi) 28.dp else 16.dp
    val shape = RoundedCornerShape(corner)

    if (isXiaomi) {
        val bgBrush = Brush.verticalGradient(
            colors = listOf(
                (highlightColor ?: Color.White).copy(alpha = if (highlightColor != null) 0.18f else 0.11f),
                Color.White.copy(alpha = 0.05f)
            )
        )
        val borderBrush = Brush.linearGradient(
            colors = listOf(
                (highlightColor ?: Color.White).copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.06f)
            )
        )
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bgBrush)
                .border(1.dp, borderBrush, shape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = Color(0xFF1B1E26),
            border = BorderStroke(1.dp, highlightColor?.copy(alpha = 0.5f) ?: Color(0xFF2B303C))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun HeaderSection(
    state: QcyUiState,
    isXiaomi: Boolean,
    onScanClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    AdaptiveGlassCard(
        isXiaomi = isXiaomi,
        highlightColor = if (state.connected) XiaomiBlue else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (state.connected) XiaomiGreen else if (state.connecting || state.scanning) XiaomiCyan else Color(0xFF636E7B))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isXiaomi) "HYPEROS AUDIO CENTER" else "QUICKY QCY CONTROL",
                        color = XiaomiCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.modelTitle,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.statusMessage,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.connected) {
                    PillActionButton(
                        text = "↻",
                        isXiaomi = isXiaomi,
                        active = false,
                        onClick = onRefreshClick
                    )
                    PillActionButton(
                        text = "Sair",
                        isXiaomi = isXiaomi,
                        active = false,
                        onClick = onDisconnectClick
                    )
                } else {
                    PillActionButton(
                        text = if (state.scanning) "Buscando..." else "Conectar",
                        isXiaomi = isXiaomi,
                        active = true,
                        onClick = onScanClick
                    )
                }
            }
        }
    }
}

@Composable
private fun BatterySection(state: QcyUiState, isXiaomi: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BatteryPod(
            title = "ESQUERDO",
            level = state.battery.left,
            charging = state.battery.leftCharging,
            isXiaomi = isXiaomi,
            modifier = Modifier.weight(1f)
        )
        BatteryPod(
            title = "DIREITO",
            level = state.battery.right,
            charging = state.battery.rightCharging,
            isXiaomi = isXiaomi,
            modifier = Modifier.weight(1f)
        )
        BatteryPod(
            title = "ESTOJO",
            level = state.battery.caseLevel,
            charging = state.battery.caseCharging,
            isXiaomi = isXiaomi,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BatteryPod(
    title: String,
    level: Int,
    charging: Boolean,
    isXiaomi: Boolean,
    modifier: Modifier = Modifier
) {
    val valid = level in 0..100
    val barColor = when {
        !valid -> Color(0xFF424A57)
        charging -> XiaomiCyan
        level <= 15 -> Color(0xFFFF4D4F)
        level <= 25 -> Color(0xFFFFA940)
        else -> XiaomiGreen
    }

    AdaptiveGlassCard(isXiaomi = isXiaomi, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            if (charging) {
                Text(text = "⚡", color = XiaomiCyan, fontSize = 11.sp)
            }
        }
        Text(
            text = if (valid) "$level%" else "--%",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        LinearProgressIndicator(
            progress = { if (valid) (level / 100f) else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = barColor,
            trackColor = Color.White.copy(alpha = 0.10f)
        )
    }
}

@Composable
private fun SleepModeTouchLockCard(
    state: QcyUiState,
    isXiaomi: Boolean,
    onToggleSleepMode: (Boolean) -> Unit,
    onToggleAutoSleepOnConnect: (Boolean) -> Unit,
    onToggleLowLatency: (Boolean) -> Unit
) {
    AdaptiveGlassCard(
        isXiaomi = isXiaomi,
        highlightColor = if (state.sleepModeEnabled || state.autoSleepOnConnect) XiaomiViolet else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CONTROLE DE TOQUE · MODO SONO",
                    color = XiaomiCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ignorar toques na lateral do fone",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Evita pausas ou trocas acidentais quando você encosta no fone",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = state.sleepModeEnabled,
                onCheckedChange = onToggleSleepMode,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = XiaomiBlue
                )
            )
        }

        // Auto-enable on connect toggle (Highlighted)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (isXiaomi) 18.dp else 12.dp))
                .background(
                    if (state.autoSleepOnConnect) XiaomiBlue.copy(alpha = 0.20f)
                    else Color.White.copy(alpha = 0.05f)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ativar sozinho ao conectar no celular",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Sempre que o fone conectar, envia o comando 0x10 para desativar os toques sozinho",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = state.autoSleepOnConnect,
                onCheckedChange = onToggleAutoSleepOnConnect,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = XiaomiGreen
                )
            )
        }

        // Low latency quick switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Modo Jogo (Baixa Latência)",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Reduz atraso de áudio em jogos e vídeos",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = state.lowLatencyEnabled,
                onCheckedChange = onToggleLowLatency,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = XiaomiBlue
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoiseControlCard(
    state: QcyUiState,
    isXiaomi: Boolean,
    onCycleAncClick: () -> Unit,
    onSelectAncMode: (AncCycleMode) -> Unit,
    onSelectAncCmdId: (Int) -> Unit
) {
    AdaptiveGlassCard(isXiaomi = isXiaomi) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CANCELAMENTO DE RUÍDO",
                    color = XiaomiCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Modo atual: ${state.ancCycleMode.label}",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            PillActionButton(
                text = "Ciclar: ${state.ancCycleMode.label} ↻",
                isXiaomi = isXiaomi,
                active = true,
                onClick = onCycleAncClick
            )
        }

        Text(
            text = "Dica: adicione o botão 'QCY Ruído' na barra rápida do Android (Quick Settings) para ciclar entre ANC, Ambiente e OFF com 1 toque.",
            color = TextSecondary,
            fontSize = 11.sp
        )

        // 3 Main Segmented Pills: ANC | Ambiente | OFF
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AncCycleMode.entries.forEach { mode ->
                val selected = state.ancCycleMode == mode
                val bg by animateColorAsState(
                    targetValue = if (selected) XiaomiBlue else Color.White.copy(alpha = 0.07f),
                    animationSpec = tween(200),
                    label = "ancPill"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(if (isXiaomi) 20.dp else 12.dp))
                        .background(bg)
                        .clickable { onSelectAncMode(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        color = if (selected) Color.White else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Sub-scenes when in ANC mode (MeloBuds Pro)
        if (state.ancCycleMode == AncCycleMode.ANC) {
            Text(
                text = "Cenário ANC (QCY MeloBuds Pro)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AncSubScene.entries.forEach { scene ->
                    val active = state.ancCmdId in scene.startCmdId..scene.endCmdId
                    PillActionButton(
                        text = scene.label,
                        isXiaomi = isXiaomi,
                        active = active,
                        onClick = { onSelectAncCmdId(scene.defaultCmdId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoveredDevicesCard(
    state: QcyUiState,
    isXiaomi: Boolean,
    onScanClick: () -> Unit,
    onConnectDevice: (String) -> Unit
) {
    AdaptiveGlassCard(isXiaomi = isXiaomi) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FONES QCY POR PERTO",
                color = XiaomiCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            PillActionButton(
                text = if (state.scanning) "Procurando..." else "Procurar QCY",
                isXiaomi = isXiaomi,
                active = false,
                onClick = onScanClick
            )
        }

        if (state.devices.isEmpty()) {
            Text(
                text = if (state.scanning) "Escaneando anúncios BLE QCY (0x521C)..."
                else "Retire os fones do estojo e toque em Procurar QCY.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        } else {
            state.devices.forEach { dev ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dev.modelTitle,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${dev.bleAddress} · L ${dev.battery.left}% R ${dev.battery.right}%",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    PillActionButton(
                        text = "Conectar",
                        isXiaomi = isXiaomi,
                        active = true,
                        onClick = { onConnectDevice(dev.bleAddress) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsAndUpdaterCard(
    state: QcyUiState,
    isXiaomi: Boolean,
    checkingUpdate: Boolean,
    downloadingPercent: Int?,
    availableRelease: ReleaseInfo?,
    updateStatusText: String,
    onSelectUiStyle: (UiStyle) -> Unit,
    onSelectUpdateSource: (UpdateSource) -> Unit,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckUpdateNow: () -> Unit,
    onDownloadAndInstallUpdate: (ReleaseInfo) -> Unit
) {
    AdaptiveGlassCard(isXiaomi = isXiaomi) {
        Text(
            text = "INTERFACE & ATUALIZAÇÕES AUTOMÁTICAS",
            color = XiaomiCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // UI Style Picker: Xiaomi HyperOS (Blur) vs Material 3 (Leve)
        Text(
            text = "Linguagem de Design da Interface",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UiStyle.entries.forEach { style ->
                val selected = state.uiStyle == style
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(if (isXiaomi) 18.dp else 12.dp))
                        .background(if (selected) XiaomiBlue else Color.White.copy(alpha = 0.06f))
                        .clickable { onSelectUiStyle(style) }
                        .padding(12.dp)
                ) {
                    Text(
                        text = style.label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = style.subtitle,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Update Source Picker: Forgejo vs GitLab
        Text(
            text = "Fonte de Atualização (Releases)",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UpdateSource.entries.forEach { source ->
                val selected = state.updateSource == source
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(if (isXiaomi) 18.dp else 12.dp))
                        .background(if (selected) XiaomiBlue else Color.White.copy(alpha = 0.06f))
                        .clickable { onSelectUpdateSource(source) }
                        .padding(12.dp)
                ) {
                    Text(
                        text = source.label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = source.hostLabel,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Procurar atualizações ao abrir o app",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Versão instalada: v${BuildConfig.VERSION_NAME} · $updateStatusText",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = state.autoCheckUpdates,
                onCheckedChange = onToggleAutoCheckUpdates,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = XiaomiBlue
                )
            )
        }

        if (downloadingPercent != null) {
            LinearProgressIndicator(
                progress = { downloadingPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = XiaomiCyan,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCheckUpdateNow,
                enabled = !checkingUpdate && downloadingPercent == null,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f))
            ) {
                Text(
                    text = if (checkingUpdate) "Consultando ${state.updateSource.label}..." else "Verificar no ${state.updateSource.label}",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
            }

            if (availableRelease != null) {
                Button(
                    onClick = { onDownloadAndInstallUpdate(availableRelease) },
                    enabled = downloadingPercent == null,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = XiaomiGreen)
                ) {
                    Text(
                        text = "Atualizar (${availableRelease.tagName})",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PillActionButton(
    text: String,
    isXiaomi: Boolean,
    active: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(if (isXiaomi) 99.dp else 10.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (active) XiaomiBlue else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
