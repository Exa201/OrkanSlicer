package ru.ytkab0bp.slicebeam.recycler

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.theme.IThemeView
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.view.BeamSwitch

open class PreferenceSwitchItem : SimpleRecyclerItem<PreferenceSwitchItem.SwitchPreferenceHolderView>() {
    private var mIcon: Drawable? = null
    private var mTitle: CharSequence? = null
    private var mSubtitle: CharSequence? = null
    private var mKey: String? = null
    private var mDefaultValue = false
    private var mChangeListener: CompoundButton.OnCheckedChangeListener? = null
    private var mLongClickListener: View.OnLongClickListener? = null
    private var valueProvider: ValueProvider? = null

    open fun setValueProvider(provider: ValueProvider?): PreferenceSwitchItem = apply { valueProvider = provider }

    open fun setKeyAndDefaultValue(key: String?, value: Boolean): PreferenceSwitchItem = apply {
        mKey = key
        mDefaultValue = value
    }

    open fun setChangeListener(listener: CompoundButton.OnCheckedChangeListener?): PreferenceSwitchItem = apply {
        mChangeListener = listener
    }

    open fun setLongClickListener(listener: View.OnLongClickListener?): PreferenceSwitchItem = apply {
        mLongClickListener = listener
    }

    open fun setTitle(title: CharSequence?): PreferenceSwitchItem = apply { mTitle = title }

    open fun setSubtitle(subtitle: CharSequence?): PreferenceSwitchItem = apply { mSubtitle = subtitle }

    open fun setIcon(iconRes: Int): PreferenceSwitchItem = apply {
        mIcon = ContextCompat.getDrawable(SliceBeam.requireInstance(), iconRes)
    }

    open fun setIcon(drawable: Drawable?): PreferenceSwitchItem = apply { mIcon = drawable }

    override fun onCreateView(ctx: Context) = SwitchPreferenceHolderView(ctx)

    override fun onBindView(view: SwitchPreferenceHolderView) = view.bind(this)

    class SwitchPreferenceHolderView(context: Context) : LinearLayout(context), IThemeView {
        @JvmField var title: TextView = TextView(context)
        @JvmField var subtitle: TextView = TextView(context)
        @JvmField var icon: ImageView = ImageView(context)
        @JvmField var matSwitch: BeamSwitch = BeamSwitch(context)

        init {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            icon.layoutParams = LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28)).apply {
                marginStart = ViewUtils.dp(4)
                marginEnd = ViewUtils.dp(8)
            }
            addView(icon)

            val innerLayout = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER_VERTICAL
            }

            title.apply {
                ellipsize = TextUtils.TruncateAt.END
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            }
            innerLayout.addView(title)

            subtitle.apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            }
            innerLayout.addView(subtitle)

            addView(
                innerLayout,
                LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = ViewUtils.dp(8)
                    marginEnd = ViewUtils.dp(8)
                    gravity = Gravity.CENTER_VERTICAL
                },
            )

            addView(matSwitch, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewUtils.dp(32)))

            val padding = ViewUtils.dp(12)
            setPadding(padding, padding, padding, padding)
            minimumHeight = ViewUtils.dp(64)
            onApplyTheme()
        }

        fun bind(item: PreferenceSwitchItem) {
            title.text = item.mTitle
            subtitle.text = item.mSubtitle
            subtitle.visibility = if (TextUtils.isEmpty(item.mSubtitle)) GONE else VISIBLE

            if (item.mIcon != null) {
                icon.visibility = VISIBLE
                icon.setImageDrawable(item.mIcon)
            } else {
                icon.visibility = GONE
            }

            matSwitch.isChecked = if (item.mKey != null) {
                Prefs.getPrefs()?.getBoolean(item.mKey, item.mDefaultValue) ?: item.mDefaultValue
            } else {
                checkNotNull(item.valueProvider) { "Switch row needs a key or a value provider" }.provide()
            }

            setOnClickListener {
                val checked = if (item.mKey != null) {
                    val prefs = Prefs.getPrefs()
                    val newValue = !(prefs?.getBoolean(item.mKey, item.mDefaultValue) ?: item.mDefaultValue)
                    prefs?.edit { putBoolean(item.mKey, newValue) }
                    matSwitch.isChecked = newValue
                    newValue
                } else {
                    (!matSwitch.isChecked).also { matSwitch.isChecked = it }
                }

                item.mChangeListener?.onCheckedChanged(matSwitch, checked)
            }
            setOnLongClickListener(item.mLongClickListener)
        }

        override fun onApplyTheme() {
            title.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary))
            subtitle.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary))
            icon.imageTintList = ColorStateList.valueOf(ThemesRepo.getColor(android.R.attr.textColorSecondary))
            setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 16f))
        }
    }

    fun interface ValueProvider {
        fun provide(): Boolean
    }
}
