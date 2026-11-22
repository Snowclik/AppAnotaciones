package com.appnotas.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appnotas.app.ui.motion.EmilEaseOut
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale
import com.appnotas.core.BreadcrumbItem

@Composable
fun BreadcrumbsBar(
    breadcrumbs: List<BreadcrumbItem>,
    onBreadcrumbClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (breadcrumbs.isEmpty()) return

    val listState = rememberLazyListState()
    val view = LocalView.current

    LaunchedEffect(breadcrumbs.size) {
        if (breadcrumbs.isNotEmpty()) {
            listState.animateScrollToItem(breadcrumbs.lastIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(
            items = breadcrumbs,
            key = { index, item -> item.id ?: "root_$index" }
        ) { index, item ->
            val isLast = index == breadcrumbs.lastIndex
            val interactionSource = remember { MutableInteractionSource() }
            val containerColor by animateColorAsState(
                targetValue = if (isLast) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = tween(durationMillis = 180, easing = EmilEaseOut),
                label = "breadcrumbBg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isLast) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(durationMillis = 180, easing = EmilEaseOut),
                label = "breadcrumbFg"
            )

            val pillShape = RoundedCornerShape(14.dp)
            Surface(
                shape = pillShape,
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier
                    .defaultMinSize(minHeight = 38.dp)
                    .pressableScale(interactionSource, pressedScale = 0.96f, enabled = !isLast)
                    .clip(pillShape)
                    .clickable(
                        enabled = !isLast,
                        interactionSource = interactionSource,
                        indication = LocalIndication.current
                    ) {
                        performTickHaptic(view)
                        onBreadcrumbClick(item.id)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (index == 0) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = "Inicio",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
