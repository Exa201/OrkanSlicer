package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.utils.Vec3d
import ru.ytkab0bp.slicebeam.view.GLView
import ru.ytkab0bp.slicebeam.view.MeasureModeEventBridge
import java.text.DecimalFormat
import kotlin.math.abs

internal data class MeasureReadout(
    val distance: Double,
    val deltaX: Double,
    val deltaY: Double,
    val deltaZ: Double,
)

internal object MeasureReadoutLogic {
    fun fromPoints(pointA: Vec3d?, pointB: Vec3d?): MeasureReadout? {
        if (pointA == null || pointB == null) return null
        return MeasureReadout(
            distance = pointA.distance(pointB),
            deltaX = abs(pointB.x - pointA.x),
            deltaY = abs(pointB.y - pointA.y),
            deltaZ = abs(pointB.z - pointA.z),
        )
    }
}

private data class MeasurePointPair(val pointA: Vec3d?, val pointB: Vec3d?)

/** Compose measurement controls; the GL renderer remains responsible for picking points. */
@Composable
internal fun MeasureModePanel(
    glView: GLView,
    onExit: () -> Unit,
) {
    var points by remember(glView) { mutableStateOf(MeasurePointPair(null, null)) }
    val currentOnExit by rememberUpdatedState(onExit)
    val numberFormat = remember { DecimalFormat("0.##") }

    DisposableEffect(glView) {
        val eventBridge = MeasureModeEventBridge { event ->
            // Renderer vectors are mutable; keep a stable snapshot for Compose state.
            points = MeasurePointPair(
                pointA = event.pointA?.let { Vec3d(it.x, it.y, it.z) },
                pointB = event.pointB?.let { Vec3d(it.x, it.y, it.z) },
            )
        }
        SliceBeam.EVENT_BUS.registerListener(eventBridge)
        onDispose { SliceBeam.EVENT_BUS.unregisterListener(eventBridge) }
    }

    val readout = MeasureReadoutLogic.fromPoints(points.pointA, points.pointB)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.MeasureTitle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            MeasureValueRow(
                label = stringResource(R.string.MeasureDistance),
                value = readout?.distance,
                numberFormat = numberFormat,
            )
            MeasureValueRow(
                label = stringResource(R.string.MeasureDeltaX),
                value = readout?.deltaX,
                numberFormat = numberFormat,
            )
            MeasureValueRow(
                label = stringResource(R.string.MeasureDeltaY),
                value = readout?.deltaY,
                numberFormat = numberFormat,
            )
            MeasureValueRow(
                label = stringResource(R.string.MeasureDeltaZ),
                value = readout?.deltaZ,
                numberFormat = numberFormat,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = {
                        points = MeasurePointPair(null, null)
                        glView.queueEvent {
                            glView.renderer.clearMeasurePoints()
                            glView.requestRender()
                        }
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.ActionClear), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = {
                        glView.queueEvent {
                            glView.renderer.isInMeasureMode = false
                            glView.renderer.clearMeasurePoints()
                            glView.requestRender()
                        }
                        currentOnExit()
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.ActionDone), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun MeasureValueRow(
    label: String,
    value: Double?,
    numberFormat: DecimalFormat,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Text(
            text = value?.let { stringResource(R.string.MeasureValueMillimeters, numberFormat.format(it)) } ?: "--",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
