package ru.ytkab0bp.slicebeam.fragment

import android.content.Context
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.components.BeamAlertDialogBuilder
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef
import ru.ytkab0bp.slicebeam.slic3r.Slic3rLocalization
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.util.HashSet
import java.util.function.BooleanSupplier

/** Help dialog and favorite action for a single Orca option. */
object ProfileOptionHelpController {
    @JvmStatic
    fun show(
        context: Context,
        definition: ConfigOptionDef,
        isFavorite: Boolean,
        favoriteKeys: MutableSet<String>,
        favoritesPreferenceKey: String,
        favoritesOnly: BooleanSupplier,
        onFavoritesChanged: Runnable,
    ): Boolean {
        BeamAlertDialogBuilder(context)
            .setTitle(Slic3rLocalization.getString(definition.getFullLabel()))
            .setMessage(Slic3rLocalization.getString(definition.tooltip))
            .setNeutralButton(
                if (isFavorite) R.string.SettingsRemoveFavorite else R.string.SettingsAddFavorite,
            ) { _, _ ->
                if (isFavorite) favoriteKeys.remove(definition.key) else favoriteKeys.add(definition.key)
                Prefs.getPrefs()?.edit()?.putStringSet(favoritesPreferenceKey, HashSet(favoriteKeys))
                    ?.apply()
                if (favoritesOnly.getAsBoolean()) onFavoritesChanged.run()
            }
            .setPositiveButton(android.R.string.ok, null)
            .show()
        return true
    }
}
