package me.muriloscolari.quicky.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.muriloscolari.quicky.MainActivity
import me.muriloscolari.quicky.R
import me.muriloscolari.quicky.ble.AncCycleMode
import me.muriloscolari.quicky.ble.QcyBleManager
import me.muriloscolari.quicky.ble.QcyProtocol
import java.util.concurrent.atomic.AtomicLong

/**
 * Floating AirPods-style pop-up sheet rendered via WindowManager (TYPE_APPLICATION_OVERLAY).
 * Automatically falls back to a transient connect notification when overlay permission is missing.
 */
object AirPodsPopupController {
    private const val BURST_DEDUP_MS = 8_000L
    private const val AUTO_DISMISS_MS = 6_000L
    private const val FALLBACK_CHANNEL_ID = "quicky_connect_popup"
    private const val FALLBACK_NOTIFICATION_ID = 1009

    private val mainHandler = Handler(Looper.getMainLooper())
    private val popupGeneration = AtomicLong(0L)
    @Volatile
    private var lastShownElapsedMs: Long = -BURST_DEDUP_MS
    @Volatile
    private var sessionShown: Boolean = false
    @Volatile
    private var deferredUntilUnlock: Boolean = false
    @Volatile
    private var pendingShowRunnable: Runnable? = null

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var dismissStateTrigger: (() -> Unit)? = null

    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun isScreenUnlockedAndInteractive(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val interactive = pm?.isInteractive ?: true
        if (!interactive) return false
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val locked = km?.isKeyguardLocked ?: false
        return !locked
    }

    /**
     * Resets the current connection session so the next connect pops up immediately
     * (even if earbuds were put back in the case for only a couple seconds).
     */
    fun resetSession() {
        popupGeneration.incrementAndGet()
        pendingShowRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingShowRunnable = null
        sessionShown = false
        deferredUntilUnlock = false
        lastShownElapsedMs = -BURST_DEDUP_MS
    }

