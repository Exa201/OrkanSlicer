package ru.ytkab0bp.slicebeam.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.launch
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.ThemesRepo

internal object ChangeLogContentResolver {
    fun localizedText(language: String, translations: Map<String, String>): String =
        translations[language]?.takeIf(String::isNotBlank)
            ?: translations["en"].orEmpty()

    fun pageCount(hasSupporters: Boolean): Int = if (hasSupporters) 2 else 1

    fun isLastPage(currentPage: Int, pageCount: Int): Boolean =
        currentPage >= (pageCount - 1).coerceAtLeast(0)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChangeLogContent(
    releaseNotes: String,
    hasSupporterPage: Boolean,
    supporters: List<String>,
    onOpenSupport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val sheetHeight = configuration.screenHeightDp.dp
    val pageHeight = (sheetHeight * 0.45f).coerceIn(140.dp, 520.dp)
    val count = ChangeLogContentResolver.pageCount(hasSupporterPage)
    val pagerState = rememberPagerState(pageCount = { count })
    val coroutineScope = rememberCoroutineScope()
    val dialogColor = Color(ThemesRepo.getColor(R.attr.dialogBackground))
    val supporterTop = Color(ThemesRepo.getColor(R.attr.boostyColorTop))
    val supporterBottom = Color(ThemesRepo.getColor(R.attr.boostyColorBottom))
    val onAccent = Color(ThemesRepo.getColor(R.attr.textColorOnAccent))
    val isSupporterPage = hasSupporterPage && pagerState.currentPage > 0
    val buttonColor = if (isSupporterPage) supporterTop else MaterialTheme.colorScheme.primary
    val buttonContentColor = if (isSupporterPage) onAccent else MaterialTheme.colorScheme.onPrimary

    LaunchedEffect(hasSupporterPage) {
        if (!hasSupporterPage && pagerState.currentPage > 0) pagerState.scrollToPage(0)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = (sheetHeight * 0.92f).coerceAtLeast(220.dp))
            .background(dialogColor, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(top = 12.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(if (isSupporterPage) R.string.ChangelogBoosty else R.string.Changelog),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 21.dp, vertical = 6.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = if (isSupporterPage) onAccent else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(pageHeight),
            contentPadding = PaddingValues(horizontal = 0.dp),
            key = { it },
        ) { page ->
            if (page == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(dialogColor)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = releaseNotes,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 16.sp,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(supporterTop, supporterBottom)))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.ChangelogBoostyDescription),
                        color = onAccent,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().weight(1f),
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
                            items(supporters) { name ->
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = onAccent,
                            contentColor = supporterTop,
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(text = stringResource(R.string.IntroBoostySupport), fontSize = 16.sp)
                    }
                }
            }
        }
        Button(
            onClick = {
                if (ChangeLogContentResolver.isLastPage(pagerState.currentPage, count)) {
                    onDismiss()
                } else {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp).heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = buttonContentColor,
            ),
        ) {
            Text(
                text = stringResource(
                    if (ChangeLogContentResolver.isLastPage(pagerState.currentPage, count)) {
                        R.string.ChangelogOK
                    } else {
                        R.string.ChangelogNext
                    },
                ),
                fontSize = 16.sp,
            )
        }
    }
}
