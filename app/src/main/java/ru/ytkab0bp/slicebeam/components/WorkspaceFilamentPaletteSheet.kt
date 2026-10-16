package ru.ytkab0bp.slicebeam.components

import android.app.Dialog
import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.OrkanUiTokens
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.view.FilamentPaletteController
import ru.ytkab0bp.slicebeam.view.FilamentPalettePolicy

/** Opens the shared filament palette as a phone sheet or a fixed side panel on wide windows. */
class WorkspaceFilamentPaletteSheet(
    private val context: Context,
    private val controller: FilamentPaletteController,
    private val hasModel: Boolean,
    private val onAssignModel: () -> Unit,
) {
    fun show() {
        when (FilamentPalettePolicy.workspacePresentation(context.resources.configuration.screenWidthDp)) {
            FilamentPalettePolicy.WorkspacePresentation.BOTTOM_SHEET -> showBottomSheet()
            FilamentPalettePolicy.WorkspacePresentation.SIDE_PANEL -> showSidePanel()
        }
    }

    private fun composeView(sidePanel: Boolean, dismiss: () -> Unit): ComposeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        setContent {
            BeamComposeTheme {
                controller.WorkspaceSheetContent(
                    hasModel = hasModel,
                    sidePanel = sidePanel,
                    onAssignModel = {
                        dismiss()
                        Handler(Looper.getMainLooper()).post { onAssignModel() }
                    },
                    onDismiss = dismiss,
                )
            }
        }
    }

    private fun showBottomSheet() {
        lateinit var dialog: BottomSheetDialog
        dialog = BottomSheetDialog(context)
        dialog.setContentView(
            composeView(sidePanel = false) { dialog.dismiss() },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        dialog.show()
        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun showSidePanel() {
        lateinit var dialog: Dialog
        dialog = Dialog(context)
        dialog.setContentView(
            composeView(sidePanel = true) { dialog.dismiss() },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.28f)
            val maxWidth = ViewUtils.dp(OrkanUiTokens.workspaceSidePanelWidth.value.toInt())
            val availableWidth = ViewUtils.dp(context.resources.configuration.screenWidthDp - 24)
            setLayout(minOf(maxWidth, availableWidth).coerceAtLeast(ViewUtils.dp(320)),
                WindowManager.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.END)
        }
    }
}