    /**
     * Called on audio/Bluetooth disconnect or BT radio off: invalidates any queued popup,
     * resets session state, and removes any overlay immediately without exit flashes.
     */
    fun onDisconnected() {
        resetSession()
        val removeTask = { removeOverlayImmediately() }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            removeTask()
        } else {
            mainHandler.post(removeTask)
        }
    }

    /**
     * If earbuds connected while the screen was locked, show the pop-up once the user unlocks.
     */
    fun onUserUnlocked(context: Context) {
        if (!deferredUntilUnlock) return
        val st = QcyBleManager.state.value
        if (!st.airpodsPopupEnabled || !st.audioConnected || !QcyBleManager.isBluetoothEnabled()) {
            deferredUntilUnlock = false
            return
        }
        deferredUntilUnlock = false
        showInstant(context)
    }

    /**
     * Manual preview triggered from Ajustes so the user can test overlay rendering anytime.
     */
    fun showPreview(context: Context) {
        showInstant(context, ignoreSessionGuard = true, isPreview = true)
    }

    /**
     * Shows the AirPods-style pop-up immediately upon Bluetooth Audio / BLE connect,
     * deduplicating the multi-broadcast burst (ACL + HFP + A2DP + GATT) within a single session.
     */
    fun showInstant(
        context: Context,
        ignoreSessionGuard: Boolean = false,
        isPreview: Boolean = false
    ) {
        val appCtx = context.applicationContext
        val state = QcyBleManager.state.value
        if (!isPreview) {
            if (!state.airpodsPopupEnabled) return
            if (!QcyBleManager.isBluetoothEnabled()) return
            if (!isScreenUnlockedAndInteractive(appCtx)) {
                deferredUntilUnlock = true
                return
            }
        }

        val now = SystemClock.elapsedRealtime()
        if (!ignoreSessionGuard && !isPreview) {
            if (sessionShown || (now - lastShownElapsedMs < BURST_DEDUP_MS)) {
                return
            }
        }
        if (!isPreview) {
            sessionShown = true
            deferredUntilUnlock = false
            lastShownElapsedMs = now
        }

        pendingShowRunnable?.let { mainHandler.removeCallbacks(it) }
        val gen = popupGeneration.incrementAndGet()
        val runnable = Runnable {
            if (popupGeneration.get() != gen) return@Runnable
            val currentState = QcyBleManager.state.value
            if (!isPreview) {
                if (!currentState.airpodsPopupEnabled || !QcyBleManager.isBluetoothEnabled()) {
                    return@Runnable
                }
                if (!currentState.audioConnected && !currentState.connected) {
                    return@Runnable
                }
            }
            if (canDrawOverlays(appCtx)) {
                showWindowOverlay(appCtx, isPreview)
            } else {
                showFallbackNotification(appCtx)
            }
        }
        pendingShowRunnable = runnable
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            mainHandler.post(runnable)
        }
    }

    fun dismiss() {
        popupGeneration.incrementAndGet()
        pendingShowRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingShowRunnable = null
        mainHandler.post {
            val trigger = dismissStateTrigger
            if (trigger != null) {
                trigger()
            } else {
                removeOverlayImmediately()
            }
        }
    }

    private fun removeOverlayImmediately() {
        dismissStateTrigger = null
        val view = overlayView
        val wm = windowManager
        val owner = overlayLifecycleOwner
        overlayView = null
        overlayLifecycleOwner = null
        windowManager = null

        if (view != null && wm != null) {
            runCatching { wm.removeViewImmediate(view) }
        }
        owner?.destroy()
    }

    private fun resolveOverlayContext(appCtx: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return runCatching {
                val dm = appCtx.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
                if (display != null) {
                    appCtx.createWindowContext(
                        display,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        null
                    )
                } else {
                    appCtx
                }
            }.getOrDefault(appCtx)
        }
        return appCtx
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showWindowOverlay(appCtx: Context, isPreview: Boolean) {
        removeOverlayImmediately()
        val overlayCtx = resolveOverlayContext(appCtx)
        val wm = overlayCtx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            ?: appCtx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            ?: return
        val blurBehindSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            runCatching { wm.isCrossWindowBlurEnabled }.getOrDefault(false)
        val owner = OverlayLifecycleOwner()
        owner.createAndResume()

        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
            if (blurBehindSupported) WindowManager.LayoutParams.FLAG_BLUR_BEHIND else 0

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            dimAmount = if (blurBehindSupported) 0.24f else 0.36f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurBehindSupported) {
                blurBehindRadius = 56
            }
        }

        val composeView = ComposeView(overlayCtx).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                AirPodsPopupOverlayContent(
                    isPreview = isPreview,
                    hasLiveBlurBehind = blurBehindSupported,
                    onRegisterDismissTrigger = { trigger ->
                        dismissStateTrigger = trigger
                    },
                    onDismissed = {
                        removeOverlayImmediately()
                    }
                )
            }
        }

        runCatching {
            wm.addView(composeView, params)
            windowManager = wm
            overlayView = composeView
            overlayLifecycleOwner = owner
        }.onFailure {
            owner.destroy()
            showFallbackNotification(appCtx)
        }
    }

    private fun showFallbackNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FALLBACK_CHANNEL_ID,
                "Pop-up de Conexão QCY",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aviso rápido ao conectar o fone (substitui o pop-up flutuante sem permissão de sobreposição)"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val state = QcyBleManager.state.value
        val bat = state.battery
        val batParts = buildList {
            if (bat.left in 0..100) add("Esq ${bat.left}%")
            if (bat.right in 0..100) add("Dir ${bat.right}%")
            if (bat.caseLevel > 0 || bat.caseCharging) add("Estojo ${bat.caseLevel}%")
        }
        val summary = if (batParts.isNotEmpty()) {
            batParts.joinToString(" · ") + " · ${state.ancCycleMode.label}"
        } else {
            "Conectado · ${state.ancCycleMode.label}"
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            21,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, FALLBACK_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(R.drawable.ic_headphones)
            .setContentTitle("${state.modelTitle} conectado")
            .setContentText(summary)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .build()

        runCatching { nm.notify(FALLBACK_NOTIFICATION_ID, notification) }
    }

    private class OverlayLifecycleOwner :
        LifecycleOwner,
        SavedStateRegistryOwner,
        ViewModelStoreOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        override val viewModelStore: ViewModelStore
            get() = store

        fun createAndResume() {
            savedStateController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun destroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }
    }
}

