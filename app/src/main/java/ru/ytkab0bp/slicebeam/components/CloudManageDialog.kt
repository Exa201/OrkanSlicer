package ru.ytkab0bp.slicebeam.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.cloud.CloudController
import ru.ytkab0bp.slicebeam.events.NeedDismissSnackbarEvent
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Cloud account actions and subscription controls, hosted entirely by Compose. */
class CloudManageDialog(context: Context) {
    private val sheetContext = context
    private var screenState by mutableStateOf(
        CloudManageScreenState.resolve(
            CloudController.getUserInfo(),
            CloudController.getUserFeatures(),
            Prefs.isCloudProfileSyncEnabled(),
        ),
    )

    @Composable
    fun Content(onDismiss: () -> Unit) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                BeamComposeTheme {
                    CloudManageContent(
                        state = screenState,
                        onSyncChanged = ::setCloudSyncEnabled,
                        onManageSubscription = ::openSubscription,
                        onLogout = {
                            CloudController.logout()
                            onDismiss()
                        },
                    )
                }
            }
        }
    }

    private fun setCloudSyncEnabled(isChecked: Boolean) {
        Prefs.setCloudProfileSyncEnabled(isChecked)
        screenState = screenState.copy(syncEnabled = isChecked)
        if (isChecked) {
            CloudController.notifyDataChanged()
        } else {
            SliceBeam.EVENT_BUS.fireEvent(NeedDismissSnackbarEvent(CloudController.CLOUD_SYNC_TAG))
        }
    }

    private fun openSubscription() {
        val url = screenState.subscriptionUrl?.trim().orEmpty()
        val uri = runCatching { Uri.parse(url) }.getOrNull()
        if (uri == null || uri.scheme !in setOf("https", "http") || uri.host.isNullOrBlank()) {
            showOpenLinkError()
            return
        }
        try {
            sheetContext.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            showOpenLinkError()
        } catch (_: SecurityException) {
            showOpenLinkError()
        }
    }

    private fun showOpenLinkError() {
        Toast.makeText(sheetContext, R.string.CloudManageLinkOpenFailed, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun CloudManageContent(
    state: CloudManageScreenState,
    onSyncChanged: (Boolean) -> Unit,
    onManageSubscription: () -> Unit,
    onLogout: () -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val syncLabel = stringResource(R.string.SettingsCloudManageFeatureCloudSync)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = screenHeight * 0.9f)
            .verticalScroll(rememberScrollState())
            .background(
                Color(ThemesRepo.getColor(R.attr.dialogBackground)),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            )
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.SettingsCloudManageButtonManage),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )

        if (state.accountName != null) {
            Text(
                text = stringResource(R.string.SettingsCloudManageLoggedInAs, state.accountName),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (state.isLoading) {
            Text(
                text = stringResource(R.string.SettingsCloudLoading),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
        }

        if (state.canSyncProfiles) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = syncLabel,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                    )
                    Switch(
                        checked = state.syncEnabled,
                        onCheckedChange = onSyncChanged,
                        modifier = Modifier.semantics { contentDescription = syncLabel },
                    )
                }
            }
        }

        if (state.hasSubscription) {
            TextButton(
                onClick = onManageSubscription,
                enabled = !state.subscriptionUrl.isNullOrBlank(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.SettingsCloudManageSubscription),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.external_link_outline_24),
                    contentDescription = null,
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(ThemesRepo.getColor(R.attr.textColorNegative)),
                contentColor = Color(ThemesRepo.getColor(R.attr.textColorOnAccent)),
            ),
        ) {
            Text(stringResource(R.string.SettingsCloudManageButtonLogOut), fontSize = 16.sp)
        }
    }
}
