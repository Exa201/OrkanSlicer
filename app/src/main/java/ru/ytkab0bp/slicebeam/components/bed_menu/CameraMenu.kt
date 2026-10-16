package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.render.Camera
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.Vec3d
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.view.GLView
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/** Camera controls rendered with Compose while retaining the existing renderer and camera actions. */
open class CameraMenu : BedMenu(), WorkspaceComposeBedMenu {
    private var fragment: BedFragment? = null

    override fun onSetBed(fragment: BedFragment?) {
        this.fragment = fragment
    }

    override fun onCreateView(ctx: Context, portrait: Boolean): ThemeAwareComposeView {
        val themedView = ThemeAwareComposeView(ctx)
        themedView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        themedView.setContent {
            BeamComposeTheme(themeRevision = themedView.themeRevision) {
                WorkspaceContent(portrait)
            }
        }
        return themedView
    }

    @Composable
    override fun WorkspaceContent(portrait: Boolean) {
        val context = LocalContext.current
        CameraMenuContent(
            portrait = portrait,
            initialOrthoEnabled = Prefs.isOrthoProjectionEnabled(),
            onPresetSelected = ::selectCamera,
            onControlModeSelected = { showControlModeDialog(context) },
            onOrthoChanged = ::setOrthoProjection,
        )
    }

    private fun showControlModeDialog(context: Context) {
        val bed = fragment ?: return
        val labels = listOf(
            context.getString(R.string.MenuCameraControlModeOne),
            context.getString(R.string.MenuCameraControlModeTwo),
            context.getString(R.string.MenuCameraControlModeThree),
        ).mapIndexed { index, label ->
            if (index == Prefs.getCameraControlMode()) "✓ $label" else label
        }
        bed.showWorkspaceComposeDialog { dismiss ->
            ModelChoiceDialog(
                title = context.getString(R.string.MenuCameraControlModeFull),
                cancelLabel = context.getString(android.R.string.cancel),
                choices = labels,
                onDismiss = dismiss,
                onSelect = { which ->
                    dismiss()
                    Prefs.setCameraControlMode(which)
                },
            )
        }
    }

    private fun setOrthoProjection(enabled: Boolean) {
        Prefs.setOrthoProjectionEnabled(enabled)
        val glView = fragment?.glView ?: return
        glView.renderer.updateProjection()
        glView.requestRender()
    }

    private fun selectCamera(preset: CameraPreset) {
        val bedFragment = fragment ?: return
        val bed = bedFragment.glView?.renderer?.bed
        if (bed == null || !bed.isValid()) {
            Toast.makeText(bedFragment.getContext(), R.string.BedConfigurationError, Toast.LENGTH_SHORT).show()
            return
        }

        val min = bed.getVolumeMin() ?: return
        val max = bed.getVolumeMax() ?: return
        val target = CameraPresetGeometry.resolve(
            preset,
            CameraVector(min.x, min.y, min.z),
            CameraVector(max.x, max.y, max.z),
        )
        animateTo(
            Vec3d(target.origin.x, target.origin.y, target.origin.z),
            Vec3d(target.position.x, target.position.y, target.position.z),
        )
    }

    private fun animateTo(toOrigin: Vec3d, toPosition: Vec3d) {
        val glView: GLView = fragment?.glView ?: return
        val camera: Camera = glView.renderer.camera
        val fromOrigin = Vec3d(camera.origin)
        val fromPosition = Vec3d(camera.position)
        val middlePoint = fromPosition.center(toPosition)
        val zoom = camera.zoom

        SpringAnimation(FloatValueHolder(0f))
            .setMinimumVisibleChange(1 / 1000f)
            .setSpring(SpringForce(1f).setStiffness(1000f).setDampingRatio(1f))
            .addUpdateListener { _, value, _ ->
                camera.zoom = ViewUtils.lerp(zoom, 1f, value)
                camera.position.set(
                    ViewUtils.lerpd(
                        fromPosition.x,
                        if (kotlin.math.abs(toPosition.x - toOrigin.x) <= 5) middlePoint.x
                        else fromPosition.x + (toPosition.x - fromPosition.x) / 2,
                        toPosition.x,
                        value,
                    ),
                    ViewUtils.lerpd(
                        fromPosition.y,
                        if (kotlin.math.abs(toPosition.y - toOrigin.y) <= 5) middlePoint.y
                        else fromPosition.y + (toPosition.y - fromPosition.y) / 2,
                        toPosition.y,
                        value,
                    ),
                    ViewUtils.lerpd(
                        fromPosition.z,
                        if (kotlin.math.abs(toPosition.z - toOrigin.z) <= 5) middlePoint.z
                        else fromPosition.z + (toPosition.z - fromPosition.z) / 2,
                        toPosition.z,
                        value,
                    ),
                )
                camera.origin.set(
                    ViewUtils.lerpd(fromOrigin.x, toOrigin.x, value),
                    ViewUtils.lerpd(fromOrigin.y, toOrigin.y, value),
                    ViewUtils.lerpd(fromOrigin.z, toOrigin.z, value),
                )
                glView.renderer.updateProjection()
                glView.requestRender()
            }
            .start()
    }
}

@Composable
private fun CameraMenuContent(
    portrait: Boolean,
    initialOrthoEnabled: Boolean,
    onPresetSelected: (CameraPreset) -> Unit,
    onControlModeSelected: () -> Unit,
    onOrthoChanged: (Boolean) -> Unit,
) {
    var orthoEnabled by remember { mutableStateOf(initialOrthoEnabled) }
    val actions = listOf(
        BedComposeMenuAction(R.string.MenuCameraIsometric, R.drawable.camera_mode_0_28, { onPresetSelected(CameraPreset.ISOMETRIC) }),
        BedComposeMenuAction(
            R.string.MenuCameraOrtho,
            R.drawable.image_format_32,
            {
                orthoEnabled = !orthoEnabled
                onOrthoChanged(orthoEnabled)
            },
            selected = orthoEnabled,
            toggleable = true,
        ),
        BedComposeMenuAction(R.string.MenuCameraTop, R.drawable.camera_mode_1_28, { onPresetSelected(CameraPreset.TOP) }),
        BedComposeMenuAction(R.string.MenuCameraBottom, R.drawable.camera_mode_2_28, { onPresetSelected(CameraPreset.BOTTOM) }),
        BedComposeMenuAction(R.string.MenuCameraFront, R.drawable.camera_mode_3_28, { onPresetSelected(CameraPreset.FRONT) }),
        BedComposeMenuAction(R.string.MenuCameraBack, R.drawable.camera_mode_4_28, { onPresetSelected(CameraPreset.BACK) }),
        BedComposeMenuAction(R.string.MenuCameraLeft, R.drawable.camera_mode_5_28, { onPresetSelected(CameraPreset.LEFT) }),
        BedComposeMenuAction(R.string.MenuCameraRight, R.drawable.camera_mode_6_28, { onPresetSelected(CameraPreset.RIGHT) }),
        BedComposeMenuAction(
            R.string.MenuCameraControlMode,
            R.drawable.rectangle_hand_point_up_28,
            { onControlModeSelected() },
            sectionBreakBefore = true,
        ),
    )
    BedMenuActionsContent(actions, portrait, multiRow = true)
}
