package com.appnotas.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppNotasShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Forma agrupada estilo Material 3 Expressive para filas contiguas dentro de una sección:
 * radios externos amplios (22.dp) y separación interna sutil (8.dp).
 */
fun segmentedGroupShape(index: Int, count: Int): RoundedCornerShape {
    val outer = 22.dp
    val inner = 8.dp
    return when {
        count <= 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(
            topStart = outer,
            topEnd = outer,
            bottomStart = inner,
            bottomEnd = inner
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = inner,
            topEnd = inner,
            bottomStart = outer,
            bottomEnd = outer
        )
        else -> RoundedCornerShape(inner)
    }
}
