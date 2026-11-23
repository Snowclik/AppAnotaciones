package com.appnotas.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

data class FolderIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

data class FolderColorOption(
    val hex: String,
    val label: String,
    val color: Color
)

data class FolderEmojiOption(
    val emoji: String,
    val label: String
)

data class MeasureUnitOption(
    val code: String,
    val displayName: String,
    val menuLabel: String
)

const val EMOJI_ICON_PREFIX = "emoji:"

val folderIconOptions = listOf(
    FolderIconOption("folder", "Carpeta", Icons.Rounded.Folder),
    FolderIconOption("store", "Negocio", Icons.Rounded.Storefront),
    FolderIconOption("payments", "Caja", Icons.Rounded.Payments),
    FolderIconOption("inventory", "Stock", Icons.Rounded.Inventory2),
    FolderIconOption("analytics", "Métricas", Icons.Rounded.Analytics),
    FolderIconOption("restaurant", "Cocina", Icons.Rounded.Restaurant),
    FolderIconOption("shipping", "Envíos", Icons.Rounded.LocalShipping),
    FolderIconOption("work", "Trabajo", Icons.Rounded.Work),
    FolderIconOption("build", "Taller", Icons.Rounded.Build),
    FolderIconOption("home", "Local", Icons.Rounded.Home)
)

// 7 colores vibrantes para encajar limpio en 1 sola fila sin círculos huérfanos
val folderColorOptions = listOf(
    FolderColorOption("#0F766E", "Turquesa", Color(0xFF0F766E)),
    FolderColorOption("#10B981", "Esmeralda", Color(0xFF10B981)),
    FolderColorOption("#2563EB", "Cobalto", Color(0xFF2563EB)),
    FolderColorOption("#6366F1", "Índigo", Color(0xFF6366F1)),
    FolderColorOption("#E11D48", "Framboesa", Color(0xFFE11D48)),
    FolderColorOption("#EA580C", "Naranja", Color(0xFFEA580C)),
    FolderColorOption("#7C3AED", "Violeta", Color(0xFF7C3AED))
)

// 12 emojis versátiles curados para cualquier rubro en 1 sola fila horizontal
val folderEmojiOptions = listOf(
    FolderEmojiOption("🏪", "Tienda"),
    FolderEmojiOption("🍕", "Pizzería"),
    FolderEmojiOption("🍎", "Frutería"),
    FolderEmojiOption("💊", "Farmacia"),
    FolderEmojiOption("🌾", "Campo"),
    FolderEmojiOption("☕", "Café"),
    FolderEmojiOption("📦", "Inventario"),
    FolderEmojiOption("💵", "Caja"),
    FolderEmojiOption("🧾", "Cuentas"),
    FolderEmojiOption("🛠️", "Taller"),
    FolderEmojiOption("🚚", "Envíos"),
    FolderEmojiOption("⭐", "Destacado")
)

val measureUnitOptions = listOf(
    MeasureUnitOption("u", "unidades", "unidades (u)"),
    MeasureUnitOption("paq", "paquetes", "paquetes (paq)"),
    MeasureUnitOption("caja", "cajas", "cajas"),
    MeasureUnitOption("doc", "docenas", "docenas (doc)"),
    MeasureUnitOption("kg", "kilos", "kilos (kg)"),
    MeasureUnitOption("g", "gramos", "gramos (g)"),
    MeasureUnitOption("mg", "miligramos", "miligramos (mg)"),
    MeasureUnitOption("lb", "libras", "libras (lb)"),
    MeasureUnitOption("lt", "litros", "litros (lt)"),
    MeasureUnitOption("ml", "mililitros", "mililitros (ml)"),
    MeasureUnitOption("hrs", "horas", "horas (hrs)")
)

fun measureDisplayName(code: String?): String {
    val clean = code?.trim().orEmpty()
    if (clean.isEmpty()) return "unidades"
    return measureUnitOptions.firstOrNull { it.code.equals(clean, ignoreCase = true) }?.displayName
        ?: clean
}

fun isEmojiIcon(iconValue: String?): Boolean {
    if (iconValue.isNullOrBlank()) return false
    if (iconValue.startsWith(EMOJI_ICON_PREFIX)) return true
    return folderIconOptions.none { it.key == iconValue }
}

fun extractEmoji(iconValue: String?): String? {
    if (iconValue.isNullOrBlank()) return null
    return when {
        iconValue.startsWith(EMOJI_ICON_PREFIX) -> iconValue.removePrefix(EMOJI_ICON_PREFIX).trim().takeIf { it.isNotEmpty() }
        folderIconOptions.none { it.key == iconValue } -> iconValue.trim().takeIf { it.isNotEmpty() }
        else -> null
    }
}

fun encodeEmojiIcon(emoji: String): String = "$EMOJI_ICON_PREFIX${emoji.trim()}"

fun resolveFolderIcon(iconKey: String?): ImageVector =
    folderIconOptions.firstOrNull { it.key == iconKey }?.icon ?: Icons.Rounded.Folder

fun resolveFolderColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(fallback)
}

@Composable
fun FolderAvatarBadge(
    iconValue: String?,
    colorHex: String?,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = 16.dp
) {
    val accent = resolveFolderColor(colorHex, fallbackColor)
    val emoji = extractEmoji(iconValue)
    val shape = RoundedCornerShape(cornerRadius)
    val badgeBrush = Brush.linearGradient(
        colors = listOf(
            accent.copy(alpha = 0.24f),
            accent.copy(alpha = 0.11f)
        )
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(badgeBrush)
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.28f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (emoji != null) {
            Text(
                text = emoji,
                fontSize = (size.value * 0.48f).sp
            )
        } else {
            Icon(
                imageVector = resolveFolderIcon(iconValue),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(size * 0.50f)
            )
        }
    }
}

fun formatNumberDisplay(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    return formatter.format(value)
}

fun formatEditableDecimal(value: Double): String {
    val rounded = kotlin.math.round(value)
    return if (kotlin.math.abs(value - rounded) < 1e-9) {
        rounded.toLong().toString()
    } else {
        value.toString()
    }
}

fun formatCalculationBreakdown(
    unit: String?,
    quantity: Double?,
    unitMultiplier: Double?,
    multiplierLabel: String?
): String? {
    if (quantity == null) return null
    val q = formatNumberDisplay(quantity)
    val cleanLabel = multiplierLabel?.trim().orEmpty()

    return if (unitMultiplier != null) {
        val m = formatNumberDisplay(unitMultiplier)
        when {
            unit == "$" && cleanLabel.isNotEmpty() && cleanLabel != "$/u" -> "$q $cleanLabel × $$m c/u"
            unit == "$" -> "$q × $$m c/u"
            cleanLabel.isNotEmpty() -> "$q × $m $cleanLabel"
            else -> "$q × $m"
        }
    } else {
        if (cleanLabel.isNotEmpty()) "por $q $cleanLabel" else "por $q"
    }
}

fun formatValueWithUnit(value: Double, unit: String?): String {
    val formatted = formatNumberDisplay(value)
    return when {
        unit.isNullOrBlank() -> formatted
        unit == "$" -> "$$formatted"
        else -> "$formatted $unit"
    }
}
