package ru.ytkab0bp.slicebeam.ui.settings

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.config.SlotFilamentProfiles
import ru.ytkab0bp.slicebeam.fragment.ProfileConfigValueController
import ru.ytkab0bp.slicebeam.ui.ColorFlow
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Shows the selected Orca filament's real material, swatch, and active temperature values. */
@Composable
fun FilamentProfileSummary(profile: ConfigObject?, modifier: Modifier = Modifier) {
    val settingsState by ProfileSettingsRepository.uiState.collectAsState()
    val profileTitle = profile?.title.orEmpty()
    val material = profileValue(profile, "filament_type")
        .firstValue().ifBlank { stringResource(R.string.FilamentSummaryUnknown) }
    val nozzle = profileValue(profile, "nozzle_temperature_initial_layer").firstValue()
        .ifBlank { profileValue(profile, "nozzle_temperature").firstValue() }
    val bed = profileValue(profile, "hot_plate_temp_initial_layer").firstValue()
        .ifBlank { profileValue(profile, "hot_plate_temp").firstValue() }
    val colorText = profileValue(profile, "filament_colour").firstValue()
    val selectedSlotColor = Prefs.getFilamentSlots()
        .getOrNull(SlotFilamentProfiles.selectedSlot)?.color ?: 0xFFB267FF.toInt()
    val swatch = profileColor(colorText, selectedSlotColor)
    val swatchDescription = stringResource(R.string.FilamentSlotColor)
    val description = stringResource(
        R.string.FilamentSummaryDescription,
        profileTitle,
        material,
        nozzle.ifBlank { "—" },
        bed.ifBlank { "—" },
    )

    key(settingsState.revision) {
        Surface(
            modifier = modifier.semantics { contentDescription = description },
            shape = RoundedCornerShape(16.dp),
            color = ColorFlow.elevated,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(30.dp)
                        .background(swatch, CircleShape)
                        .semantics { contentDescription = swatchDescription },
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = profileTitle.ifBlank { material },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.FilamentSummaryMaterial, material),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.FilamentSummaryTemperatures,
                            nozzle.ifBlank { "—" },
                            bed.ifBlank { "—" },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorFlow.filament,
                    )
                }
            }
        }
    }
}

private fun profileValue(profile: ConfigObject?, key: String): String {
    if (profile == null) return ""
    return ProfileConfigValueController.valueFor(
        ConfigObject.PROFILE_LIST_FILAMENT,
        profile,
        key,
        null,
        null,
        -1,
        false,
    ).orEmpty()
}

private fun String.firstValue(): String = trim().substringBefore(';').substringBefore(',').trim()

private fun profileColor(value: String, fallback: Int): Color {
    if (value.isBlank()) return Color(fallback)
    return runCatching {
        val normalized = value.trim().let { if (it.startsWith("#")) it else "#$it" }
        Color(AndroidColor.parseColor(normalized))
    }.getOrElse { Color(fallback) }
}
