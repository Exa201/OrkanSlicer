package ru.ytkab0bp.slicebeam.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.SparseBooleanArray
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckedTextView
import androidx.compose.ui.platform.AbstractComposeView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils

open class BeamAlertDialogBuilder : MaterialAlertDialogBuilder {
    private var customView: View? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, overrideThemeResId: Int) : super(context, overrideThemeResId)

    override fun setView(view: View?): BeamAlertDialogBuilder {
        customView = view
        if (view != null && isDarkDialogBackground()) {
            applyDarkDialogText(view)
        }
        super.setView(view)
        return this
    }

    override fun create(): AlertDialog {
        val dialog = super.create()
        dialog.window!!.setBackgroundDrawable(
            GradientDrawable().apply {
                cornerRadius = ViewUtils.dp(32).toFloat()
                setColor(ThemesRepo.getColor(R.attr.dialogBackground))
            }
        )
        if (containsComposeView(customView)) {
            // AlertController checks native View.onCheckIsTextEditor before Compose
            // creates its text fields. It consequently blocks the IME for Compose
            // content. Clear that flag after onCreate, when the window attaches;
            // callers may still supply their own onShow listener.
            dialog.window!!.decorView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                }

                override fun onViewDetachedFromWindow(view: View) = Unit
            })
        }
        return dialog
    }

    private fun containsComposeView(view: View?): Boolean {
        if (view is AbstractComposeView) return true
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                if (containsComposeView(view.getChildAt(index))) return true
            }
        }
        return false
    }

    override fun show(): AlertDialog {
        val dialog = super.show()
        val decor = dialog.window!!.decorView
        val darkBackground = isDarkDialogBackground()
        if (darkBackground) applyDarkDialogText(decor)

        val resources = context.resources
        val titleId = resources.getIdentifier("alertTitle", "id", context.packageName)
        if (titleId > 0) {
            decor.findViewById<TextView>(titleId)?.setTextColor(
                ThemesRepo.getColor(android.R.attr.textColorPrimary)
            )
        }
        decor.findViewById<TextView>(android.R.id.message)?.setTextColor(
            ThemesRepo.getColor(android.R.attr.textColorSecondary)
        )

        val buttonColor = ThemesRepo.getColor(
            if (darkBackground) android.R.attr.textColorPrimary else android.R.attr.colorAccent
        )
        listOf(android.R.id.button1, android.R.id.button2, android.R.id.button3).forEach { id ->
            decor.findViewById<TextView>(id)?.setTextColor(buttonColor)
        }

        val listId = resources.getIdentifier("select_dialog_listview", "id", context.packageName)
        if (listId > 0) {
            val listView = decor.findViewById<ListView>(listId)
            val wrapped = listView?.adapter
            if (listView != null && wrapped != null) {
                val checked = listView.checkedItemPositions?.let { source ->
                    SparseBooleanArray(source.size()).apply {
                        for (index in 0 until source.size()) {
                            put(source.keyAt(index), source.valueAt(index))
                        }
                    }
                }
                listView.adapter = object : ListAdapter by wrapped {
                    @SuppressLint("RestrictedApi")
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val row = wrapped.getView(position, convertView, parent)
                        val text = row.findViewById<TextView>(android.R.id.text1)
                        if (text != null) {
                            text.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary))
                            if (text is AppCompatCheckedTextView) {
                                text.setSupportCompoundDrawablesTintList(
                                    ColorStateList(
                                        arrayOf(
                                            intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
                                            intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked)
                                        ),
                                        intArrayOf(
                                            ThemesRepo.getColor(android.R.attr.colorAccent),
                                            ThemesRepo.getColor(R.attr.dividerContrastColor)
                                        )
                                    )
                                )
                            }
                        }
                        return row
                    }

                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun getAutofillOptions(): Array<CharSequence>? = wrapped.autofillOptions
                }
                checked?.let { positions ->
                    for (index in 0 until positions.size()) {
                        listView.setItemChecked(positions.keyAt(index), positions.valueAt(index))
                    }
                }
            }
        }
        if (darkBackground) {
            // Adapter-backed views (such as Spinner) may create their selected child
            // only after the dialog's first layout pass.
            decor.post {
                if (dialog.isShowing) applyDarkDialogText(decor)
            }
        }
        return dialog
    }

    companion object {
        @JvmStatic
        fun applyDarkDialogText(view: View) {
            if (view is TextView) {
                if (ColorUtils.calculateLuminance(view.currentTextColor) < 0.38) {
                    view.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary))
                }
                if (view is EditText && ColorUtils.calculateLuminance(view.currentHintTextColor) < 0.38) {
                    view.setHintTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary))
                }
            }
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    applyDarkDialogText(view.getChildAt(index))
                }
            }
        }

        @JvmStatic
        fun isDarkDialogBackground(): Boolean =
            ColorUtils.calculateLuminance(ThemesRepo.getColor(R.attr.dialogBackground)) < 0.35
    }
}
