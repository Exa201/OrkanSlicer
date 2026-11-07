package ru.ytkab0bp.slicebeam

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.fragment.ProfileOptionHelpDialog
import ru.ytkab0bp.slicebeam.fragment.ProfileOptionHelpDialogState
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Exercises the real Compose help dialog and favorite callbacks on an Android device. */
@RunWith(AndroidJUnit4::class)
class ProfileOptionHelpDialogDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun favoriteActionsPersistAndTheHelpDialogDismisses() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.init(context.applicationContext as Application)
        val prefs = requireNotNull(Prefs.getPrefs())
        val preferenceKey = "profile_setting_favorites"
        val favoritesBefore = prefs.getStringSet(preferenceKey, emptySet()).orEmpty().toSet()
        val optionKey = "ui2_device_test_option_help"
        val addedFavorites = favoritesBefore + optionKey
        val removedFavorites = favoritesBefore - optionKey
        var dialog by mutableStateOf<ProfileOptionHelpDialogState?>(null)
        val title = "Profile option help test"
        val message = "This is explanatory profile option text."

        fun showFavoriteDialog(favorite: Boolean) {
            dialog = ProfileOptionHelpDialogState(
                title = title,
                message = message,
                favoriteAction = if (favorite) R.string.SettingsRemoveFavorite else R.string.SettingsAddFavorite,
                onFavorite = {
                    prefs.edit().putStringSet(
                        preferenceKey,
                        if (favorite) removedFavorites else addedFavorites,
                    ).commit()
                },
            )
        }

        try {
            showFavoriteDialog(favorite = false)
            compose.setContent {
                dialog?.let { current ->
                    ProfileOptionHelpDialog(current) { dialog = null }
                }
            }

            compose.onNodeWithText(title).assertIsDisplayed()
            compose.onNodeWithText(message).assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.SettingsAddFavorite))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { dialog == null }
            assertEquals(addedFavorites, prefs.getStringSet(preferenceKey, emptySet()).orEmpty())
            compose.onAllNodesWithText(title).assertCountEquals(0)

            compose.runOnIdle { showFavoriteDialog(favorite = true) }
            compose.onNodeWithText(context.getString(R.string.SettingsRemoveFavorite))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { dialog == null }
            assertEquals(removedFavorites, prefs.getStringSet(preferenceKey, emptySet()).orEmpty())
            compose.onAllNodesWithText(title).assertCountEquals(0)
        } finally {
            prefs.edit().putStringSet(preferenceKey, favoritesBefore).commit()
        }
    }
}
