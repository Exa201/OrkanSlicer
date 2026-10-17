package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.view.GLView

@Composable
internal fun VariableLayerHeightPanel(glView: GLView, onExit: () -> Unit) {
    var quality by remember { mutableFloatStateOf(0.5f) }
    var status by remember { mutableIntStateOf(R.string.NoObjectSelected) }

    fun refreshStatus() {
        glView.queueEvent {
            val renderer = glView.renderer
            val objectIndex = renderer.selectedObject
            val currentStatus = when {
                objectIndex < 0 -> R.string.NoObjectSelected
                renderer.model?.hasAdaptiveLayerHeight(objectIndex) == true -> R.string.VariableLayerAdaptive
                else -> R.string.VariableLayerDefault
            }
            ViewUtils.postOnMainThread { status = currentStatus }
        }
    }

    LaunchedEffect(glView) { refreshStatus() }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.VariableLayerTitle), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.VariableLayerStatus))
        Text(stringResource(status), color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.VariableLayerQuality, quality))
        Slider(value = quality, onValueChange = { quality = it }, valueRange = 0f..1f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    glView.queueEvent {
                        val index = glView.renderer.selectedObject
                        val model = glView.renderer.model
                        val result = if (index >= 0 && model != null) {
                            runCatching { model.clearAdaptiveLayerHeight(index) }
                        } else null
                        ViewUtils.postOnMainThread {
                            if (result == null) SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.NoObjectSelected))
                            else {
                                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.VariableLayerCleared))
                                refreshStatus()
                                glView.requestRender()
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.ActionClear)) }
            Button(
                onClick = {
                    val configPath = runCatching {
                        SliceBeam.genCurrentConfig()
                        SliceBeam.getCurrentConfigFile().absolutePath
                    }.getOrElse { error ->
                        SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.VariableLayerFailed, error.message))
                        return@Button
                    }
                    glView.queueEvent {
                        val index = glView.renderer.selectedObject
                        val model = glView.renderer.model
                        val result = if (index >= 0 && model != null) runCatching {
                            model.applyAdaptiveLayerHeight(index, configPath, quality)
                        } else null
                        ViewUtils.postOnMainThread {
                            if (result == null) SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.NoObjectSelected))
                            else if (result.isFailure) SliceBeam.EVENT_BUS.fireEvent(
                                NeedSnackbarEvent(R.string.VariableLayerFailed, result.exceptionOrNull()?.message),
                            ) else {
                                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.VariableLayerApplied))
                                refreshStatus()
                                glView.requestRender()
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1.2f).heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.VariableLayerAdaptive)) }
            OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.ActionDone))
            }
        }
    }
}
