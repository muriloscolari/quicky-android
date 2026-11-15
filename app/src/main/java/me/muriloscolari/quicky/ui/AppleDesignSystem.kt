package me.muriloscolari.quicky.ui

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import kotlin.math.roundToInt

@Immutable
data class AppleColors(
    val isDark: Boolean,
    val background: Color,
    val grouped: Color,
    val tertiaryGrouped: Color,
    val systemFill: Color,
    val divider: Color,
    val accent: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val glassTint: Color,
    val glassFallbackBg: Color,
    val glassBorder: Color,
    val segmentedTrack: Color,
    val segmentedThumb: Color
)

val LightAppleColors = AppleColors(
    isDark = false,
    background = Color(0xFFF2F2F7),
    grouped = Color(0xFFFFFFFF),
    tertiaryGrouped = Color(0xFFFFFFFF),
    systemFill = Color(0xFF787880).copy(alpha = 0.20f),
    divider = Color(0xFF3C3C43).copy(alpha = 0.18f),
    accent = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    orange = Color(0xFFFF9500),
    red = Color(0xFFFF3B30),
    label = Color(0xFF000000),
    secondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.60f),
    tertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.30f),
    glassTint = Color(0xFFFFFFFF).copy(alpha = 0.62f),
    glassFallbackBg = Color(0xFFF9F9FB).copy(alpha = 0.88f),
    glassBorder = Color(0xFF000000).copy(alpha = 0.08f),
    segmentedTrack = Color(0xFF787880).copy(alpha = 0.16f),
    segmentedThumb = Color(0xFFFFFFFF)
)

val DarkAppleColors = AppleColors(
    isDark = true,
    background = Color(0xFF000000),
    grouped = Color(0xFF1C1C1E),
    tertiaryGrouped = Color(0xFF2C2C2E),
    systemFill = Color(0xFF787880).copy(alpha = 0.36f),
    divider = Color(0xFF545458).copy(alpha = 0.48f),
    accent = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    orange = Color(0xFFFF9F0A),
    red = Color(0xFFFF453A),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.60f),
    tertiaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.30f),
    glassTint = Color(0xFF1C1C1E).copy(alpha = 0.56f),
    glassFallbackBg = Color(0xFF1C1C1E).copy(alpha = 0.88f),
    glassBorder = Color(0xFFFFFFFF).copy(alpha = 0.14f),
    segmentedTrack = Color(0xFF787880).copy(alpha = 0.28f),
    segmentedThumb = Color(0xFF3A3A3C)
)

val LocalAppleColors = compositionLocalOf { DarkAppleColors }

@Composable
fun rememberAppleColors(darkTheme: Boolean = isSystemInDarkTheme()): AppleColors {
    return remember(darkTheme) { if (darkTheme) DarkAppleColors else LightAppleColors }
}

object AppleTypography {
    val largeTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp
    )
    val title3 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    )
    val headline = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold
    )
    val body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal
    )
    val subheadline = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    )
    val footnote = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal
    )
    val caption1 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal
    )
    val caption2 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Medium
    )
}

fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

object AppleMotion {
    fun <T> criticalSpring(stiffness: Float = Spring.StiffnessMediumLow) = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = stiffness
    )

    val dpSpring = criticalSpring<Dp>(Spring.StiffnessMediumLow)
    val floatSpring = criticalSpring<Float>(Spring.StiffnessMediumLow)
    val colorSpring = criticalSpring<Color>(Spring.StiffnessMedium)
}

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        }.getOrDefault(false)
    }
}

/**
 * Subtle atmospheric radial backdrop that guards against 0-radius Android crashes.
 */
@Composable
fun AppleMeshBackdrop(
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val radiusBase = (minOf(w, h) * 0.78f).coerceAtLeast(1f)
        val alpha = (if (isDark) 0.16f else 0.10f).coerceIn(0f, 1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = alpha),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.06f),
                radius = radiusBase
            ),
            radius = radiusBase,
            center = Offset(w * 0.5f, h * 0.06f)
        )
    }
}

/**
 * Apple Liquid Glass surface powered by Haze 2.1 `hazeGlass` with automatic translucent fallback.
 *
 * - All lighting floats are strictly kept within 0f..1f.
 * - Hover/press interactive spotlight is disabled via `lightingIntensity(0f)`.
 * - Set `useHaze = false` inside Popup/Dialog windows to avoid cross-window Haze issues.
 */
