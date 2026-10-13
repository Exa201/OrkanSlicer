package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R

@Composable
internal fun PrinterWebOpenButton(
    hostConfigured: Boolean,
    onOpen: () -> Unit,
) {
    val description = stringResource(
        if (hostConfigured) R.string.PrinterOpenWebDescription
        else R.string.PrinterWebConfigureDescription,
    )
    Button(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(painterResource(R.drawable.globe_cross_outline_28), contentDescription = null,
            modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun PrinterWebCloseButton(onClose: () -> Unit) {
    val description = stringResource(R.string.ClosePanel)
    Surface(
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClose),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.35f),
        contentColor = Color.White,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = "×", fontSize = 28.sp, color = Color.White)
        }
    }
}
