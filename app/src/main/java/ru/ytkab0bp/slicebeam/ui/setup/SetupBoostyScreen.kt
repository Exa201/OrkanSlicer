package ru.ytkab0bp.slicebeam.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.ThemesRepo

@Composable
internal fun SetupBoostyScreen(
    supporters: List<String>,
    boostyOnly: Boolean,
    onOpenSupport: () -> Unit,
    onContinue: () -> Unit,
) {
    val topColor = Color(ThemesRepo.getColor(R.attr.boostyColorTop))
    val bottomColor = Color(ThemesRepo.getColor(R.attr.boostyColorBottom))
    val onAccent = Color(ThemesRepo.getColor(R.attr.textColorOnAccent))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            .padding(top = 42.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.IntroBoostyTitle),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            color = onAccent,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.12f),
            contentColor = onAccent,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(supporters, key = { index, _ -> index }) { _, name ->
                    Text(
                        text = name,
                        color = onAccent,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Button(
            onClick = onOpenSupport,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = onAccent, contentColor = topColor),
        ) {
            Text(text = stringResource(R.string.IntroBoostySupport), fontSize = 16.sp, maxLines = 2)
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = onAccent, contentColor = topColor),
        ) {
            Text(
                text = stringResource(if (boostyOnly) android.R.string.ok else R.string.IntroNext),
                fontSize = 16.sp,
                maxLines = 1,
            )
        }
    }
}
