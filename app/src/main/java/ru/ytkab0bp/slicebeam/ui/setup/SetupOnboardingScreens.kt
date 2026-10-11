package ru.ytkab0bp.slicebeam.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R

@Composable
internal fun SetupIntroScreen(onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.SetupOrcaCoreVersion),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            color = Color.White.copy(alpha = 0.88f),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        SetupPrimaryButton(labelRes = R.string.IntroStart, onClick = onNext)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
internal fun SetupPresetSourceScreen(
    onSelectBuiltIn: () -> Unit,
    onImportProfile: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 42.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = stringResource(R.string.SetupPresetSource),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = true, role = Role.RadioButton, onClick = onSelectBuiltIn)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.SetupBuiltInPresets),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                )
                RadioButton(selected = true, onClick = null)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(role = Role.Button, onClick = onImportProfile),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.SetupLoadProfile),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.SetupLoadProfileSubtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.SetupBrowse),
                    modifier = Modifier.padding(start = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        SetupPrimaryButton(labelRes = R.string.IntroNext, onClick = onNext)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
internal fun SetupAboutScreen(versionName: String, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding(), verticalArrangement = Arrangement.Bottom) {
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
        ) {
            Text(
                text = stringResource(R.string.SettingsAboutVersion, versionName),
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                textAlign = TextAlign.Start,
            )
        }
        SetupPrimaryButton(labelRes = android.R.string.ok, onClick = onClose)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
internal fun SetupFinishScreen(onFinish: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.IntroConfigured),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        SetupPrimaryButton(labelRes = R.string.IntroFinish, onClick = onFinish)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SetupPrimaryButton(labelRes: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(text = stringResource(labelRes), fontSize = 16.sp, maxLines = 1)
    }
}
