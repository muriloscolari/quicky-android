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
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
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
import me.muriloscolari.quicky.MainActivity
import me.muriloscolari.quicky.R
import me.muriloscolari.quicky.ble.AncCycleMode
import me.muriloscolari.quicky.ble.QcyBleManager
import me.muriloscolari.quicky.ble.QcyProtocol

/**
 * Floating AirPods-style pop-up sheet rendered via WindowManager (TYPE_APPLICATION_OVERLAY).
 * Automatically falls back to a transient connect notification when overlay permission is missing.
 */
object AirPodsPopupController {
    private const val COOLDOWN_MS = 60_000L
    private const val AUTO_DISMISS_MS = 6_000L
    private const val FALLBACK_CHANNEL_ID = "quicky_connect_popup"
    private const val FALLBACK_NOTIFICATION_ID = 1009

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastShownElapsedMs: Long = -COOLDOWN_MS

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
     * Triggers the AirPods-style pop-up on a genuine disconnected -> connected transition,
     * respecting screen unlock state and 60s anti-spam cooldown unless `ignoreCooldown` is true.
     */
    fun showOnConnect(context: Context, ignoreCooldown: Boolean = false) {
        val appCtx = context.applicationContext
        val now = SystemClock.elapsedRealtime()
        if (!ignoreCooldown && now - lastShownElapsedMs < COOLDOWN_MS) {
            return
        }
        if (!isScreenUnlockedAndInteractive(appCtx)) {
            return
        }
        if (!QcyBleManager.state.value.connected) {
            return
        }
        lastShownElapsedMs = now

        mainHandler.post {
            if (!QcyBleManager.state.value.connected) return@post
            if (canDrawOverlays(appCtx)) {
                showWindowOverlay(appCtx)
            } else {
                showFallbackNotification(appCtx)
            }
        }
    }

    fun dismiss() {
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

    @SuppressLint("ClickableViewAccessibility")
    private fun showWindowOverlay(appCtx: Context) {
        removeOverlayImmediately()
        val wm = appCtx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val owner = OverlayLifecycleOwner()
        owner.createAndResume()

        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        val composeView = ComposeView(appCtx).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                AirPodsPopupOverlayContent(
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
    onRegisterDismissTrigger: (() -> Unit) -> Unit,
    onDismissed: () -> Unit
) {
    val state by QcyBleManager.state.collectAsState()
    val colors = rememberAppleColors()
    val haptics = rememberAppleHaptics()

    var visible by remember { mutableStateOf(false) }
    var hasBeenShown by remember { mutableStateOf(false) }
    var interactionNonce by remember { mutableIntStateOf(0) }

    val triggerDismiss = {
        if (visible) {
            visible = false
        }
    }

    LaunchedEffect(Unit) {
        onRegisterDismissTrigger(triggerDismiss)
        hasBeenShown = true
        visible = true
    }

    // Auto-dismiss 6s after appear or last interaction
    LaunchedEffect(visible, hasBeenShown, interactionNonce) {
        if (visible) {
            delay(6_000L)
            visible = false
        } else if (hasBeenShown) {
            // Wait for exit animation before detaching WindowManager view
            delay(280L)
            onDismissed()
        }
    }

    // Dismiss if disconnected while visible
    LaunchedEffect(state.connected) {
        if (!state.connected && visible) {
            visible = false
        }
    }

    CompositionLocalProvider(
        LocalAppleColors provides colors,
        LocalAppleHaptics provides haptics
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (visible) Color.Black.copy(alpha = 0.28f) else Color.Transparent)
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
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = AppleMotion.criticalSpring(),
                    initialOffsetY = { it / 2 }
                ) + fadeIn(),
                exit = slideOutVertically(
                    animationSpec = AppleMotion.criticalSpring(),
                    targetOffsetY = { it }
                ) + fadeOut()
            ) {
                // Use translucent fallback (`useHaze = false`) because WindowManager has no HazeSource
                GlassSurface(
                    hazeState = null,
                    useHaze = false,
                    cornerRadius = 30.dp,
                    specular = 0.56f,
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
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
                        // Grabber handle
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(colors.secondaryLabel.copy(alpha = 0.35f))
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = state.modelTitle,
                            style = AppleTypography.title3,
                            color = colors.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(colors.green)
                            )
                            Text(
                                text = "Conectado · ${QcyProtocol.formatAncShortLabel(state.ancCmdId)}",
                                style = AppleTypography.footnote,
                                color = colors.secondaryLabel
                            )
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

                        Spacer(modifier = Modifier.height(14.dp))

                        ApplePillButton(
                            text = "Fechar",
                            prominent = false,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { triggerDismiss() }
                        )
                    }
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
