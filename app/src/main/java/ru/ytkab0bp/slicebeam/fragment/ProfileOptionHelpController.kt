package ru.ytkab0bp.slicebeam.fragment

import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef
import ru.ytkab0bp.slicebeam.slic3r.Slic3rLocalization
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.util.HashSet
import java.util.function.BooleanSupplier

/** Help dialog and favorite action for a single Orca option. */
object ProfileOptionHelpController {
    internal fun show(
        definition: ConfigOptionDef,
        isFavorite: Boolean,
        favoriteKeys: MutableSet<String>,
        favoritesPreferenceKey: String,
        favoritesOnly: BooleanSupplier,
        onFavoritesChanged: Runnable,
        onShowDialog: (ProfileOptionHelpDialogState) -> Unit,
    ): Boolean {
        onShowDialog(
            ProfileOptionHelpDialogState(
                title = Slic3rLocalization.getString(definition.getFullLabel()),
                message = Slic3rLocalization.getString(definition.tooltip),
                favoriteAction = if (isFavorite) R.string.SettingsRemoveFavorite else R.string.SettingsAddFavorite,
                onFavorite = {
                    if (isFavorite) favoriteKeys.remove(definition.key) else favoriteKeys.add(definition.key)
                    Prefs.getPrefs()?.edit()?.putStringSet(favoritesPreferenceKey, HashSet(favoriteKeys))
                        ?.apply()
                    if (favoritesOnly.getAsBoolean()) onFavoritesChanged.run()
                },
            ),
        )
        return true
    }
}
