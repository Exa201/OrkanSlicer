package ru.ytkab0bp.slicebeam.ui.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import ru.ytkab0bp.slicebeam.R

/** Decorative setup artwork behind the onboarding pager. */
@Composable
internal fun SetupActivityBackdrop(
    backgroundAlpha: Float,
    title: String,
    titleTranslationYPx: Float,
    titleTopInsetPx: Int,
    titleScale: Float,
    titleColor: Int,
    onTitleHeightChanged: (Int) -> Unit,
) {
    val density = LocalDensity.current
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.startup_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = backgroundAlpha.coerceIn(0f, 1f) },
            contentScale = ContentScale.Crop,
        )
        if (title.isNotEmpty()) {
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = with(density) { (titleTopInsetPx + titleTranslationYPx).toDp() })
                    .graphicsLayer(
                        scaleX = titleScale,
                        scaleY = titleScale,
                        transformOrigin = TransformOrigin(0.5f, 0f),
                    )
                    .onSizeChanged { onTitleHeightChanged(it.height) },
                color = Color(titleColor),
                fontSize = with(density) { 32.dp.toSp() },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}