@Composable
fun GlassSurface(
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    useHaze: Boolean = true,
    specular: Float = 0.46f,
    tintOverride: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalAppleColors.current
    val reduceMotion = rememberReduceMotion()
    val safeCorner = cornerRadius.coerceAtLeast(0.dp)
    val shape = RoundedCornerShape(safeCorner)
    val canUseHaze = useHaze && hazeState != null && !reduceMotion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val effectiveTint = tintOverride ?: colors.glassTint

    val safeSpecular = if (specular.isFinite()) specular.coerceIn(0f, 1f) else 0.46f

    val glassModifier = if (canUseHaze && hazeState != null) {
        val style = remember(safeCorner, effectiveTint, safeSpecular, colors.isDark) {
            GlassStyle.regular.then {
                shape(RoundedCornerShape(safeCorner))
                tint(effectiveTint)
                specularIntensity(safeSpecular)
                ambientResponse(0.15f)
                hovered {
                    lightingIntensity(0f)
                }
                pressed {
                    lightingIntensity(0f)
                    whitePointDelta(0.06f)
                }
            }
        }
        Modifier
            .clip(shape)
            .hazeGlass(
                input = HazeInput.Backdrop(hazeState),
                style = style
            )
    } else {
        val topSheen = if (colors.isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.35f)
        Modifier
            .clip(shape)
            .background(colors.glassFallbackBg)
            .background(
                Brush.verticalGradient(
                    colors = listOf(topSheen, Color.Transparent)
                )
            )
            .border(0.75.dp, colors.glassBorder, shape)
    }

    Box(
        modifier = modifier.then(glassModifier),
        content = content
    )
}

/**
 * iOS Settings-style grouped card: RoundedCornerShape(20.dp), no border, subtle elevation in light mode.
 */
@Composable
fun PortalCard(
    modifier: Modifier = Modifier,
    insideSheet: Boolean = false,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppleColors.current
    val shape = RoundedCornerShape(20.dp)
    val bgColor = if (insideSheet) colors.tertiaryGrouped else colors.grouped
    val shadowMod = if (!colors.isDark && !insideSheet) {
        Modifier.shadow(
            elevation = 2.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.05f),
            spotColor = Color.Black.copy(alpha = 0.05f)
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(shadowMod)
            .clip(shape)
            .background(bgColor)
            .padding(contentPadding.coerceAtLeast(0.dp)),
        content = content
    )
}

@Composable
fun PortalDivider(
    startIndent: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppleColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent.coerceAtLeast(0.dp))
            .height(0.5.dp)
            .background(colors.divider)
    )
}

@Composable
fun AppleIconBadge(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
fun PortalListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalAppleColors.current
    val clickMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickMod)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (leading != null) {
            leading()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppleTypography.subheadline.copy(fontWeight = FontWeight.Medium),
                color = colors.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = AppleTypography.caption1,
                    color = colors.secondaryLabel
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * iOS-style switch with critically damped animation.
 */
@Composable
fun AppleToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppleColors.current
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.green else colors.systemFill,
        animationSpec = AppleMotion.colorSpring,
        label = "appleToggleTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = AppleMotion.dpSpring,
        label = "appleToggleThumb"
    )

    Box(
        modifier = modifier
            .width(51.dp)
            .height(31.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.coerceAtLeast(0.dp))
                .size(27.dp)
                .shadow(2.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.18f))
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * Apple Segmented Control with a single sliding elevated thumb and subtle dividers
 * that fade out adjacent to the selected segment.
 */
@Composable
fun <T> PortalSegmentedControl(
    items: List<T>,
    selectedItem: T,
    labelProvider: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isCapsule: Boolean = false
) {
    if (items.isEmpty()) return
    val colors = LocalAppleColors.current
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    val trackShape = if (isCapsule) CircleShape else RoundedCornerShape(10.dp)
    val thumbShape = if (isCapsule) CircleShape else RoundedCornerShape(8.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(trackShape)
            .background(colors.segmentedTrack)
            .padding(2.dp)
    ) {
        val segmentWidth = (maxWidth / items.size).coerceAtLeast(1.dp)
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = AppleMotion.dpSpring,
            label = "segmentedThumbOffset"
        )

        // Single sliding thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.coerceAtLeast(0.dp))
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = if (colors.isDark) 0.dp else 2.dp,
                    shape = thumbShape,
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(thumbShape)
                .background(colors.segmentedThumb)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val showDivider = index > 0 && index != selectedIndex && index - 1 != selectedIndex

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(item) },
                    contentAlignment = Alignment.Center
                ) {
                    if (showDivider) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(0.5.dp)
                                .height(16.dp)
                                .background(colors.divider)
                        )
                    }
                    Text(
                        text = labelProvider(item),
                        style = AppleTypography.footnote.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isSelected) colors.label else colors.secondaryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tactile 0.97f press scale modifier using critical damping.
 */
