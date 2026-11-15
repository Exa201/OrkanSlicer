package ru.ytkab0bp.slicebeam.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared ColorFlow palette. Values are opaque and selected accents meet AA on the dark surfaces. */
object ColorFlow {
    val background = Color(0xFF0B1020)
    val surface = Color(0xFF151A2F)
    val elevated = Color(0xFF202740)
    val outline = Color(0xFF414B66)
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFBCC7DA)
    val model = Color(0xFF1DBDFF)
    val print = Color(0xFFFF9C34)
    val filament = Color(0xFFB267FF)
    val printer = Color(0xFF39D978)
    val app = Color(0xFF8B7DFF)
    val warning = Color(0xFFFFBF47)
    val error = Color(0xFFFF6574)
    val success = Color(0xFF39D978)
    val disabled = Color(0xFF78839A)

    fun globalDestination(slot: Int): Color = when (slot) {
        0 -> model
        2 -> print
        3 -> filament
        4 -> printer
        else -> app
    }

    fun workspaceDestination(slot: Int): Color = when (slot) {
        0, 1 -> model
        2 -> app
        3 -> printer
        4 -> print
        else -> filament
    }
}

object OrkanDesignTokens {
    val spacing2 = 2.dp
    val spacing4 = 4.dp
    val spacing8 = 8.dp
    val spacing12 = 12.dp
    val spacing16 = 16.dp
    val spacing24 = 24.dp
    val cardRadius = 18.dp
    val controlRadius = 14.dp
    val touchTarget = 48.dp
    val iconSize = 22.dp
}

/** Multi-tone wrapper around the repository's offline VK vector glyphs. */
@Composable
fun OrkanIcon(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = OrkanDesignTokens.iconSize,
    accent: Color = MaterialTheme.colorScheme.primary,
    secondaryAccent: Color = ColorFlow.textSecondary,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val foreground = if (enabled) accent else ColorFlow.disabled
    Box(
        modifier = modifier.size(size * 1.55f)
            .then(if (selected) Modifier.background(
                Brush.linearGradient(listOf(foreground.copy(alpha = 0.25f), secondaryAccent.copy(alpha = 0.12f))),
                RoundedCornerShape(size * 0.62f),
            ) else Modifier)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(size),
            tint = foreground,
        )
        if (enabled) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = size * 0.04f, bottom = size * 0.04f)
                    .size(size * 0.25f).background(secondaryAccent, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
    }
}

@Composable
fun OrkanCard(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(OrkanDesignTokens.cardRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Box(Modifier.border(1.dp, accent.copy(alpha = 0.13f), RoundedCornerShape(OrkanDesignTokens.cardRadius))) {
            content()
        }
    }
}

@Composable
fun OrkanActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    @DrawableRes iconRes: Int? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = OrkanDesignTokens.touchTarget),
        shape = RoundedCornerShape(OrkanDesignTokens.controlRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = ColorFlow.background,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (iconRes != null) OrkanIcon(iconRes, null, size = 18.dp, accent = ColorFlow.background,
            secondaryAccent = ColorFlow.textPrimary, enabled = enabled)
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun OrkanSettingRow(
    title: String,
    value: String? = null,
    description: String? = null,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    @DrawableRes iconRes: Int? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = OrkanDesignTokens.touchTarget)
            .clip(RoundedCornerShape(OrkanDesignTokens.controlRadius))
            .then(if (onClick != null) Modifier.clickable(enabled, role = Role.Button) { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) OrkanIcon(iconRes, null, accent = accent, secondaryAccent = ColorFlow.textSecondary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold)
            description?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value != null) Text(value, style = MaterialTheme.typography.labelLarge, color = accent,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun OrkanAccordion(
    title: String,
    summary: String? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    @DrawableRes iconRes: Int? = null,
    content: @Composable () -> Unit,
) {
    OrkanCard(modifier, accent) {
        Column {
            Row(
                Modifier.fillMaxWidth().heightIn(min = OrkanDesignTokens.touchTarget)
                    .clip(RoundedCornerShape(OrkanDesignTokens.cardRadius))
                    .clickable(role = Role.Button, onClick = onToggle)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (iconRes != null) OrkanIcon(iconRes, null, accent = accent, secondaryAccent = ColorFlow.textSecondary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold)
                    summary?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(
                    painterResource(ru.ytkab0bp.slicebeam.R.drawable.dropdown_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).rotate(if (expanded) 180f else 0f),
                    tint = accent,
                )
            }
            if (expanded) content()
        }
    }
}

@Composable
fun OrkanProfilePicker(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
) {
    OrkanCard(modifier.clip(RoundedCornerShape(OrkanDesignTokens.cardRadius)).clickable(role = Role.Button, onClick = onClick), accent) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OrkanIcon(iconRes, null, accent = accent, secondaryAccent = ColorFlow.textSecondary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(painterResource(ru.ytkab0bp.slicebeam.R.drawable.dropdown_24),
                contentDescription = null, modifier = Modifier.size(20.dp), tint = accent)
        }
    }
}

@Composable
fun OrkanColorChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.heightIn(min = OrkanDesignTokens.touchTarget)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(OrkanDesignTokens.controlRadius),
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp,
            if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(18.dp).background(color, CircleShape).border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
