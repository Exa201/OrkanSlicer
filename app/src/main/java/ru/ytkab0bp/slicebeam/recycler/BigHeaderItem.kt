package ru.ytkab0bp.slicebeam.recycler

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils

class BigHeaderItem @JvmOverloads constructor(t: String? = null) : SimpleRecyclerItem<TextView>() {
    @JvmField
    var title: String? = t

    override fun onCreateView(ctx: Context): TextView = TextView(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
        typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary))
        setPadding(ViewUtils.dp(21), ViewUtils.dp(12), ViewUtils.dp(21), ViewUtils.dp(12))
        gravity = Gravity.START
        layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onBindView(view: TextView) {
        view.text = title
    }
}