fun Modifier.applePressScale(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberReduceMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduceMotion) 0.97f else 1f,
        animationSpec = AppleMotion.floatSpring,
        label = "applePressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

@Composable
fun ApplePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = true,
    enabled: Boolean = true
) {
    val colors = LocalAppleColors.current
    val bg = if (prominent) colors.accent else colors.systemFill
    val fg = if (prominent) Color.White else colors.accent
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (enabled) bg else bg.copy(alpha = 0.5f))
            .applePressScale(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppleTypography.footnote.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) fg else fg.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppleCircleButton(
    hazeState: HazeState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        hazeState = hazeState,
        cornerRadius = 20.dp,
        modifier = modifier
            .size(38.dp)
            .applePressScale(onClick = onClick),
        content = content
    )
}

/**
 * 8-stroke radial Apple Activity Indicator.
 */
@Composable
fun AppleActivityIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalAppleColors.current.secondaryLabel
) {
    val infiniteTransition = rememberInfiniteTransition(label = "appleSpinner")
    val step by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = LinearEasing
            )
        ),
        label = "spinnerStep"
    )
    Canvas(modifier = modifier.size(size.coerceAtLeast(8.dp))) {
        val w = this.size.width
        val h = this.size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val activeIndex = step.toInt() % 8
        val strokeWidth = (w * 0.11f).coerceAtLeast(1.5f)
        val innerR = w * 0.24f
        val outerR = w * 0.46f
        for (i in 0 until 8) {
            val distance = (i - activeIndex + 8) % 8
            val alpha = (0.20f + (1f - distance / 7f) * 0.75f).coerceIn(0f, 1f)
            rotate(degrees = i * 45f) {
                drawLine(
                    color = color.copy(alpha = alpha),
                    start = Offset(center.x, center.y - innerR),
                    end = Offset(center.x, center.y - outerR),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Circular Progress Ring with smooth critically damped animation.
 */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    strokeWidth: Dp = 5.dp,
    centerContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val safeTarget = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = safeTarget,
        animationSpec = AppleMotion.floatSpring,
        label = "progressRing"
    )
    Box(
        modifier = modifier.size(size.coerceAtLeast(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            if (w <= 0f || h <= 0f) return@Canvas
            val strokePx = strokeWidth.toPx().coerceAtLeast(1f)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            val sweep = (animatedProgress.coerceIn(0f, 1f) * 360f)
            if (sweep > 0.5f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        if (centerContent != null) {
            centerContent()
        }
    }
}

/**
 * Layer 3 Apple Sheet: grabber (36x5.dp), vertical drag-to-dismiss, floating 8.dp inset & 34.dp corners.
 */
@Composable
fun AppleSheet(
    title: String,
    hazeState: HazeState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppleColors.current
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (colors.isDark) 0.45f else 0.30f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        GlassSurface(
            hazeState = hazeState,
            cornerRadius = 34.dp,
            useHaze = false,
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .offset { IntOffset(0, dragOffsetPx.roundToInt().coerceAtLeast(0)) }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffsetPx > 140f) {
                                onDismiss()
                            }
                            dragOffsetPx = 0f
                        },
                        onDragCancel = { dragOffsetPx = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            dragOffsetPx = (dragOffsetPx + dragAmount).coerceAtLeast(0f)
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.grouped.copy(alpha = 0.96f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .heightIn(max = 540.dp)
            ) {
                // Grabber
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(colors.tertiaryLabel)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = AppleTypography.title3,
                        color = colors.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ApplePillButton(
                        text = "Fechar",
                        prominent = false,
                        onClick = onDismiss
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                content()
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/**
 * Layer 3 Apple Alert: compact card (maxWidth = 316.dp), centered text, side-by-side capsule buttons.
 */
@Composable
fun AppleAlert(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmDestructive: Boolean = false
) {
    val colors = LocalAppleColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (colors.isDark) 0.50f else 0.32f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            hazeState = null,
            useHaze = false,
            cornerRadius = 26.dp,
            modifier = Modifier
                .widthIn(max = 316.dp)
                .padding(horizontal = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.grouped.copy(alpha = 0.96f))
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = AppleTypography.headline,
                    color = colors.label,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = AppleTypography.footnote,
                    color = colors.secondaryLabel,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(colors.systemFill)
                            .applePressScale(onClick = onDismiss)
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dismissText,
                            style = AppleTypography.subheadline.copy(fontWeight = FontWeight.Medium),
                            color = colors.label,
                            maxLines = 1
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (confirmDestructive) colors.red else colors.accent)
                            .applePressScale(onClick = onConfirm)
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmText,
                            style = AppleTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
