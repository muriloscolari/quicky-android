package me.muriloscolari.quicky.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

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

    private var overlayDialog: Dialog? = null
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
        val dialog = overlayDialog
        val view = overlayView
        val wm = windowManager
        val owner = overlayLifecycleOwner
        overlayDialog = null
        overlayView = null
        overlayLifecycleOwner = null
        windowManager = null

        if (dialog != null) {
            runCatching {
                dialog.window?.setWindowAnimations(0)
                dialog.dismiss()
            }
        }
        if (view != null && wm != null) {
            runCatching { wm.removeViewImmediate(view) }
        }
        owner?.destroy()
    }

    private fun dismissWithWindowAnimation() {
        dismissStateTrigger = null
        val dialog = overlayDialog
        if (dialog != null) {
            overlayDialog = null
            runCatching { dialog.dismiss() }
                .onFailure { removeOverlayImmediately() }
            return
        }
        removeOverlayImmediately()
    }

    private fun dpToPx(context: Context, dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)

    private fun computeCardWidthPx(context: Context, wm: WindowManager): Int {
        val screenWidthPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { wm.currentWindowMetrics.bounds.width() }
                .getOrElse { context.resources.displayMetrics.widthPixels }
        } else {
            context.resources.displayMetrics.widthPixels
        }
        val horizontalMarginsPx = dpToPx(context, 32f).toInt()
        val maxCardWidthPx = dpToPx(context, 420f).toInt()
        return min((screenWidthPx - horizontalMarginsPx).coerceAtLeast(dpToPx(context, 260f).toInt()), maxCardWidthPx)
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
        val cardWidthPx = computeCardWidthPx(overlayCtx, wm)
        val hasLiveWindowBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            runCatching { wm.isCrossWindowBlurEnabled }.getOrDefault(false)

        val owner = OverlayLifecycleOwner()
        owner.createAndResume()
        var activeWindowHasBlur = false

        val composeView = ComposeView(overlayCtx).apply {
            setContent {
                AirPodsPopupOverlayContent(
                    isPreview = isPreview,
                    hasLiveWindowBlur = activeWindowHasBlur,
                    onRegisterDismissTrigger = { trigger ->
                        dismissStateTrigger = trigger
                    },
                    onDismissed = {
                        dismissWithWindowAnimation()
                    }
                )
            }
        }

        // Primary path: TYPE_APPLICATION_OVERLAY Dialog supports Window.setBackgroundBlurRadius()
        // clipped cleanly to the 30dp rounded pill corners + smooth native SurfaceFlinger animations.
        val dialogShown = runCatching {
            val themedCtx = ContextThemeWrapper(overlayCtx, android.R.style.Theme_Translucent_NoTitleBar)
            val dialog = Dialog(themedCtx).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(false)
                setCanceledOnTouchOutside(true)
            }

            val cornerPx = dpToPx(overlayCtx, 30f)
            val blurPx = dpToPx(overlayCtx, 40f).toInt()
            val bottomMarginPx = dpToPx(overlayCtx, 14f).toInt()

            val window = dialog.window ?: error("Dialog window unavailable")
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.addFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            )
            window.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            window.setLayout(cardWidthPx, WindowManager.LayoutParams.WRAP_CONTENT)
            window.setWindowAnimations(R.style.LiquidGlassPopupWindowAnimation)
            window.attributes = window.attributes.apply {
                format = PixelFormat.TRANSLUCENT
                y = bottomMarginPx
            }

            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerPx
                // Non-zero alpha ensures GradientDrawable.getOutline() provides a rounded outline
                // so Android 12+ clips live cross-window blur cleanly to the pill shape.
                setColor(if (hasLiveWindowBlur) 0x02000000 else android.graphics.Color.TRANSPARENT)
            }
            window.setBackgroundDrawable(bgDrawable)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasLiveWindowBlur) {
                window.setBackgroundBlurRadius(blurPx)
                activeWindowHasBlur = true
            }

            window.decorView.apply {
                clipToOutline = true
                outlineProvider = ViewOutlineProvider.BACKGROUND
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE) {
                        dismissStateTrigger?.invoke() ?: dismissWithWindowAnimation()
                    }
                    false
                }
            }

            dialog.setOnDismissListener {
                if (overlayLifecycleOwner === owner) {
                    overlayDialog = null
                    overlayLifecycleOwner = null
                    dismissStateTrigger = null
                    owner.destroy()
                }
            }

            dialog.setContentView(composeView)
            dialog.show()
            overlayDialog = dialog
            overlayLifecycleOwner = owner
            true
        }.getOrDefault(false)

        if (dialogShown) return

        // Fallback path: direct WindowManager.addView if OEM blocks TYPE_APPLICATION_OVERLAY Dialog
        activeWindowHasBlur = false
        composeView.setViewTreeLifecycleOwner(owner)
        composeView.setViewTreeSavedStateRegistryOwner(owner)
        composeView.setViewTreeViewModelStoreOwner(owner)
        composeView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismissStateTrigger?.invoke() ?: removeOverlayImmediately()
            }
            false
        }

        val flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        val params = WindowManager.LayoutParams(
            cardWidthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dpToPx(overlayCtx, 14f).toInt()
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
    hasLiveWindowBlur: Boolean,
    onRegisterDismissTrigger: (() -> Unit) -> Unit,
    onDismissed: () -> Unit
) {
    val state by QcyBleManager.state.collectAsState()
    val colors = rememberAppleColors()
    val haptics = rememberAppleHaptics()

    var dismissing by remember { androidx.compose.runtime.mutableStateOf(false) }
    var hadActiveLink by remember { androidx.compose.runtime.mutableStateOf(false) }
    var interactionNonce by remember { mutableIntStateOf(0) }

    val triggerDismiss: () -> Unit = {
        if (!dismissing) {
            dismissing = true
            onDismissed()
        }
    }

    LaunchedEffect(Unit) {
        onRegisterDismissTrigger(triggerDismiss)
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
        val cardShape = RoundedCornerShape(30.dp)
        val baseGlassFill = when {
            hasLiveWindowBlur && colors.isDark -> Color(0xFF13151A).copy(alpha = 0.50f)
            hasLiveWindowBlur -> Color(0xFFF7F9FF).copy(alpha = 0.56f)
            colors.isDark -> Color(0xFF17191E).copy(alpha = 0.88f)
            else -> Color(0xFFF6F8FC).copy(alpha = 0.90f)
        }
        val specularBorderBrush = if (colors.isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.14f),
                    Color(0xFF64D2FF).copy(alpha = 0.28f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.96f),
                    Color.White.copy(alpha = 0.30f),
                    Color.Black.copy(alpha = 0.12f)
                )
            )
        }
        val lensSheenBrush = if (colors.isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.03f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.05f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.20f)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(baseGlassFill)
                .background(lensSheenBrush)
                .border(1.1.dp, specularBorderBrush, cardShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    interactionNonce++
                }
        ) {
            if (!hasLiveWindowBlur) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.accent.copy(alpha = if (colors.isDark) 0.18f else 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.18f, size.height * 0.12f),
                            radius = size.width * 0.72f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.green.copy(alpha = if (colors.isDark) 0.12f else 0.09f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.65f),
                            radius = size.width * 0.60f
                        )
                    )
                }
            }

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
                        .background(colors.secondaryLabel.copy(alpha = 0.38f))
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    PopupEarbudsEmblem(
                        accentColor = if (state.connected) colors.green else colors.accent,
                        isDark = colors.isDark
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = state.modelTitle,
                            style = AppleTypography.title3,
                            color = colors.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
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

@Composable
private fun PopupEarbudsEmblem(
    accentColor: Color,
    isDark: Boolean
) {
    val badgeShape = RoundedCornerShape(14.dp)
    val tintBg = if (isDark) Color.White.copy(alpha = 0.11f) else Color.White.copy(alpha = 0.55f)
    val rimColor = if (isDark) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.85f)
    val iconColor = if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF1C1C1E)

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(badgeShape)
            .background(tintBg)
            .border(0.8.dp, rimColor, badgeShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            // Glow behind earbuds
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.40f), Color.Transparent)
                ),
                radius = w * 0.65f
            )
            val stroke = Stroke(width = w * 0.085f)
            // Left earbud silhouette
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(w * 0.10f, h * 0.14f),
                size = Size(w * 0.30f, h * 0.28f),
                cornerRadius = CornerRadius(w * 0.14f)
            )
            drawLine(
                color = iconColor,
                start = Offset(w * 0.25f, h * 0.40f),
                end = Offset(w * 0.25f, h * 0.84f),
                strokeWidth = stroke.width
            )
            // Right earbud silhouette
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(w * 0.60f, h * 0.14f),
                size = Size(w * 0.30f, h * 0.28f),
                cornerRadius = CornerRadius(w * 0.14f)
            )
            drawLine(
                color = iconColor,
                start = Offset(w * 0.75f, h * 0.40f),
                end = Offset(w * 0.75f, h * 0.84f),
                strokeWidth = stroke.width
            )
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
