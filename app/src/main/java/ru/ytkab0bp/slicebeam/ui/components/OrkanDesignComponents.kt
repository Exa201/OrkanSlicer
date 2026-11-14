package ru.ytkab0bp.slicebeam.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.ui.LocalOrkanSection
import ru.ytkab0bp.slicebeam.ui.OrkanSection

/** Colorful, decorative vector badge; the surrounding label provides the accessible name. */
@Composable
fun OrkanIconBadge(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
) {
    val section = LocalOrkanSection.current
    val start = if (section == OrkanSection.DEFAULT) MaterialTheme.colorScheme.primary else section.accent
    val end = if (section == OrkanSection.DEFAULT) MaterialTheme.colorScheme.secondary else section.secondary
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(listOf(start, end)))
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(size * 0.52f),
            // Dark, section-specific line art keeps at least 4.5:1 contrast against
            // the saturated accent badge on both light and dark app themes.
            tint = if (section == OrkanSection.DEFAULT) MaterialTheme.colorScheme.onPrimary
                else section.iconAccent,
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(if (section == OrkanSection.DEFAULT) end else section.accent.copy(alpha = 0.9f)),
        )
    }
}

/** Reusable, readable page heading used by profile and settings destinations. */
@Composable
fun OrkanPageHeader(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
) {
    val accent = LocalOrkanSection.current.let {
        if (it == OrkanSection.DEFAULT) MaterialTheme.colorScheme.primary else it.accent
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 76.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrkanIconBadge(iconRes, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

/** Elevated section card with a colorful vector heading, suitable for settings groups. */
@Composable
fun OrkanSectionCard(
    @DrawableRes iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val accent = LocalOrkanSection.current.let {
        if (it == OrkanSection.DEFAULT) MaterialTheme.colorScheme.primary else it.accent
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 60.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OrkanIconBadge(iconRes, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            content()
        }
    }
}
