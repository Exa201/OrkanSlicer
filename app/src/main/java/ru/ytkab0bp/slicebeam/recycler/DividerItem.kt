package ru.ytkab0bp.slicebeam.recycler

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.view.DividerView

class DividerItem : SimpleRecyclerItem<DividerView>() {
    override fun onCreateView(ctx: Context): DividerView = DividerView(ctx).apply {
        layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewUtils.dp(1),
        ).apply {
            leftMargin = ViewUtils.dp(16)
            rightMargin = ViewUtils.dp(16)
            topMargin = ViewUtils.dp(6)
            bottomMargin = ViewUtils.dp(6)
        }
    }
}
