package com.appnotas.app.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

val folderIconOptions = listOf(
    FolderIconOption("folder", "Carpeta", Icons.Rounded.Folder),
    FolderIconOption("store", "Negocio", Icons.Rounded.Storefront),
    FolderIconOption("payments", "Caja", Icons.Rounded.Payments),
    FolderIconOption("inventory", "Inventario", Icons.Rounded.Inventory2),
    FolderIconOption("analytics", "Métricas", Icons.Rounded.Analytics),
    FolderIconOption("restaurant", "Alimentos", Icons.Rounded.Restaurant),
    FolderIconOption("shipping", "Envíos", Icons.Rounded.LocalShipping),
    FolderIconOption("work", "Trabajo", Icons.Rounded.Work),
    FolderIconOption("build", "Taller", Icons.Rounded.Build),
    FolderIconOption("home", "Hogar", Icons.Rounded.Home)
)

val folderColorOptions = listOf(
    FolderColorOption("#2E7D32", "Verde", Color(0xFF2E7D32)),
    FolderColorOption("#1565C0", "Azul", Color(0xFF1565C0)),
    FolderColorOption("#EF6C00", "Naranja", Color(0xFFEF6C00)),
    FolderColorOption("#6A1B9A", "Morado", Color(0xFF6A1B9A)),
    FolderColorOption("#C62828", "Rojo", Color(0xFFC62828)),
    FolderColorOption("#00838F", "Turquesa", Color(0xFF00838F)),
    FolderColorOption("#4E342E", "Café", Color(0xFF4E342E))
)

val quickUnitPresets = listOf("$", "kg", "pz", "hrs", "lt", "%")

fun resolveFolderIcon(iconKey: String?): ImageVector =
    folderIconOptions.firstOrNull { it.key == iconKey }?.icon ?: Icons.Rounded.Folder

fun resolveFolderColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(fallback)
}

fun formatNumberDisplay(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    return formatter.format(value)
}

fun formatValueWithUnit(value: Double, unit: String?): String {
    val formatted = formatNumberDisplay(value)
    return when {
        unit.isNullOrBlank() -> formatted
        unit == "$" -> "$$formatted"
        else -> "$formatted $unit"
    }
}
