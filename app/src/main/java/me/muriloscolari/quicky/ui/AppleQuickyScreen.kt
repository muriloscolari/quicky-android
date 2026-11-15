package me.muriloscolari.quicky.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
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

private enum class AppleTab(val label: String) {
    CONTROLS("Controles"),
    SETTINGS("Ajustes")
}

@Composable
fun AppleQuickyScreen(
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
    val appleColors = rememberAppleColors()
    val reduceMotion = rememberReduceMotion()
    val hazeState = rememberHazeState()

    var activeTab by rememberSaveable { mutableStateOf(AppleTab.CONTROLS) }
    var editingKey by remember { mutableStateOf<QcyKeyId?>(null) }
    var pendingFindSide by remember { mutableStateOf<FindEarbudSide?>(null) }

    val controlsListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    val currentListState = if (activeTab == AppleTab.CONTROLS) controlsListState else settingsListState

    val collapseProgress by remember(currentListState) {
        derivedStateOf {
            if (currentListState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (currentListState.firstVisibleItemScrollOffset / 110f).coerceIn(0f, 1f)
            }
        }
    }

    val sheetOpen = editingKey != null
    val bgScale by animateFloatAsState(
        targetValue = if (sheetOpen && !reduceMotion) 0.94f else 1f,
        animationSpec = AppleMotion.floatSpring,
        label = "sheetPullbackScale"
    )
    val bgCorner by animateDpAsState(
        targetValue = if (sheetOpen && !reduceMotion) 28.dp else 0.dp,
        animationSpec = AppleMotion.dpSpring,
        label = "sheetPullbackCorner"
    )

    BackHandler(enabled = pendingFindSide != null || editingKey != null || activeTab == AppleTab.SETTINGS) {
        when {
            pendingFindSide != null -> pendingFindSide = null
            editingKey != null -> editingKey = null
            activeTab == AppleTab.SETTINGS -> activeTab = AppleTab.CONTROLS
        }
    }

    CompositionLocalProvider(LocalAppleColors provides appleColors) {
        // Black underlay for depth pullback when an AppleSheet is open
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val safeCorner = bgCorner.coerceAtLeast(0.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = bgScale
                        scaleY = bgScale
                    }
                    .clip(RoundedCornerShape(safeCorner))
                    .background(appleColors.background)
            ) {
                // CAMADA 1: Edge-to-Edge Content (hazeSource)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState)
                ) {
                    AppleMeshBackdrop(
                        accentColor = appleColors.accent,
                        isDark = appleColors.isDark
                    )

                    if (activeTab == AppleTab.CONTROLS) {
                        AppleControlsList(
                            listState = controlsListState,
                            state = state,
                            collapseProgress = collapseProgress,
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
                            onRequestFindEarbuds = { side -> pendingFindSide = side },
                            onStartFindEarbuds = onStartFindEarbuds,
                            onStopFindEarbuds = onStopFindEarbuds
                        )
                    } else {
                        AppleSettingsList(
                            listState = settingsListState,
                            state = state,
                            collapseProgress = collapseProgress,
                            checkingUpdate = checkingUpdate,
                            downloadingPercent = downloadingPercent,
                            availableRelease = availableRelease,
                            updateStatusText = updateStatusText,
                            batteryUnrestricted = batteryUnrestricted,
                            companionLinked = companionLinked,
                            isXiaomiDevice = isXiaomiDevice,
                            companionStatusNote = companionStatusNote,
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

                // CAMADA 2: Floating Glass Navigation (Top Bar + Floating Capsule Tab Bar)
                AppleFloatingTopBar(
                    title = if (activeTab == AppleTab.CONTROLS) state.modelTitle else "Ajustes",
                    subtitle = if (activeTab == AppleTab.CONTROLS) state.statusMessage else "v${BuildConfig.VERSION_NAME}",
                    connected = state.connected,
                    showConnectedDot = activeTab == AppleTab.CONTROLS,
                    showRefresh = activeTab == AppleTab.CONTROLS && state.connected,
                    collapseProgress = collapseProgress,
                    hazeState = hazeState,
                    onRefreshClick = onRefreshClick,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                AppleFloatingTabBar(
                    activeTab = activeTab,
                    hasUpdateBadge = availableRelease != null,
                    hazeState = hazeState,
                    onSelectTab = { activeTab = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // CAMADA 3: Overlays (AppleSheet & AppleAlert)
            AnimatedVisibility(
                visible = editingKey != null,
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut() + slideOutVertically { it / 3 }
            ) {
                val targetKey = editingKey
                if (targetKey != null) {
                    val currentFunc = state.keyMappings[targetKey.keyId] ?: targetKey.defaultFuncId
                    AppleSheet(
                        title = targetKey.label,
                        hazeState = hazeState,
                        onDismiss = { editingKey = null }
                    ) {
                        PortalCard(
                            insideSheet = true,
                            contentPadding = 4.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                QcyKeyAction.entries.forEachIndexed { idx, action ->
                                    if (idx > 0) {
                                        PortalDivider(startIndent = 16.dp)
                                    }
                                    val selected = currentFunc == action.funcId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                onSetKeyMapping(targetKey.keyId, action.funcId)
                                                editingKey = null
                                            }
                                            .padding(horizontal = 14.dp, vertical = 13.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = action.label,
                                            style = AppleTypography.subheadline.copy(
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            color = if (selected) appleColors.accent else appleColors.label
                                        )
                                        if (selected) {
                                            Text(
                                                text = "✓",
                                                style = AppleTypography.headline,
                                                color = appleColors.accent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = pendingFindSide != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val side = pendingFindSide
                if (side != null) {
                    AppleAlert(
                        title = "Os fones estão fora do ouvido?",
                        message = "Para proteger sua audição, retire os fones do ouvido antes de tocar o alerta sonoro de localização.",
                        confirmText = "Emitir som",
                        dismissText = "Cancelar",
                        onConfirm = {
                            pendingFindSide = null
                            onStartFindEarbuds(side)
                        },
                        onDismiss = { pendingFindSide = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleFloatingTopBar(
    title: String,
    subtitle: String,
    connected: Boolean,
    showConnectedDot: Boolean,
    showRefresh: Boolean,
    collapseProgress: Float,
    hazeState: dev.chrisbanes.haze.HazeState,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppleColors.current
    val safeProgress = if (collapseProgress.isFinite()) collapseProgress.coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (safeProgress > 0.05f) {
            GlassSurface(
                hazeState = hazeState,
                cornerRadius = 22.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = safeProgress }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = AppleTypography.headline,
                            color = colors.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (showConnectedDot) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (connected) colors.green else colors.tertiaryLabel)
                                )
                            }
                            Text(
                                text = subtitle,
                                style = AppleTypography.caption1.tabular(),
                                color = colors.secondaryLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (showRefresh) {
                        ApplePillButton(
                            text = "↻",
                            prominent = false,
                            onClick = onRefreshClick
                        )
                    }
                }
            }
        } else if (showRefresh) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AppleCircleButton(
                    hazeState = hazeState,
                    onClick = onRefreshClick,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        text = "↻",
                        style = AppleTypography.headline,
                        color = colors.label
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleFloatingTabBar(
    activeTab: AppleTab,
    hasUpdateBadge: Boolean,
    hazeState: dev.chrisbanes.haze.HazeState,
    onSelectTab: (AppleTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppleColors.current
    val tabs = AppleTab.entries
    val selectedIndex = tabs.indexOf(activeTab).coerceAtLeast(0)

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            hazeState = hazeState,
            cornerRadius = 32.dp,
            specular = 0.50f,
            modifier = Modifier
                .width(248.dp)
                .height(54.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                val tabWidth = (maxWidth / tabs.size).coerceAtLeast(1.dp)
                val lensOffset by animateDpAsState(
                    targetValue = tabWidth * selectedIndex,
                    animationSpec = AppleMotion.dpSpring,
                    label = "tabLensOffset"
                )

                // Sliding glass lens behind active tab
                Box(
                    modifier = Modifier
                        .offset(x = lensOffset.coerceAtLeast(0.dp))
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            if (colors.isDark) Color.White.copy(alpha = 0.14f)
                            else Color.White.copy(alpha = 0.86f)
                        )
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEach { tab ->
                        val selected = tab == activeTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onSelectTab(tab) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = tab.label,
                                    style = AppleTypography.subheadline.copy(
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = if (selected) colors.accent else colors.secondaryLabel,
                                    maxLines = 1
                                )
                                if (tab == AppleTab.SETTINGS && hasUpdateBadge) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(colors.accent)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppleControlsList(
    listState: LazyListState,
    state: QcyUiState,
    collapseProgress: Float,
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
    val colors = LocalAppleColors.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var expandedTaps by rememberSaveable { mutableStateOf(false) }
    var selectedFindSide by rememberSaveable { mutableStateOf(FindEarbudSide.BOTH) }
    val leftLongDisabled = state.keyMappings[QcyKeyId.LEFT_LONG.keyId] == QcyKeyAction.NONE.funcId
    val rightLongDisabled = state.keyMappings[QcyKeyId.RIGHT_LONG.keyId] == QcyKeyAction.NONE.funcId
    val isRinging = state.findingSide != null

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = statusBarTop + 18.dp,
            bottom = navBarBottom + 92.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Large Title Header
        item(key = "header") {
            val headerAlpha = (1f - collapseProgress * 1.15f).coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .graphicsLayer { alpha = headerAlpha }
            ) {
                Text(
                    text = state.modelTitle,
                    style = AppleTypography.largeTitle,
                    color = colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (state.connected) colors.green else colors.tertiaryLabel)
                    )
                    Text(
                        text = state.statusMessage,
                        style = AppleTypography.subheadline,
                        color = colors.secondaryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Battery Card
        item(key = "battery") {
            PortalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppleBatteryRingColumn(
                        label = "Esquerdo",
                        level = state.battery.left,
                        charging = state.battery.leftCharging,
                        modifier = Modifier.weight(1f)
                    )
                    AppleBatteryRingColumn(
                        label = "Direito",
                        level = state.battery.right,
                        charging = state.battery.rightCharging,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.battery.caseLevel > 0 || state.battery.caseCharging) {
                        AppleBatteryRingColumn(
                            label = "Estojo",
                            level = state.battery.caseLevel,
                            charging = state.battery.caseCharging,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Connection / Discovery Card when disconnected
        if (!state.connected) {
            item(key = "discovery") {
                PortalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(
                                text = if (state.scanning) "Procurando fones..." else "Fone desconectado",
                                style = AppleTypography.headline,
                                color = colors.label
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Retire os fones do estojo para conectar",
                                style = AppleTypography.caption1,
                                color = colors.secondaryLabel
                            )
                        }
                        if (state.scanning) {
                            AppleActivityIndicator(color = colors.accent)
                        } else {
                            ApplePillButton(
                                text = "Procurar",
                                prominent = true,
                                onClick = onScanClick
                            )
                        }
                    }

                    if (state.devices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        state.devices.forEachIndexed { idx, dev ->
                            if (idx > 0 || true) {
                                PortalDivider()
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = dev.modelTitle,
                                        style = AppleTypography.subheadline.copy(fontWeight = FontWeight.Medium),
                                        color = colors.label
                                    )
                                    Text(
                                        text = "${dev.bleAddress} · L ${dev.battery.left}% R ${dev.battery.right}%",
                                        style = AppleTypography.caption1.tabular(),
                                        color = colors.secondaryLabel
                                    )
                                }
                                ApplePillButton(
                                    text = "Conectar",
                                    prominent = false,
                                    onClick = { onConnectDevice(dev.bleAddress) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Noise Control Card
        item(key = "anc") {
            PortalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Controle de ruído",
                        style = AppleTypography.headline,
                        color = colors.label
                    )
                    Text(
                        text = "Ciclar ↻",
                        style = AppleTypography.footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onCycleAncClick)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                PortalSegmentedControl(
                    items = AncCycleMode.entries,
                    selectedItem = state.ancCycleMode,
                    labelProvider = { it.label },
                    onSelect = onSelectAncMode,
                    isCapsule = true
                )

                if (state.ancCycleMode == AncCycleMode.ANC) {
                    val activeScene = QcyProtocol.subSceneForCmdId(state.ancCmdId)
                    val currentAncLevel = activeScene?.levelFromCmdId(state.ancCmdId)

                    Spacer(modifier = Modifier.height(14.dp))
                    PortalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AncSubScene.entries.forEach { scene ->
                            val active = state.ancCmdId in scene.startCmdId..scene.endCmdId
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (active) colors.accent.copy(alpha = 0.16f)
                                        else colors.systemFill.copy(alpha = 0.55f)
                                    )
                                    .border(
                                        width = if (active) 1.dp else 0.dp,
                                        color = if (active) colors.accent else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        val targetCmd = if (scene.levelCount > 1 && currentAncLevel != null) {
                                            scene.cmdIdForLevel(currentAncLevel)
                                        } else {
                                            scene.defaultCmdId
                                        }
                                        onSelectAncCmdId(targetCmd)
                                    }
                                    .padding(horizontal = 13.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = scene.label,
                                    style = AppleTypography.caption1.copy(
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = if (active) colors.accent else colors.secondaryLabel
                                )
                            }
                        }
                    }

                    if (activeScene != null && activeScene.levelCount > 1 && currentAncLevel != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AppleNoiseSlider(
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
                    Spacer(modifier = Modifier.height(14.dp))
                    PortalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    AppleNoiseSlider(
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

        // Touch & Audio Preferences Card (iOS Settings style with colored 30.dp badges)
        item(key = "preferences") {
            PortalCard(contentPadding = 16.dp) {
                PortalListRow(
                    title = "Ignorar toques na lateral",
                    subtitle = "Modo Sono · desativa os comandos de toque do fone",
                    leading = {
                        AppleIconBadge(backgroundColor = Color(0xFF5856D6)) {
                            Text("☾", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    trailing = {
                        AppleToggle(checked = state.sleepModeEnabled, onCheckedChange = onToggleSleepMode)
                    }
                )
                PortalDivider(startIndent = 42.dp)
                PortalListRow(
                    title = "Bloquear toques ao conectar",
                    subtitle = "Ativa o Modo Sono sozinho ao conectar no celular",
                    leading = {
                        AppleIconBadge(backgroundColor = colors.accent) {
                            Text("🔒", fontSize = 13.sp)
                        }
                    },
                    trailing = {
                        AppleToggle(checked = state.autoSleepOnConnect, onCheckedChange = onToggleAutoSleepOnConnect)
                    }
                )
                PortalDivider(startIndent = 42.dp)
                PortalListRow(
                    title = "Modo Jogo",
                    subtitle = "Baixa latência para jogos e vídeos",
                    leading = {
                        AppleIconBadge(backgroundColor = colors.orange) {
                            Text("⚡", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    trailing = {
                        AppleToggle(checked = state.lowLatencyEnabled, onCheckedChange = onToggleLowLatency)
                    }
                )
                PortalDivider(startIndent = 42.dp)
                PortalListRow(
                    title = "Conexão Dupla (Multiponto)",
                    subtitle = "2 aparelhos · desativa LDAC e reinicia o fone",
                    leading = {
                        AppleIconBadge(backgroundColor = colors.green) {
                            Text("⇄", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    trailing = {
                        AppleToggle(checked = state.dualConnectEnabled, onCheckedChange = onToggleDualConnect)
                    }
                )
                PortalDivider(startIndent = 42.dp)
                PortalListRow(
                    title = "Áudio HD (LDAC)",
                    subtitle = "Alta definição · desativa Conexão Dupla",
                    leading = {
                        AppleIconBadge(backgroundColor = Color(0xFFAF52DE)) {
                            Text("HD", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    trailing = {
                        AppleToggle(checked = state.ldacEnabled, onCheckedChange = onToggleLdac)
                    }
                )
            }
        }

        // Touch Customization Card
        item(key = "touches") {
            PortalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "Personalizar toques",
                            style = AppleTypography.headline,
                            color = colors.label
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (state.sleepModeEnabled) {
                                "Modo Sono ativo bloqueia todos os toques (incl. volume 3x)"
                            } else {
                                "Ajuste gestos sem bloquear o toque triplo de volume"
                            },
                            style = AppleTypography.caption1,
                            color = colors.secondaryLabel
                        )
                    }
                    ApplePillButton(
                        text = if (expandedTaps) "Ocultar" else "1/2/3 toques",
                        prominent = expandedTaps,
                        onClick = { expandedTaps = !expandedTaps }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                PortalDivider()

                PortalListRow(
                    title = "Desativar segurar Esquerdo",
                    subtitle = "Remove Modo Jogo ao segurar · experimental",
                    trailing = {
                        AppleToggle(
                            checked = leftLongDisabled,
                            onCheckedChange = { disable ->
                                onSetKeyMapping(
                                    QcyKeyId.LEFT_LONG.keyId,
                                    if (disable) QcyKeyAction.NONE.funcId else QcyKeyId.LEFT_LONG.defaultFuncId
                                )
                            }
                        )
                    }
                )
                PortalDivider()
                PortalListRow(
                    title = "Desativar segurar Direito",
                    subtitle = "Remove troca de ANC ao segurar · experimental",
                    trailing = {
                        AppleToggle(
                            checked = rightLongDisabled,
                            onCheckedChange = { disable ->
                                onSetKeyMapping(
                                    QcyKeyId.RIGHT_LONG.keyId,
                                    if (disable) QcyKeyAction.NONE.funcId else QcyKeyId.RIGHT_LONG.defaultFuncId
                                )
                            }
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
                        PortalDivider()
                        PortalListRow(
                            title = key.label,
                            trailing = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = QcyKeyAction.labelFor(currentFuncId),
                                        style = AppleTypography.subheadline,
                                        color = colors.secondaryLabel
                                    )
                                    Text(
                                        text = "›",
                                        style = AppleTypography.title3,
                                        color = colors.tertiaryLabel
                                    )
                                }
                            },
                            onClick = { onEditTapKey(key) }
                        )
                    }
                }
            }
        }

        // Find Earbuds Card
        item(key = "find") {
            PortalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "Encontrar fones",
                            style = AppleTypography.headline,
                            color = colors.label
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isRinging) {
                                "Emitindo bip no lado ${state.findingSide?.label}..."
                            } else {
                                "Toca um bip agudo para localizar os fones por perto"
                            },
                            style = AppleTypography.caption1,
                            color = if (isRinging) colors.accent else colors.secondaryLabel
                        )
                    }
                    ApplePillButton(
                        text = if (isRinging) "Parar som" else "Tocar som",
                        prominent = isRinging,
                        onClick = {
                            if (isRinging) {
                                onStopFindEarbuds()
                            } else {
                                onRequestFindEarbuds(selectedFindSide)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                PortalSegmentedControl(
                    items = FindEarbudSide.entries,
                    selectedItem = state.findingSide ?: selectedFindSide,
                    labelProvider = { it.label },
                    onSelect = { side ->
                        selectedFindSide = side
                        if (isRinging) {
                            onStartFindEarbuds(side)
                        }
                    }
                )
            }
        }

        if (state.connected) {
            item(key = "disconnect") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Desconectar fone",
                        style = AppleTypography.subheadline.copy(fontWeight = FontWeight.Medium),
                        color = colors.red,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onDisconnectClick)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleSettingsList(
    listState: LazyListState,
    state: QcyUiState,
    collapseProgress: Float,
    checkingUpdate: Boolean,
    downloadingPercent: Int?,
    availableRelease: ReleaseInfo?,
    updateStatusText: String,
    batteryUnrestricted: Boolean,
    companionLinked: Boolean,
    isXiaomiDevice: Boolean,
    companionStatusNote: String?,
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
    val colors = LocalAppleColors.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = statusBarTop + 18.dp,
            bottom = navBarBottom + 92.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "settings_header") {
            val headerAlpha = (1f - collapseProgress * 1.15f).coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .graphicsLayer { alpha = headerAlpha }
            ) {
                Text(
                    text = "Ajustes",
                    style = AppleTypography.largeTitle,
                    color = colors.label
                )
            }
        }

        // Background Reliability Card
        item(key = "background") {
            PortalCard {
                Text(
                    text = "Execução em 2º plano",
                    style = AppleTypography.headline,
                    color = colors.label
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Garante que o botão da barra e o Modo Sono automático funcionem mesmo após horas sem abrir o app.",
                    style = AppleTypography.caption1,
                    color = colors.secondaryLabel
                )
                Spacer(modifier = Modifier.height(10.dp))

                PortalListRow(
                    title = "Vínculo de acessório (Sistema)",
                    subtitle = if (companionLinked) {
                        "Vinculado · Android acorda o app ao conectar"
                    } else {
                        companionStatusNote ?: "Recomendado · despertar sem notificação fixa"
                    },
                    trailing = {
                        ApplePillButton(
                            text = if (companionLinked) "Vinculado" else "Vincular",
                            prominent = !companionLinked,
                            onClick = onRequestCompanionLink
                        )
                    }
                )
                PortalDivider()
                PortalListRow(
                    title = "Bateria sem restrições",
                    subtitle = if (batteryUnrestricted) {
                        "Ativo · sistema não encerra o Bluetooth"
                    } else {
                        "Restrito · pode impedir conexão automática"
                    },
                    trailing = {
                        ApplePillButton(
                            text = if (batteryUnrestricted) "Ativo" else "Liberar",
                            prominent = !batteryUnrestricted,
                            onClick = onRequestBatteryUnrestricted
                        )
                    }
                )
                if (isXiaomiDevice) {
                    PortalDivider()
                    PortalListRow(
                        title = "Início Automático (HyperOS)",
                        subtitle = "Ative nas configurações da Xiaomi para iniciar ao conectar",
                        trailing = {
                            ApplePillButton(
                                text = "Abrir",
                                prominent = false,
                                onClick = onOpenXiaomiAutostart
                            )
                        }
                    )
                }
            }
        }

        // Notifications & Tile Card
        item(key = "notifications") {
            PortalCard {
                PortalListRow(
                    title = "Notificações do aplicativo",
                    subtitle = "Exibir controles rápidos fixos na barra de notificações",
                    leading = {
                        AppleIconBadge(backgroundColor = colors.red) {
                            Text("🔔", fontSize = 13.sp)
                        }
                    },
                    trailing = {
                        AppleToggle(
                            checked = state.notificationsEnabled,
                            onCheckedChange = onToggleNotifications
                        )
                    }
                )
                PortalDivider(startIndent = 42.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Dica: toque no botão 'QCY Ruído' nas Configurações Rápidas do Android para ciclar ANC/Ambiente/OFF, ou segure o botão para abrir este aplicativo direto.",
                    style = AppleTypography.caption1,
                    color = colors.secondaryLabel
                )
            }
        }

        // UI Style Card
        item(key = "ui_style") {
            PortalCard {
                Text(
                    text = "Aparência da interface",
                    style = AppleTypography.headline,
                    color = colors.label
                )
                Spacer(modifier = Modifier.height(6.dp))
                UiStyle.entries.forEachIndexed { idx, style ->
                    if (idx > 0) PortalDivider()
                    val selected = state.uiStyle == style
                    PortalListRow(
                        title = style.label,
                        subtitle = style.subtitle,
                        trailing = {
                            if (selected) {
                                Text(
                                    text = "✓",
                                    style = AppleTypography.headline,
                                    color = colors.accent
                                )
                            }
                        },
                        onClick = { onSelectUiStyle(style) }
                    )
                }
            }
        }

        // Updates Card
        item(key = "updates") {
            PortalCard {
                Text(
                    text = "Atualizações do aplicativo",
                    style = AppleTypography.headline,
                    color = colors.label
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Escolha de onde o aplicativo deve buscar novas versões",
                    style = AppleTypography.caption1,
                    color = colors.secondaryLabel
                )
                Spacer(modifier = Modifier.height(12.dp))

                PortalSegmentedControl(
                    items = UpdateSource.entries,
                    selectedItem = state.updateSource,
                    labelProvider = { "${it.label} (${it.hostLabel})" },
                    onSelect = onSelectUpdateSource
                )

                Spacer(modifier = Modifier.height(10.dp))
                PortalDivider()

                PortalListRow(
                    title = "Verificar ao abrir o aplicativo",
                    subtitle = "Versão atual: v${BuildConfig.VERSION_NAME} · $updateStatusText",
                    trailing = {
                        AppleToggle(
                            checked = state.autoCheckUpdates,
                            onCheckedChange = onToggleAutoCheckUpdates
                        )
                    }
                )

                if (downloadingPercent != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (downloadingPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = colors.accent,
                        trackColor = colors.systemFill
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(colors.systemFill)
                            .applePressScale(
                                enabled = !checkingUpdate && downloadingPercent == null,
                                onClick = onCheckUpdateNow
                            )
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (checkingUpdate) {
                            AppleActivityIndicator(size = 18.dp, color = colors.label)
                        } else {
                            Text(
                                text = "Procurar atualização",
                                style = AppleTypography.footnote.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.label,
                                maxLines = 1
                            )
                        }
                    }

                    if (availableRelease != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(colors.accent)
                                .applePressScale(
                                    enabled = downloadingPercent == null,
                                    onClick = { onDownloadAndInstallUpdate(availableRelease) }
                                )
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Instalar ${availableRelease.tagName}",
                                style = AppleTypography.footnote.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppleBatteryRingColumn(
    label: String,
    level: Int,
    charging: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppleColors.current
    val valid = level in 0..100
    val ringColor = when {
        !valid -> colors.tertiaryLabel
        charging -> colors.accent
        level <= 15 -> colors.red
        level <= 25 -> colors.orange
        else -> colors.green
    }
    val progress = if (valid) (level / 100f).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ProgressRing(
            progress = progress,
            color = ringColor,
            trackColor = colors.systemFill.copy(alpha = 0.4f),
            size = 58.dp,
            strokeWidth = 5.5.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (valid) "$level%" else "--",
                    style = AppleTypography.footnote.copy(fontWeight = FontWeight.SemiBold).tabular(),
                    color = colors.label
                )
                if (charging) {
                    Text(
                        text = "⚡",
                        fontSize = 10.sp,
                        color = colors.accent
                    )
                }
            }
        }
        Text(
            text = label,
            style = AppleTypography.caption1.copy(fontWeight = FontWeight.Medium),
            color = colors.secondaryLabel
        )
    }
}

@Composable
private fun AppleNoiseSlider(
    title: String,
    currentLevel: Int,
    levelCount: Int,
    ancCmdId: Int,
    levelText: (Int) -> String,
    onCommitLevel: (Int) -> Unit
) {
    val colors = LocalAppleColors.current
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
                style = AppleTypography.caption1.copy(fontWeight = FontWeight.Medium),
                color = colors.secondaryLabel
            )
            Text(
                text = levelText(displayLevel),
                style = AppleTypography.caption1.copy(fontWeight = FontWeight.SemiBold).tabular(),
                color = colors.accent
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
                thumbColor = Color.White,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.systemFill,
                activeTickColor = Color.White.copy(alpha = 0.6f),
                inactiveTickColor = colors.tertiaryLabel
            )
        )
    }
}