@Composable
private fun AirPodsPopupOverlayContent(
    isPreview: Boolean,
    hasLiveBlurBehind: Boolean,
    onRegisterDismissTrigger: (() -> Unit) -> Unit,
    onDismissed: () -> Unit
) {
    val state by QcyBleManager.state.collectAsState()
    val colors = rememberAppleColors()
    val haptics = rememberAppleHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 180.dp.toPx() }

    val progress = remember { Animatable(1f) }
    var dismissing by remember { androidx.compose.runtime.mutableStateOf(false) }
    var hadActiveLink by remember { androidx.compose.runtime.mutableStateOf(false) }
    var interactionNonce by remember { mutableIntStateOf(0) }

    val triggerDismiss: () -> Unit = {
        if (!dismissing) {
            dismissing = true
            scope.launch {
                progress.animateTo(1f, animationSpec = tween(durationMillis = 200))
                onDismissed()
            }
        }
    }

    LaunchedEffect(Unit) {
        onRegisterDismissTrigger(triggerDismiss)
        progress.animateTo(0f, animationSpec = AppleMotion.criticalSpring())
    }

    // Auto-dismiss 6s after appear or last interaction
    LaunchedEffect(dismissing, interactionNonce) {
        if (!dismissing) {
            delay(6_000L)
            triggerDismiss()
        }
    }

    // Dismiss early only if both BT Audio and BLE GATT drop after having been active
    LaunchedEffect(state.connected, state.audioConnected, isPreview) {
        if (isPreview) return@LaunchedEffect
        if (state.connected || state.audioConnected) {
            hadActiveLink = true
        } else if (hadActiveLink && !dismissing) {
            triggerDismiss()
        }
    }

    CompositionLocalProvider(
        LocalAppleColors provides colors,
        LocalAppleHaptics provides haptics
    ) {
        val cardShape = RoundedCornerShape(32.dp)
        val glassFill = when {
            hasLiveBlurBehind && colors.isDark -> Color(0xFF1A1B20).copy(alpha = 0.72f)
            hasLiveBlurBehind -> Color(0xFFFBFBFD).copy(alpha = 0.78f)
            colors.isDark -> Color(0xFF1A1B20).copy(alpha = 0.92f)
            else -> Color(0xFFFBFBFD).copy(alpha = 0.94f)
        }
        val specularBorder = if (colors.isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.36f),
                    Color.White.copy(alpha = 0.09f),
                    Color.White.copy(alpha = 0.20f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f),
                    Color.White.copy(alpha = 0.42f),
                    Color.Black.copy(alpha = 0.08f)
                )
            )
        }
        val topSheen = if (colors.isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.11f),
                    Color.White.copy(alpha = 0.02f),
                    Color.Transparent
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.10f),
                    Color.Transparent
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    triggerDismiss()
                }
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = progress.value * slideOffsetPx
                        alpha = (1f - progress.value).coerceIn(0f, 1f)
                    }
                    .shadow(
                        elevation = 24.dp,
                        shape = cardShape,
                        ambientColor = Color.Black.copy(alpha = 0.40f),
                        spotColor = Color.Black.copy(alpha = 0.48f)
                    )
                    .clip(cardShape)
                    .background(glassFill)
                    .background(topSheen)
                    .border(0.85.dp, specularBorder, cardShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        interactionNonce++
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with centered title + sleek iOS circular close button
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 38.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.modelTitle,
                                style = AppleTypography.title3,
                                color = colors.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (state.connected) colors.green else colors.accent)
                                )
                                Text(
                                    text = if (state.connected) {
                                        "Conectado · ${QcyProtocol.formatAncShortLabel(state.ancCmdId)}"
                                    } else if (isPreview) {
                                        "Liquid Glass · Pré-visualização"
                                    } else {
                                        "Áudio conectado · sincronizando..."
                                    },
                                    style = AppleTypography.footnote,
                                    color = colors.secondaryLabel
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    if (colors.isDark) Color.White.copy(alpha = 0.14f)
                                    else Color.Black.copy(alpha = 0.08f)
                                )
                                .clickable {
                                    haptics.tick()
                                    triggerDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.secondaryLabel
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    val bat = state.battery
                    val hasCase = bat.caseLevel > 0 || bat.caseCharging
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PopupBatteryRing(
                            label = "Esquerdo",
                            level = bat.left,
                            charging = bat.leftCharging
                        )
                        PopupBatteryRing(
                            label = "Direito",
                            level = bat.right,
                            charging = bat.rightCharging
                        )
                        if (hasCase) {
                            PopupBatteryRing(
                                label = "Estojo",
                                level = bat.caseLevel,
                                charging = bat.caseCharging
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    PortalSegmentedControl(
                        items = AncCycleMode.entries,
                        selectedItem = state.ancCycleMode,
                        labelProvider = { it.label },
                        onSelect = { mode ->
                            interactionNonce++
                            QcyBleManager.setAncCycleMode(mode)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PopupBatteryRing(
    label: String,
    level: Int,
    charging: Boolean
) {
    val colors = LocalAppleColors.current
    val ringColor = when {
        level < 0 -> colors.secondaryLabel.copy(alpha = 0.4f)
        level <= 20 -> colors.red
        level <= 45 -> colors.orange
        else -> colors.green
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = if (level in 0..100) level / 100f else 0f,
                color = ringColor,
                trackColor = colors.systemFill,
                size = 64.dp,
                strokeWidth = 6.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (level in 0..100) "$level%" else "--",
                    style = AppleTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.label
                )
                if (charging) {
                    Text(
                        text = "⚡",
                        fontSize = 10.sp,
                        color = colors.green
                    )
                }
            }
        }
        Text(
            text = label,
            style = AppleTypography.caption1,
            color = colors.secondaryLabel
        )
    }
}
