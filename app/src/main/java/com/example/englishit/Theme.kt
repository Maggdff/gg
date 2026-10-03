package com.example.englishit

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ---------------------------------------------------------------------------
// Brand palette
// ---------------------------------------------------------------------------
@Immutable
class Brand(
    val primary: Color, val primaryDark: Color,
    val gradA: Color, val gradB: Color,
    val success: Color, val successDark: Color, val successBg: Color, val successText: Color,
    val danger: Color, val dangerDark: Color, val dangerBg: Color, val dangerText: Color,
    val gold: Color, val goldDark: Color,
    val orange: Color, val infoBg: Color,
    val card: Color, val cardBorder: Color, val muted: Color
)

val LightBrand = Brand(
    primary = Color(0xFF5B5BF0), primaryDark = Color(0xFF4343C9),
    gradA = Color(0xFF6A5AE0), gradB = Color(0xFF18B4D8),
    success = Color(0xFF16B364), successDark = Color(0xFF0E8F4E),
    successBg = Color(0xFFDDF8E8), successText = Color(0xFF0B7A3E),
    danger = Color(0xFFF04461), dangerDark = Color(0xFFC22B46),
    dangerBg = Color(0xFFFFE1E7), dangerText = Color(0xFFB3213C),
    gold = Color(0xFFFFB020), goldDark = Color(0xFFD98E00),
    orange = Color(0xFFFF7A29), infoBg = Color(0xFFE0ECFF),
    card = Color(0xFFFFFFFF), cardBorder = Color(0xFFE3E7F5), muted = Color(0xFF6B7194)
)

val DarkBrand = Brand(
    primary = Color(0xFF8B8BFF), primaryDark = Color(0xFF5F5FD6),
    gradA = Color(0xFF5848C8), gradB = Color(0xFF1597AD),
    success = Color(0xFF2FD07A), successDark = Color(0xFF1C9B58),
    successBg = Color(0xFF12372A), successText = Color(0xFF5BE49A),
    danger = Color(0xFFFF6B85), dangerDark = Color(0xFFC94560),
    dangerBg = Color(0xFF3D1A25), dangerText = Color(0xFFFF8FA3),
    gold = Color(0xFFFFC53D), goldDark = Color(0xFFC99200),
    orange = Color(0xFFFF9150), infoBg = Color(0xFF1B2A4D),
    card = Color(0xFF1A1E3A), cardBorder = Color(0xFF2B3158), muted = Color(0xFF9AA0C8)
)

val LocalBrand = staticCompositionLocalOf { LightBrand }

val brand: Brand
    @Composable @ReadOnlyComposable get() = LocalBrand.current

fun Color.darken(f: Float = 0.78f) = Color(red * f, green * f, blue * f, alpha)
fun Color.lighten(f: Float = 0.25f) = lerp(this, Color.White, f)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val b = if (dark) DarkBrand else LightBrand
    val scheme = if (dark) {
        darkColorScheme(
            primary = b.primary, onPrimary = Color(0xFF10123A),
            primaryContainer = Color(0xFF2A2D66), onPrimaryContainer = Color(0xFFE0E0FF),
            secondary = b.gradB, background = Color(0xFF0E1128), onBackground = Color(0xFFEDEFFF),
            surface = b.card, onSurface = Color(0xFFEDEFFF),
            surfaceVariant = Color(0xFF242952), onSurfaceVariant = b.muted,
            outline = b.cardBorder, outlineVariant = b.cardBorder,
            error = b.danger, surfaceTint = Color.Transparent
        )
    } else {
        lightColorScheme(
            primary = b.primary, onPrimary = Color.White,
            primaryContainer = Color(0xFFE4E4FF), onPrimaryContainer = Color(0xFF1B1B6B),
            secondary = b.gradB, background = Color(0xFFF3F5FF), onBackground = Color(0xFF1A1D3B),
            surface = b.card, onSurface = Color(0xFF1A1D3B),
            surfaceVariant = Color(0xFFEBEEFB), onSurfaceVariant = b.muted,
            outline = b.cardBorder, outlineVariant = b.cardBorder,
            error = b.danger, surfaceTint = Color.Transparent
        )
    }
    CompositionLocalProvider(LocalBrand provides b) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(24.dp)
            ),
            content = content
        )
    }
}

// ---------------------------------------------------------------------------
// Reusable components
// ---------------------------------------------------------------------------
@Composable
fun LogoMark(size: Dp, modifier: Modifier = Modifier, inverted: Boolean = false) {
    val b = brand
    val bg = if (inverted) SolidColor(Color.White) else Brush.linearGradient(listOf(b.gradA, b.gradB))
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "E",
            color = if (inverted) b.primaryDark else Color.White,
            fontSize = (size.value * 0.56f).sp,
            fontWeight = FontWeight.Black
        )
    }
}

/** 3D "chunky" button: a coloured face sitting on a darker edge that squashes when pressed. */
@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    face: Color = brand.primary,
    edge: Color = brand.primaryDark,
    textColor: Color = Color.White,
    height: Dp = 52.dp,
    fontSize: TextUnit = 16.sp
) {
    val b = brand
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val drop by animateDpAsState(if (pressed && enabled) 4.dp else 0.dp, label = "drop")
    val shape = RoundedCornerShape(16.dp)
    val faceC = if (enabled) face else b.cardBorder
    val edgeC = if (enabled) edge else b.cardBorder.darken(0.9f)
    val txtC = if (enabled) textColor else b.muted
    Box(
        modifier
            .height(height + 4.dp)
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Box(
            Modifier.fillMaxWidth().height(height).align(Alignment.BottomCenter)
                .clip(shape).background(edgeC)
        )
        Box(
            Modifier.fillMaxWidth().height(height).offset(y = drop).clip(shape).background(faceC),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                color = txtC,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val b = brand
    ChunkyButton(
        text, onClick, modifier, enabled,
        face = b.card, edge = b.cardBorder, textColor = b.primary
    )
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val b = brand
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = b.card,
        border = BorderStroke(1.dp, b.cardBorder),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier, size: TextUnit = 13.sp) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = fg, fontSize = size, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun AppProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    track: Color = brand.cardBorder,
    brush: Brush = Brush.horizontalGradient(listOf(brand.success, brand.gradB))
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(modifier.height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(p)
                .clip(RoundedCornerShape(50)).background(brush)
        )
    }
}

@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val b = brand
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = b.card,
            border = BorderStroke(1.dp, b.cardBorder)
        ) {
            Column(
                Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}
