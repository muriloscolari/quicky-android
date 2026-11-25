package me.muriloscolari.quicky.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.muriloscolari.quicky.BuildConfig
import me.muriloscolari.quicky.ble.AncCycleMode
import me.muriloscolari.quicky.ble.AncSubScene
import me.muriloscolari.quicky.ble.FindEarbudSide
import me.muriloscolari.quicky.ble.QcyEqPreset
import me.muriloscolari.quicky.ble.QcyKeyAction
import me.muriloscolari.quicky.ble.QcyKeyId
import me.muriloscolari.quicky.ble.QcyProtocol
import me.muriloscolari.quicky.ble.QcyUiState
import me.muriloscolari.quicky.ble.UiStyle
import me.muriloscolari.quicky.service.QcyDeviceControlsService
import me.muriloscolari.quicky.update.ReleaseInfo
import me.muriloscolari.quicky.update.UpdateSource
import kotlin.math.roundToInt

private enum class MaterialTab(val label: String, val icon: ImageVector) {
    CONTROLS("Controles", QuickyIcons.Headphones),
    SOUND("Som", QuickyIcons.Equalizer),
    SETTINGS("Ajustes", QuickyIcons.Settings)
}

@Composable
fun QuickyScreen(
    state: QcyUiState,
    checkingUpdate: Boolean,
    downloadingPercent: Int?,
    availableRelease: ReleaseInfo?,
    updateStatusText: String,
    batteryUnrestricted: Boolean,
    companionLinked: Boolean,
    overlayPermissionGranted: Boolean,
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
    onSelectEqPreset: (QcyEqPreset) -> Unit,
    onSetCustomEqBands: (List<Int>) -> Unit,
    onSetSoundBalance: (Int) -> Unit,
    onSetKeyMapping: (Int, Int) -> Unit,
    onStartFindEarbuds: (FindEarbudSide) -> Unit,
    onStopFindEarbuds: () -> Unit,
    onToggleAirpodsPopup: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
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
    if (state.uiStyle == UiStyle.APPLE_LIQUID_GLASS) {
        AppleQuickyScreen(
            state = state,
            checkingUpdate = checkingUpdate,
            downloadingPercent = downloadingPercent,
            availableRelease = availableRelease,
            updateStatusText = updateStatusText,
            batteryUnrestricted = batteryUnrestricted,
            companionLinked = companionLinked,
            overlayPermissionGranted = overlayPermissionGranted,
            isXiaomiDevice = isXiaomiDevice,
            companionStatusNote = companionStatusNote,
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
            onSelectEqPreset = onSelectEqPreset,
            onSetCustomEqBands = onSetCustomEqBands,
            onSetSoundBalance = onSetSoundBalance,
            onSetKeyMapping = onSetKeyMapping,
            onStartFindEarbuds = onStartFindEarbuds,
            onStopFindEarbuds = onStopFindEarbuds,
            onToggleAirpodsPopup = onToggleAirpodsPopup,
            onRequestOverlayPermission = onRequestOverlayPermission,
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
        return
    }

    MaterialYouQuickyScreen(
        state = state,
        checkingUpdate = checkingUpdate,
        downloadingPercent = downloadingPercent,
        availableRelease = availableRelease,
        updateStatusText = updateStatusText,
        batteryUnrestricted = batteryUnrestricted,
        companionLinked = companionLinked,
        overlayPermissionGranted = overlayPermissionGranted,
        isXiaomiDevice = isXiaomiDevice,
        companionStatusNote = companionStatusNote,
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
        onSelectEqPreset = onSelectEqPreset,
        onSetCustomEqBands = onSetCustomEqBands,
        onSetSoundBalance = onSetSoundBalance,
        onSetKeyMapping = onSetKeyMapping,
        onStartFindEarbuds = onStartFindEarbuds,
        onStopFindEarbuds = onStopFindEarbuds,
        onToggleAirpodsPopup = onToggleAirpodsPopup,
        onRequestOverlayPermission = onRequestOverlayPermission,
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
}

@Composable
private fun MaterialYouQuickyScreen(
    state: QcyUiState,
    checkingUpdate: Boolean,
    downloadingPercent: Int?,
    availableRelease: ReleaseInfo?,
    updateStatusText: String,
    batteryUnrestricted: Boolean,
    companionLinked: Boolean,
    overlayPermissionGranted: Boolean,
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
    onSelectEqPreset: (QcyEqPreset) -> Unit,
    onSetCustomEqBands: (List<Int>) -> Unit,
    onSetSoundBalance: (Int) -> Unit,
    onSetKeyMapping: (Int, Int) -> Unit,
    onStartFindEarbuds: (FindEarbudSide) -> Unit,
    onStopFindEarbuds: () -> Unit,
    onToggleAirpodsPopup: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
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
    var activeTab by rememberSaveable { mutableStateOf(MaterialTab.CONTROLS) }
    var editingKey by remember { mutableStateOf<QcyKeyId?>(null) }
    var pendingFindSide by remember { mutableStateOf<FindEarbudSide?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colorScheme = MaterialTheme.colorScheme

    BackHandler(enabled = activeTab != MaterialTab.CONTROLS) {
        activeTab = MaterialTab.CONTROLS
    }

    // Gesture mapping bottom sheet
    if (editingKey != null) {
        val targetKey = editingKey!!
        val currentFunc = state.keyMappings[targetKey.keyId] ?: targetKey.defaultFuncId
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { editingKey = null },
            sheetState = sheetState,
            containerColor = colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = targetKey.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
                Text(
                    text = "Escolha a ação executada ao usar este gesto",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                QcyKeyAction.entries.forEach { action ->
                    val selected = currentFunc == action.funcId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onSetKeyMapping(targetKey.keyId, action.funcId)
                                editingKey = null
                            },
                        color = if (selected) colorScheme.secondaryContainer else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurface
                            )
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    onSetKeyMapping(targetKey.keyId, action.funcId)
                                    editingKey = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Find Earbuds safety confirmation dialog
    if (pendingFindSide != null) {
        val side = pendingFindSide!!
        AlertDialog(
            onDismissRequest = { pendingFindSide = null },
            icon = {
                Icon(
                    imageVector = QuickyIcons.VolumeUp,
                    contentDescription = null,
                    tint = colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Os fones estão fora do ouvido?",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Para proteger sua audição, retire os fones do ouvido antes de tocar o alerta sonoro de localização."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingFindSide = null
                        onStartFindEarbuds(side)
                    }
                ) {
                    Text("Emitir som")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFindSide = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (activeTab) {
                                MaterialTab.CONTROLS -> state.modelTitle
                                MaterialTab.SOUND -> "Som"
                                MaterialTab.SETTINGS -> "Ajustes"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    if (activeTab == MaterialTab.CONTROLS) {
                        StatusChip(
                            connected = state.connected,
                            scanning = state.scanning || state.connecting
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (state.connected) {
                            FilledTonalIconButton(
                                onClick = onRefreshClick,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = QuickyIcons.Refresh,
                                    contentDescription = "Atualizar",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    } else if (activeTab == MaterialTab.SETTINGS) {
                        Surface(
                            color = colorScheme.surfaceContainerHigh,
                            shape = CircleShape,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    scrolledContainerColor = colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                MaterialTab.entries.forEach { tab ->
                    val selected = activeTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { activeTab = tab },
                        icon = {
                            if (tab == MaterialTab.SETTINGS && availableRelease != null) {
                                BadgedBox(badge = { Badge() }) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = activeTab,
            animationSpec = tween(200),
            label = "materialTabCrossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tab ->
            when (tab) {
                MaterialTab.CONTROLS -> MaterialControlsTab(
                    state = state,
                    onScanClick = onScanClick,
                    onConnectDevice = onConnectDevice,
                    onDisconnectClick = onDisconnectClick,
                    onCycleAncClick = onCycleAncClick,
                    onSelectAncMode = onSelectAncMode,
                    onSelectAncCmdId = onSelectAncCmdId,
                    onToggleSleepMode = onToggleSleepMode,
                    onToggleAutoSleepOnConnect = onToggleAutoSleepOnConnect,
                    onToggleLowLatency = onToggleLowLatency,
                    onToggleLdac = onToggleLdac,
                    onToggleDualConnect = onToggleDualConnect,
                    onSetKeyMapping = onSetKeyMapping,
                    onEditTapKey = { editingKey = it },
                    onRequestFindEarbuds = { pendingFindSide = it },
                    onStartFindEarbuds = onStartFindEarbuds,
                    onStopFindEarbuds = onStopFindEarbuds
                )

                MaterialTab.SOUND -> MaterialSoundTab(
                    state = state,
                    onSelectEqPreset = onSelectEqPreset,
                    onSetCustomEqBands = onSetCustomEqBands,
                    onSetSoundBalance = onSetSoundBalance
                )

                MaterialTab.SETTINGS -> MaterialSettingsTab(
                    state = state,
                    checkingUpdate = checkingUpdate,
                    downloadingPercent = downloadingPercent,
                    availableRelease = availableRelease,
                    updateStatusText = updateStatusText,
                    batteryUnrestricted = batteryUnrestricted,
                    companionLinked = companionLinked,
                    overlayPermissionGranted = overlayPermissionGranted,
                    isXiaomiDevice = isXiaomiDevice,
                    companionStatusNote = companionStatusNote,
                    onToggleAirpodsPopup = onToggleAirpodsPopup,
                    onRequestOverlayPermission = onRequestOverlayPermission,
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
            }
        }
    }
}

@Composable
private fun StatusChip(connected: Boolean, scanning: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = when {
        connected -> colorScheme.primaryContainer
        scanning -> colorScheme.tertiaryContainer
        else -> colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        connected -> colorScheme.onPrimaryContainer
        scanning -> colorScheme.onTertiaryContainer
        else -> colorScheme.onSurfaceVariant
    }
    val label = when {
        connected -> "Conectado"
        scanning -> "Buscando..."
        else -> "Desconectado"
    }
    Surface(
        color = containerColor,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(contentColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialControlsTab(
    state: QcyUiState,
    onScanClick: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnectClick: () -> Unit,
    onCycleAncClick: () -> Unit,
    onSelectAncMode: (AncCycleMode) -> Unit,
    onSelectAncCmdId: (Int) -> Unit,
    onToggleSleepMode: (Boolean) -> Unit,
    onToggleAutoSleepOnConnect: (Boolean) -> Unit,
    onToggleLowLatency: (Boolean) -> Unit,
    onToggleLdac: (Boolean) -> Unit,
    onToggleDualConnect: (Boolean) -> Unit,
    onSetKeyMapping: (Int, Int) -> Unit,
    onEditTapKey: (QcyKeyId) -> Unit,
    onRequestFindEarbuds: (FindEarbudSide) -> Unit,
    onStartFindEarbuds: (FindEarbudSide) -> Unit,
    onStopFindEarbuds: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var expandedTaps by rememberSaveable { mutableStateOf(false) }
    var selectedFindSide by rememberSaveable { mutableStateOf(FindEarbudSide.BOTH) }
    val leftLongDisabled = state.keyMappings[QcyKeyId.LEFT_LONG.keyId] == QcyKeyAction.NONE.funcId
    val rightLongDisabled = state.keyMappings[QcyKeyId.RIGHT_LONG.keyId] == QcyKeyAction.NONE.funcId
    val ringingSide = state.findingSide
    val isRinging = ringingSide != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Battery & Status Card
        item(key = "battery_hero") {
            ExpressiveCard(containerColor = colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconBadge(
                        icon = QuickyIcons.Headphones,
                        containerColor = if (state.connected) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                        contentColor = if (state.connected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                        size = 44.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.firmware != "--") {
                            Text(
                                text = "Firmware ${state.firmware} · ${QcyProtocol.formatAncShortLabel(state.ancCmdId)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val hasCase = state.battery.caseLevel > 0 || state.battery.caseCharging
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MaterialBatteryTile(
                        label = "Esquerdo",
                        level = state.battery.left,
                        charging = state.battery.leftCharging,
                        modifier = Modifier.weight(1f)
                    )
                    MaterialBatteryTile(
                        label = "Direito",
                        level = state.battery.right,
                        charging = state.battery.rightCharging,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasCase) {
                        MaterialBatteryTile(
                            label = "Estojo",
                            level = state.battery.caseLevel,
                            charging = state.battery.caseCharging,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Discovery Card when disconnected
        if (!state.connected) {
            item(key = "discovery") {
                ExpressiveCard(containerColor = colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconBadge(
                            icon = QuickyIcons.Bluetooth,
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.scanning) "Procurando fones..." else "Conectar fones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Retire os fones do estojo para parear via BLE",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        if (state.scanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = colorScheme.primary
                            )
                        } else {
                            Button(onClick = onScanClick) {
                                Text("Procurar")
                            }
                        }
                    }

                    if (state.devices.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.devices.forEach { dev ->
                                Surface(
                                    color = colorScheme.surfaceContainerLowest,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = dev.modelTitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${dev.bleAddress} · E ${dev.battery.left}% · D ${dev.battery.right}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                        FilledTonalButton(onClick = { onConnectDevice(dev.bleAddress) }) {
                                            Text("Conectar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Noise Control Card (ANC / Ambiente / OFF)
        item(key = "noise_control") {
            ExpressiveCard(containerColor = colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconBadge(
                            icon = QuickyIcons.NoiseCancel,
                            containerColor = colorScheme.secondaryContainer,
                            contentColor = colorScheme.onSecondaryContainer
                        )
                        Column {
                            Text(
                                text = "Controle de ruído",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = QcyProtocol.formatAncShortLabel(state.ancCmdId),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = onCycleAncClick,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = QuickyIcons.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ciclar")
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val modes = AncCycleMode.entries
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.ancCycleMode == mode,
                            onClick = { onSelectAncMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            label = {
                                Text(
                                    text = mode.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = state.ancCycleMode == AncCycleMode.ANC,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val activeScene = QcyProtocol.subSceneForCmdId(state.ancCmdId)
                    val currentAncLevel = activeScene?.levelFromCmdId(state.ancCmdId)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AncSubScene.entries.forEach { scene ->
                                val active = state.ancCmdId in scene.startCmdId..scene.endCmdId
                                FilterChip(
                                    selected = active,
                                    onClick = {
                                        val targetCmd = if (scene.levelCount > 1 && currentAncLevel != null) {
                                            scene.cmdIdForLevel(currentAncLevel)
                                        } else {
                                            scene.defaultCmdId
                                        }
                                        onSelectAncCmdId(targetCmd)
                                    },
                                    label = { Text(scene.label) },
                                    leadingIcon = if (active) {
                                        {
                                            Icon(
                                                imageVector = QuickyIcons.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }

                        if (activeScene != null && activeScene.levelCount > 1 && currentAncLevel != null) {
                            MaterialNoiseLevelSlider(
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
                    }
                }

                AnimatedVisibility(
                    visible = state.ancCycleMode == AncCycleMode.AMBIENT,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialNoiseLevelSlider(
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
        }

        // 4. Expressive Segmented List: Earbud Preferences
        item(key = "prefs_section") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel("Preferências do fone")
                Spacer(modifier = Modifier.height(4.dp))

                SegmentedToggleItem(
                    position = SegmentPosition.TOP,
                    icon = QuickyIcons.Bedtime,
                    iconContainerColor = colorScheme.primaryContainer,
                    iconContentColor = colorScheme.onPrimaryContainer,
                    title = "Ignorar toques na lateral",
                    subtitle = "Modo Sono · desativa comandos de toque do fone",
                    checked = state.sleepModeEnabled,
                    onCheckedChange = onToggleSleepMode
                )
                SegmentedToggleItem(
                    position = SegmentPosition.MIDDLE,
                    icon = QuickyIcons.Lock,
                    iconContainerColor = colorScheme.secondaryContainer,
                    iconContentColor = colorScheme.onSecondaryContainer,
                    title = "Bloquear toques ao conectar",
                    subtitle = "Ativa o Modo Sono automaticamente ao parear",
                    checked = state.autoSleepOnConnect,
                    onCheckedChange = onToggleAutoSleepOnConnect
                )
                SegmentedToggleItem(
                    position = SegmentPosition.MIDDLE,
                    icon = QuickyIcons.Gamepad,
                    iconContainerColor = colorScheme.tertiaryContainer,
                    iconContentColor = colorScheme.onTertiaryContainer,
                    title = "Modo Jogo",
                    subtitle = "Baixa latência para jogos e vídeos",
                    checked = state.lowLatencyEnabled,
                    onCheckedChange = onToggleLowLatency
                )
                SegmentedToggleItem(
                    position = SegmentPosition.MIDDLE,
                    icon = QuickyIcons.Devices,
                    iconContainerColor = colorScheme.secondaryContainer,
                    iconContentColor = colorScheme.onSecondaryContainer,
                    title = "Conexão Dupla (Multiponto)",
                    subtitle = "2 aparelhos · desativa LDAC e reinicia o fone",
                    checked = state.dualConnectEnabled,
                    onCheckedChange = onToggleDualConnect
                )
                SegmentedToggleItem(
                    position = SegmentPosition.BOTTOM,
                    icon = QuickyIcons.HighQuality,
                    iconContainerColor = colorScheme.primaryContainer,
                    iconContentColor = colorScheme.onPrimaryContainer,
                    title = "Áudio HD (LDAC)",
                    subtitle = "Alta definição · desativa Conexão Dupla",
                    checked = state.ldacEnabled,
                    onCheckedChange = onToggleLdac
                )
            }
        }

        // 5. Touch Customization Card
        item(key = "touches") {
            ExpressiveCard(containerColor = colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconBadge(
                        icon = QuickyIcons.TouchApp,
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Personalizar toques",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (state.sleepModeEnabled) {
                                "Modo Sono bloqueia todos os toques (incl. volume 3x)"
                            } else {
                                "Configure gestos sem bloquear o toque triplo"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { expandedTaps = !expandedTaps },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(if (expandedTaps) "Ocultar" else "1/2/3 toques")
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                InlineToggleRow(
                    title = "Desativar segurar Esquerdo",
                    subtitle = "Remove Modo Jogo ao segurar · experimental",
                    checked = leftLongDisabled,
                    onCheckedChange = { disable ->
                        onSetKeyMapping(
                            QcyKeyId.LEFT_LONG.keyId,
                            if (disable) QcyKeyAction.NONE.funcId else QcyKeyId.LEFT_LONG.defaultFuncId
                        )
                    }
                )

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                InlineToggleRow(
                    title = "Desativar segurar Direito",
                    subtitle = "Remove troca de ANC ao segurar · experimental",
                    checked = rightLongDisabled,
                    onCheckedChange = { disable ->
                        onSetKeyMapping(
                            QcyKeyId.RIGHT_LONG.keyId,
                            if (disable) QcyKeyAction.NONE.funcId else QcyKeyId.RIGHT_LONG.defaultFuncId
                        )
                    }
                )

                AnimatedVisibility(
                    visible = expandedTaps,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val tapKeys = listOf(
                        QcyKeyId.LEFT_SINGLE,
                        QcyKeyId.RIGHT_SINGLE,
                        QcyKeyId.LEFT_DOUBLE,
                        QcyKeyId.RIGHT_DOUBLE,
                        QcyKeyId.LEFT_TRIPLE,
                        QcyKeyId.RIGHT_TRIPLE
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        tapKeys.forEach { key ->
                            val currentFuncId = state.keyMappings[key.keyId] ?: key.defaultFuncId
                            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onEditTapKey(key) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = QcyKeyAction.labelFor(currentFuncId),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = QuickyIcons.ChevronRight,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Find Earbuds Card
        item(key = "find_earbuds") {
            ExpressiveCard(containerColor = colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconBadge(
                        icon = QuickyIcons.VolumeUp,
                        containerColor = if (isRinging) colorScheme.errorContainer else colorScheme.tertiaryContainer,
                        contentColor = if (isRinging) colorScheme.onErrorContainer else colorScheme.onTertiaryContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Encontrar fones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (ringingSide != null) {
                                "Emitindo bip no lado ${ringingSide.label}..."
                            } else {
                                "Toca um bip agudo para localizar por perto"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRinging) colorScheme.error else colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            if (isRinging) {
                                onStopFindEarbuds()
                            } else {
                                onRequestFindEarbuds(selectedFindSide)
                            }
                        },
                        colors = if (isRinging) {
                            ButtonDefaults.buttonColors(
                                containerColor = colorScheme.error,
                                contentColor = colorScheme.onError
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        }
                    ) {
                        Text(if (isRinging) "Parar" else "Tocar")
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val sides = FindEarbudSide.entries
                    val activeSide = state.findingSide ?: selectedFindSide
                    sides.forEachIndexed { index, side ->
                        SegmentedButton(
                            selected = activeSide == side,
                            onClick = {
                                selectedFindSide = side
                                if (isRinging) {
                                    onStartFindEarbuds(side)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = sides.size),
                            label = { Text(side.label) }
                        )
                    }
                }
            }
        }

        // 7. Disconnect Action
        if (state.connected) {
            item(key = "disconnect") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = onDisconnectClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error)
                    ) {
                        Icon(
                            imageVector = QuickyIcons.LinkOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Desconectar fone")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialSoundTab(
    state: QcyUiState,
    onSelectEqPreset: (QcyEqPreset) -> Unit,
    onSetCustomEqBands: (List<Int>) -> Unit,
    onSetSoundBalance: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var draftBands by remember(state.eqBands, state.eqPreset) {
        mutableStateOf(state.eqBands)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Equalizer Card
        item(key = "eq_card") {
            ExpressiveCard(containerColor = colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconBadge(
                        icon = QuickyIcons.Equalizer,
                        containerColor = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Equalizador",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (state.connected) {
                                "10 bandas · Perfil: ${state.eqPreset.label}"
                            } else {
                                "Pré-visualização · conecte os fones para aplicar"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    if (draftBands.any { it != 0 } || state.eqPreset != QcyEqPreset.DEFAULT) {
                        FilledTonalButton(
                            onClick = {
                                draftBands = QcyEqPreset.DEFAULT.defaultBands
                                onSelectEqPreset(QcyEqPreset.DEFAULT)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Zerar")
                        }
                    }
                }

                MaterialEqCurvePreview(bands = draftBands)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    QcyEqPreset.entries.forEach { preset ->
                        val selected = state.eqPreset == preset
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (preset != QcyEqPreset.CUSTOM) {
                                    draftBands = preset.defaultBands
                                }
                                onSelectEqPreset(preset)
                            },
                            label = { Text(preset.label) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = QuickyIcons.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    QcyProtocol.EQ_BAND_LABELS.forEachIndexed { idx, freqLabel ->
                        val committedVal = (state.eqBands.getOrNull(idx) ?: 0)
                            .coerceIn(QcyProtocol.EQ_MIN_DB, QcyProtocol.EQ_MAX_DB)
                        val currentDraft = (draftBands.getOrNull(idx) ?: committedVal)
                            .coerceIn(QcyProtocol.EQ_MIN_DB, QcyProtocol.EQ_MAX_DB)

                        MaterialEqBandRow(
                            freqLabel = freqLabel,
                            dbValue = currentDraft,
                            bandIndex = idx,
                            onDraftChange = { newDb ->
                                val updated = draftBands.toMutableList()
                                if (updated.size == QcyProtocol.EQ_BAND_FREQS.size) {
                                    updated[idx] = newDb
                                    draftBands = updated
                                }
                            },
                            onCommit = {
                                onSetCustomEqBands(draftBands)
                            }
                        )
                    }
                }
            }
        }

        // Channel Balance Card
        item(key = "balance_card") {
            MaterialSoundBalanceCard(
                soundBalance = state.soundBalance,
                connected = state.connected,
                onCommitBalance = onSetSoundBalance
            )
        }
    }
}

@Composable
private fun MaterialSettingsTab(
    state: QcyUiState,
    checkingUpdate: Boolean,
    downloadingPercent: Int?,
    availableRelease: ReleaseInfo?,
    updateStatusText: String,
    batteryUnrestricted: Boolean,
    companionLinked: Boolean,
    overlayPermissionGranted: Boolean,
    isXiaomiDevice: Boolean,
    companionStatusNote: String?,
    onToggleAirpodsPopup: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
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
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var controlCenterHint by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Control Center / Device Controls Card (Android 11+ / Xiaomi HyperOS / Samsung OneUI / AOSP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            item(key = "device_controls") {
                ExpressiveCard(containerColor = colorScheme.secondaryContainer.copy(alpha = 0.55f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconBadge(
                            icon = QuickyIcons.Widgets,
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Widget no Centro de Controle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (isXiaomiDevice) {
                                    "Selecione QuiCkY QCY no cartão Casa inteligente (Centro de Controle) para controlar ANC, Ambiente, Modo Sono e Modo Jogo."
                                } else {
                                    "Selecione QuiCkY QCY em Controles de dispositivos (Configurações Rápidas) para controlar ANC, Ambiente, Modo Sono e Modo Jogo."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = controlCenterHint ?: if (isXiaomiDevice) {
                                "HyperOS: Centro de Controle → Casa inteligente → QuiCkY QCY"
                            } else {
                                "Compatível com HyperOS, OneUI e Android 11+"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f).padding(end = 10.dp)
                        )
                        Button(
                            onClick = {
                                val ok = QcyDeviceControlsService.requestAddControlToSystem(context)
                                controlCenterHint = if (ok) {
                                    "Solicitação enviada ao sistema · escolha QuiCkY QCY no painel"
                                } else {
                                    "Abra o Centro de Controle e selecione QuiCkY QCY em Casa inteligente"
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Adicionar")
                        }
                    }
                }
            }
        }

        // 2. Background Reliability Section
        item(key = "background") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel("Execução em 2º plano")
                Spacer(modifier = Modifier.height(4.dp))

                SegmentedActionItem(
                    position = SegmentPosition.TOP,
                    icon = QuickyIcons.Shield,
                    title = "Vínculo de acessório (Sistema)",
                    subtitle = if (companionLinked) {
                        "Vinculado · Android acorda o app ao conectar"
                    } else {
                        companionStatusNote ?: "Recomendado · despertar sem notificação fixa"
                    },
                    highlightSubtitle = companionLinked,
                    buttonText = if (companionLinked) "Vinculado" else "Vincular",
                    primaryButton = !companionLinked,
                    onClick = onRequestCompanionLink
                )

                SegmentedActionItem(
                    position = if (isXiaomiDevice) SegmentPosition.MIDDLE else SegmentPosition.BOTTOM,
                    icon = QuickyIcons.Bolt,
                    title = "Bateria sem restrições",
                    subtitle = if (batteryUnrestricted) {
                        "Ativo · sistema não encerra o Bluetooth em espera"
                    } else {
                        "Restrito · pode impedir conexão automática"
                    },
                    highlightSubtitle = batteryUnrestricted,
                    buttonText = if (batteryUnrestricted) "Ativo" else "Liberar",
                    primaryButton = !batteryUnrestricted,
                    onClick = onRequestBatteryUnrestricted
                )

                if (isXiaomiDevice) {
                    SegmentedActionItem(
                        position = SegmentPosition.BOTTOM,
                        icon = QuickyIcons.Settings,
                        title = "Início Automático (HyperOS)",
                        subtitle = "Ative nas configurações da Xiaomi para iniciar ao conectar",
                        highlightSubtitle = false,
                        buttonText = "Abrir",
                        primaryButton = false,
                        onClick = onOpenXiaomiAutostart
                    )
                }
            }
        }

        // 3. AirPods-Style Pop-up Card
        item(key = "popup") {
            ExpressiveCard(containerColor = colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconBadge(
                        icon = QuickyIcons.Layers,
                        containerColor = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pop-up estilo AirPods",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Mostra bateria e controle de ruído ao conectar",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    ExpressiveSwitch(
                        checked = state.airpodsPopupEnabled,
                        onCheckedChange = onToggleAirpodsPopup
                    )
                }

                AnimatedVisibility(
                    visible = state.airpodsPopupEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "Sobrepor outros apps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = if (overlayPermissionGranted) {
                                        "Permitido · pop-up flutuante ativo"
                                    } else {
                                        "Sem permissão · usando notificação rápida"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (overlayPermissionGranted) colorScheme.primary else colorScheme.onSurfaceVariant
                                )
                            }
                            if (overlayPermissionGranted) {
                                FilledTonalButton(onClick = onRequestOverlayPermission) {
                                    Text("Ativo")
                                }
                            } else {
                                Button(onClick = onRequestOverlayPermission) {
                                    Text("Permitir")
                                }
                            }
                        }

                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "Testar pop-up agora",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Pré-visualizar o cartão flutuante na tela",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(onClick = { AirPodsPopupController.showPreview(context) }) {
                                Text("Testar")
                            }
                        }
                    }
                }
            }
        }

        // 4. Notifications & Quick Settings Tile Card
        item(key = "notifications") {
            ExpressiveCard(containerColor = colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconBadge(
                        icon = QuickyIcons.Notifications,
                        containerColor = colorScheme.tertiaryContainer,
                        contentColor = colorScheme.onTertiaryContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notificações do aplicativo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Controles rápidos fixos na barra de notificações",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    ExpressiveSwitch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = onToggleNotifications
                    )
                }

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = "Dica: toque no bloco 'QCY Ruído' nas Configurações Rápidas para ciclar ANC / Ambiente / OFF, ou segure para abrir o app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        // 5. Interface Appearance Card (Material You / Apple Liquid Glass)
        item(key = "ui_style") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel("Aparência da interface")
                Spacer(modifier = Modifier.height(4.dp))
                val styles = UiStyle.entries
                styles.forEachIndexed { idx, style ->
                    val selected = state.uiStyle == style
                    val pos = when {
                        styles.size == 1 -> SegmentPosition.SINGLE
                        idx == 0 -> SegmentPosition.TOP
                        idx == styles.lastIndex -> SegmentPosition.BOTTOM
                        else -> SegmentPosition.MIDDLE
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(segmentShape(pos))
                            .clickable { onSelectUiStyle(style) },
                        shape = segmentShape(pos),
                        color = if (selected) colorScheme.secondaryContainer.copy(alpha = 0.65f) else colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconBadge(
                                icon = QuickyIcons.Palette,
                                containerColor = if (selected) colorScheme.primary else colorScheme.surfaceContainerHighest,
                                contentColor = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = style.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectUiStyle(style) }
                            )
                        }
                    }
                }
            }
        }

        // 6. App Updates Card
        item(key = "updates") {
            ExpressiveCard(containerColor = colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconBadge(
                        icon = QuickyIcons.SystemUpdate,
                        containerColor = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Atualizações do aplicativo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Versão atual: v${BuildConfig.VERSION_NAME} · $updateStatusText",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val sources = UpdateSource.entries
                    sources.forEachIndexed { index, source ->
                        SegmentedButton(
                            selected = state.updateSource == source,
                            onClick = { onSelectUpdateSource(source) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = sources.size),
                            label = {
                                Text(
                                    text = source.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                InlineToggleRow(
                    title = "Verificar ao abrir o aplicativo",
                    subtitle = "Fonte: ${state.updateSource.label} (${state.updateSource.hostLabel})",
                    checked = state.autoCheckUpdates,
                    onCheckedChange = onToggleAutoCheckUpdates
                )

                if (downloadingPercent != null) {
                    LinearProgressIndicator(
                        progress = { (downloadingPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onCheckUpdateNow,
                        enabled = !checkingUpdate && downloadingPercent == null,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (checkingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (checkingUpdate) "Verificando..." else "Procurar atualização")
                    }

                    if (availableRelease != null) {
                        Button(
                            onClick = { onDownloadAndInstallUpdate(availableRelease) },
                            enabled = downloadingPercent == null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Instalar ${availableRelease.tagName}")
                        }
                    }
                }
            }
        }
    }
}

private enum class SegmentPosition { TOP, MIDDLE, BOTTOM, SINGLE }

private fun segmentShape(position: SegmentPosition): RoundedCornerShape = when (position) {
    SegmentPosition.SINGLE -> RoundedCornerShape(24.dp)
    SegmentPosition.TOP -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    SegmentPosition.MIDDLE -> RoundedCornerShape(8.dp)
    SegmentPosition.BOTTOM -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

@Composable
private fun ExpressiveCard(
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    size: Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
private fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = QuickyIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        } else null
    )
}

@Composable
private fun SegmentedToggleItem(
    position: SegmentPosition,
    icon: ImageVector,
    iconContainerColor: Color,
    iconContentColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(segmentShape(position))
            .clickable { onCheckedChange(!checked) },
        shape = segmentShape(position),
        color = colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(
                icon = icon,
                containerColor = iconContainerColor,
                contentColor = iconContentColor
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun SegmentedActionItem(
    position: SegmentPosition,
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlightSubtitle: Boolean,
    buttonText: String,
    primaryButton: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = segmentShape(position),
        color = colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(
                icon = icon,
                containerColor = if (highlightSubtitle) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                contentColor = if (highlightSubtitle) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highlightSubtitle) colorScheme.primary else colorScheme.onSurfaceVariant
                )
            }
            if (primaryButton) {
                Button(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(buttonText)
                }
            } else {
                FilledTonalButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
private fun InlineToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
        ExpressiveSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun MaterialBatteryTile(
    label: String,
    level: Int,
    charging: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val valid = level in 0..100
    val targetProgress = if (valid) (level / 100f).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(420),
        label = "batteryRing"
    )
    val accentColor = when {
        !valid -> colorScheme.outline
        charging -> colorScheme.primary
        level <= 15 -> colorScheme.error
        level <= 25 -> colorScheme.tertiary
        else -> colorScheme.primary
    }
    val trackColor = colorScheme.surfaceVariant

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 6.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    if (animatedProgress > 0f) {
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (valid) "$level%" else "--",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    if (charging) {
                        Icon(
                            imageVector = QuickyIcons.Bolt,
                            contentDescription = "Carregando",
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MaterialNoiseLevelSlider(
    title: String,
    currentLevel: Int,
    levelCount: Int,
    ancCmdId: Int,
    levelText: (Int) -> String,
    onCommitLevel: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
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
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurfaceVariant
            )
            Surface(
                color = colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = levelText(displayLevel),
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
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
            steps = (levelCount - 2).coerceAtLeast(0)
        )
    }
}

@Composable
private fun MaterialEqCurvePreview(bands: List<Int>) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = colorScheme.primary
    val gridColor = colorScheme.outlineVariant.copy(alpha = 0.55f)
    val zeroLineColor = colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp),
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainerHighest
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f || bands.size < 2) return@Canvas

                val midY = h * 0.5f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, h * 0.14f),
                    end = Offset(w, h * 0.14f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = zeroLineColor,
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.3.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, h * 0.86f),
                    end = Offset(w, h * 0.86f),
                    strokeWidth = 1.dp.toPx()
                )

                val span = (QcyProtocol.EQ_MAX_DB - QcyProtocol.EQ_MIN_DB).coerceAtLeast(1)
                val points = bands.mapIndexed { idx, db ->
                    val x = (idx.toFloat() / (bands.size - 1)) * w
                    val norm = ((db - QcyProtocol.EQ_MIN_DB).toFloat() / span).coerceIn(0f, 1f)
                    val y = h - norm * h
                    Offset(x, y)
                }

                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) * 0.5f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, h)
                    lineTo(points.first().x, h)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.30f), Color.Transparent),
                        startY = 0f,
                        endY = h
                    )
                )
                drawPath(
                    path = linePath,
                    color = accentColor,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )
                points.forEach { pt ->
                    drawCircle(
                        color = accentColor,
                        radius = 3.5.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialEqBandRow(
    freqLabel: String,
    dbValue: Int,
    bandIndex: Int,
    onDraftChange: (Int) -> Unit,
    onCommit: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var draggingRaw by remember(bandIndex) { mutableStateOf<Float?>(null) }
    val displayDb = (draggingRaw?.roundToInt() ?: dbValue)
        .coerceIn(QcyProtocol.EQ_MIN_DB, QcyProtocol.EQ_MAX_DB)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = freqLabel,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.width(46.dp)
        )
        Slider(
            value = draggingRaw ?: dbValue.toFloat(),
            onValueChange = { raw ->
                draggingRaw = raw
                val rounded = raw.roundToInt().coerceIn(QcyProtocol.EQ_MIN_DB, QcyProtocol.EQ_MAX_DB)
                if (rounded != dbValue) {
                    onDraftChange(rounded)
                }
            },
            onValueChangeFinished = {
                val finalDb = (draggingRaw?.roundToInt() ?: dbValue)
                    .coerceIn(QcyProtocol.EQ_MIN_DB, QcyProtocol.EQ_MAX_DB)
                draggingRaw = null
                onDraftChange(finalDb)
                onCommit()
            },
            valueRange = QcyProtocol.EQ_MIN_DB.toFloat()..QcyProtocol.EQ_MAX_DB.toFloat(),
            steps = (QcyProtocol.EQ_MAX_DB - QcyProtocol.EQ_MIN_DB - 1).coerceAtLeast(0),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = when {
                displayDb > 0 -> "+$displayDb dB"
                displayDb < 0 -> "$displayDb dB"
                else -> "0 dB"
            },
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = if (displayDb != 0) colorScheme.primary else colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp)
        )
    }
}

@Composable
private fun MaterialSoundBalanceCard(
    soundBalance: Int,
    connected: Boolean,
    onCommitBalance: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var draggingValue by remember(soundBalance) { mutableStateOf<Float?>(null) }
    val displayBalance = (draggingValue?.roundToInt() ?: soundBalance).coerceIn(0, 100)

    val balanceLabel = when {
        displayBalance == 50 -> "Centro"
        displayBalance < 50 -> "Esquerdo · ${50 - displayBalance}%"
        else -> "Direito · ${displayBalance - 50}%"
    }

    ExpressiveCard(containerColor = colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(
                icon = QuickyIcons.Tune,
                containerColor = colorScheme.secondaryContainer,
                contentColor = colorScheme.onSecondaryContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Balanço de canais",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = if (connected) balanceLabel else "$balanceLabel · conecte para aplicar",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Medium,
                    color = if (displayBalance == 50) colorScheme.onSurfaceVariant else colorScheme.primary
                )
            }
            if (displayBalance != 50) {
                FilledTonalButton(
                    onClick = {
                        draggingValue = null
                        onCommitBalance(50)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Centralizar")
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Slider(
                value = draggingValue ?: soundBalance.toFloat(),
                onValueChange = { draggingValue = it },
                onValueChangeFinished = {
                    val target = (draggingValue?.roundToInt() ?: soundBalance).coerceIn(0, 100)
                    draggingValue = null
                    if (target != soundBalance) {
                        onCommitBalance(target)
                    }
                },
                valueRange = 0f..100f
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Esquerdo",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Centro",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Direito",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
