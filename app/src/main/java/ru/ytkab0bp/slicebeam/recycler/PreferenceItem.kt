package ru.ytkab0bp.slicebeam.recycler

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.theme.BeamTheme
import ru.ytkab0bp.slicebeam.theme.IThemeView
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils

open class PreferenceItem : SimpleRecyclerItem<PreferenceItem.PreferenceHolderView>() {
    private var mIcon: Drawable? = null
    private var mTitle: CharSequence? = null
    private var mSubtitle: ValueProvider? = null
    private var onClickListener: View.OnClickListener? = null
    private var onLongClickListener: View.OnLongClickListener? = null
    private var textColorRes = 0
    private var noTint = false
    private var valueProvider: ValueProvider? = null
    private var roundRadius = 0f
    private var mPaddings = ViewUtils.dp(12)
    private var mForceDark = false

    open fun setTitle(title: CharSequence?): PreferenceItem = apply { mTitle = title }

    open fun setSubtitle(subtitle: CharSequence?): PreferenceItem = apply {
        mSubtitle = ValueProvider { subtitle }
    }

    open fun setPaddings(paddings: Int): PreferenceItem = apply { mPaddings = paddings }

    open fun setForceDark(forceDark: Boolean): PreferenceItem = apply { mForceDark = forceDark }

    open fun setSubtitleProvider(provider: ValueProvider?): PreferenceItem = apply { mSubtitle = provider }

    open fun setValueProvider(provider: ValueProvider?): PreferenceItem = apply { valueProvider = provider }

    open fun setValue(text: String?): PreferenceItem = apply { valueProvider = ValueProvider { text } }

    open fun setIcon(iconRes: Int): PreferenceItem = apply {
        mIcon = ContextCompat.getDrawable(SliceBeam.requireInstance(), iconRes)
    }

    open fun setIcon(drawable: Drawable?): PreferenceItem = apply { mIcon = drawable }

    open fun setNoTint(noTint: Boolean): PreferenceItem = apply { this.noTint = noTint }

    open fun setRoundRadius(roundRadius: Float): PreferenceItem = apply { this.roundRadius = roundRadius }

    open fun setTextColorRes(textColorRes: Int): PreferenceItem = apply { this.textColorRes = textColorRes }

    open fun setOnClickListener(listener: View.OnClickListener?): PreferenceItem = apply { onClickListener = listener }

    open fun setOnLongClickListener(listener: View.OnLongClickListener?): PreferenceItem = apply { onLongClickListener = listener }

    override fun onCreateView(ctx: Context) = PreferenceHolderView(ctx)

    override fun onBindView(view: PreferenceHolderView) = view.bind(this)

    class PreferenceHolderView(context: Context) : LinearLayout(context), IThemeView {
        private val title: TextView
        private val subtitle: TextView
        private val icon: ImageView
        private val value: TextView
        private var radius = 0f
        private var item: PreferenceItem? = null

        init {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            icon = object : AppCompatImageView(context) {
                private val clipPath = Path()

                override fun draw(canvas: Canvas) {
                    if (radius != 0f) {
                        canvas.save()
                        clipPath.rewind()
                        clipPath.addRoundRect(
                            0f,
                            0f,
                            width.toFloat(),
                            height.toFloat(),
                            radius,
                            radius,
                            Path.Direction.CW,
                        )
                        canvas.clipPath(clipPath)
                    }
                    super.draw(canvas)
                    if (radius != 0f) canvas.restore()
                }
            }
            icon.layoutParams = LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28)).apply {
                marginStart = ViewUtils.dp(4)
                marginEnd = ViewUtils.dp(8)
            }
            addView(icon)

            val innerLayout = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER_VERTICAL
            }

            title = TextView(context).apply {
                ellipsize = TextUtils.TruncateAt.END
                setSingleLine(false)
                maxLines = 2
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            }
            innerLayout.addView(title)

            subtitle = TextView(context).apply {
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            }
            innerLayout.addView(subtitle)

            addView(
                innerLayout,
                LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = ViewUtils.dp(8)
                    marginEnd = ViewUtils.dp(8)
                },
            )

            value = TextView(context).apply {
                maxWidth = ViewUtils.dp(120)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setPadding(ViewUtils.dp(8), ViewUtils.dp(6), ViewUtils.dp(8), ViewUtils.dp(6))
                visibility = GONE
            }
            addView(value, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

            minimumHeight = ViewUtils.dp(64)
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            onApplyTheme()
        }

        fun bind(item: PreferenceItem) {
            this.item = item
            setPadding(item.mPaddings, item.mPaddings, item.mPaddings, item.mPaddings)
            title.text = item.mTitle
            title.visibility = if (TextUtils.isEmpty(item.mTitle)) GONE else VISIBLE

            val subtitleText = item.mSubtitle?.provide()
            subtitle.text = subtitleText
            subtitle.visibility = if (TextUtils.isEmpty(subtitleText)) GONE else VISIBLE

            val valueText = item.valueProvider?.provide()
            value.text = valueText
            value.visibility = if (TextUtils.isEmpty(valueText)) GONE else VISIBLE

            if (item.mIcon != null) {
                icon.visibility = VISIBLE
                icon.setImageDrawable(item.mIcon)
            } else {
                icon.visibility = GONE
            }

            // Clear recycled listeners so an item without an action cannot inherit one.
            setOnClickListener(item.onClickListener)
            isClickable = item.onClickListener != null
            setOnLongClickListener(item.onLongClickListener)

            if (item.textColorRes != 0) title.setTextColor(ThemesRepo.getColor(item.textColorRes))
            title.typeface = if (item.textColorRes != 0 || item.mIcon != null) {
                ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            } else {
                Typeface.DEFAULT
            }

            icon.imageTintList = if (item.noTint) {
                null
            } else {
                ColorStateList.valueOf(
                    ThemesRepo.getColor(
                        if (item.textColorRes != 0) item.textColorRes else android.R.attr.textColorSecondary,
                    ),
                )
            }
            radius = item.roundRadius
            icon.invalidate()

            icon.layoutParams = icon.layoutParams.apply {
                width = if (radius != 0f) ViewUtils.dp(42) else ViewUtils.dp(28)
                height = width
            }
            if (item.mForceDark) onApplyTheme()
        }

        override fun onApplyTheme() {
            val theme: BeamTheme = if (item?.mForceDark == true) BeamTheme.DARK else ThemesRepo.getCurrent()
            title.setTextColor(theme.colors.get(android.R.attr.textColorPrimary))
            subtitle.setTextColor(theme.colors.get(android.R.attr.textColorSecondary))
            value.setTextColor(theme.colors.get(android.R.attr.textColorSecondary))
            icon.imageTintList = ColorStateList.valueOf(theme.colors.get(android.R.attr.textColorSecondary))
            setBackground(ViewUtils.createRipple(theme.colors.get(android.R.attr.colorControlHighlight), 16f))
        }
    }

    fun interface ValueProvider {
        fun provide(): CharSequence?
    }
}
