package ru.ytkab0bp.slicebeam.ui.setup

import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged

internal interface SetupComposePage {
    @Composable
    fun Content()

    fun onPageCreated()
}

internal data class SetupPageNavigationRequest(
    val id: Int,
    val page: Int,
    val animated: Boolean = true,
)

/** Compose owns the setup flow and page navigation while each existing page keeps its behavior. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SetupActivityRoot(
    pages: List<SetupComposePage>,
    pageCount: Int,
    initialPage: Int,
    navigationRequest: SetupPageNavigationRequest?,
    userScrollEnabled: Boolean,
    contentInsets: PaddingValues,
    backgroundAlpha: Float,
    title: String,
    titleTranslationYPx: Float,
    titleTopInsetPx: Int,
    titleScale: Float,
    titleColor: Int,
    onRootHeightChanged: (Int) -> Unit,
    onTitleHeightChanged: (Int) -> Unit,
    onPagerStateChanged: (position: Float, page: Int, settled: Boolean) -> Unit,
) {
    val safePageCount = pageCount.coerceIn(1, pages.size.coerceAtLeast(1))
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, safePageCount - 1),
        pageCount = { safePageCount },
    )

    LaunchedEffect(safePageCount) {
        val lastPage = safePageCount - 1
        if (pagerState.currentPage > lastPage) pagerState.scrollToPage(lastPage)
    }
    LaunchedEffect(navigationRequest?.id) {
        val request = navigationRequest ?: return@LaunchedEffect
        val targetPage = request.page.coerceIn(0, safePageCount - 1)
        if (request.animated) {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = spring(stiffness = 600f, dampingRatio = 1f),
            )
        } else {
            pagerState.scrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow {
            Triple(
                pagerState.currentPage + pagerState.currentPageOffsetFraction,
                pagerState.currentPage,
                pagerState.isScrollInProgress,
            )
        }.distinctUntilChanged().collect { (position, page, scrolling) ->
            onPagerStateChanged(position, page, !scrolling)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { onRootHeightChanged(it.height) },
    ) {
        SetupActivityBackdrop(
            backgroundAlpha = backgroundAlpha,
            title = title,
            titleTranslationYPx = titleTranslationYPx,
            titleTopInsetPx = titleTopInsetPx,
            titleScale = titleScale,
            titleColor = titleColor,
            onTitleHeightChanged = onTitleHeightChanged,
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(contentInsets),
            userScrollEnabled = userScrollEnabled,
            key = { page -> pages.getOrNull(page)?.let(System::identityHashCode) ?: page },
            pageSpacing = 0.dp,
        ) { page ->
            val item = pages.getOrNull(page)
            if (item != null) SetupActivityPage(item)
        }
    }
}

@Composable
private fun SetupActivityPage(item: SetupComposePage) {
    LaunchedEffect(item) { item.onPageCreated() }
    item.Content()
}
