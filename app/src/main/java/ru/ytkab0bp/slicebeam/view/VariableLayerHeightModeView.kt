package ru.ytkab0bp.slicebeam.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.io.File

@SuppressLint("ViewConstructor")
class VariableLayerHeightModeView(
    context: Context,
    private val glView: GLView,
    private val onExit: Runnable?,
) : FrameLayout(context) {
    private val qualityText: TextView
    private val seekBar: SeekBar
    private val statusValue: TextView

    init {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewUtils.dp(16), ViewUtils.dp(16), ViewUtils.dp(16), ViewUtils.dp(16))
            setBackgroundResource(R.drawable.bottom_sheet_rounded_background)
            backgroundTintList = ColorStateList.valueOf(ThemesRepo.getColor(R.attr.dialogBackground))
        }

        val title = TextView(context).apply {
            setText(R.string.VariableLayerTitle)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary))
            setPadding(0, 0, 0, ViewUtils.dp(8))
        }
        panel.addView(title)

        statusValue = createValueRow(panel, context.getString(R.string.VariableLayerStatus))
        panel.addView(
            View(context),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(8)),
        )

        qualityText = TextView(context).apply {
            text = context.getString(R.string.VariableLayerQuality, 0.5)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary))
        }
        panel.addView(qualityText)

        seekBar = SeekBar(context).apply {
            max = 100
            progress = 50
            setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        val value = progress / 100.0
                        qualityText.text = context.getString(R.string.VariableLayerQuality, value)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                    override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
                },
            )
        }
        panel.addView(
            seekBar,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(32)),
        )

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, ViewUtils.dp(16), 0, 0)
        }

        val clear = createTextButton(context, context.getString(R.string.ActionClear), accent = false)
        clear.setOnClickListener {
            val selectedObject = glView!!.renderer.selectedObject
            if (selectedObject != -1) {
                glView!!.renderer.model!!.clearAdaptiveLayerHeight(selectedObject)
                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.VariableLayerCleared))
                updateStatus()
            } else {
                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.NoObjectSelected))
            }
        }

        val apply = createTextButton(context, context.getString(R.string.VariableLayerAdaptive), accent = true)
        apply.setOnClickListener {
            val selectedObject = glView!!.renderer.selectedObject
            if (selectedObject != -1) {
                try {
                    SliceBeam.genCurrentConfig()
                    val configFile: File = SliceBeam.getCurrentConfigFile()
                    val quality = seekBar.progress / 100f
                    glView!!.renderer.model!!.applyAdaptiveLayerHeight(
                        selectedObject,
                        configFile.absolutePath,
                        quality,
                    )
                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.VariableLayerApplied))
                    updateStatus()
                } catch (error: Exception) {
                    SliceBeam.EVENT_BUS.fireEvent(
                        NeedSnackbarEvent(R.string.VariableLayerFailed, error.message),
                    )
                }
            } else {
                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.NoObjectSelected))
            }
        }

        val done = createTextButton(context, context.getString(R.string.ActionDone), accent = false)
        done.setOnClickListener { onExit?.run() }

        actions.addView(
            clear,
            LinearLayout.LayoutParams(0, ViewUtils.dp(48), 1f).apply {
                rightMargin = ViewUtils.dp(4)
            },
        )
        actions.addView(
            apply,
            LinearLayout.LayoutParams(0, ViewUtils.dp(48), 1.2f).apply {
                leftMargin = ViewUtils.dp(4)
                rightMargin = ViewUtils.dp(4)
            },
        )
        actions.addView(
            done,
            LinearLayout.LayoutParams(0, ViewUtils.dp(48), 1f).apply {
                leftMargin = ViewUtils.dp(4)
            },
        )
        panel.addView(
            actions,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        addView(
            panel,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.BOTTOM },
        )
        updateStatus()
    }

    private fun createValueRow(parent: LinearLayout, label: String): TextView {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, ViewUtils.dp(4), 0, ViewUtils.dp(4))
        }
        val labelView = TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary))
        }
        row.addView(labelView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val valueView = TextView(context).apply {
            text = "--"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary))
        }
        row.addView(
            valueView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        parent.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        return valueView
    }

    private fun updateStatus() {
        val selectedObject = glView!!.renderer.selectedObject
        if (selectedObject != -1) {
            if (glView!!.renderer.model!!.hasAdaptiveLayerHeight(selectedObject)) {
                statusValue.setText(R.string.VariableLayerAdaptive)
                statusValue.setTextColor(ThemesRepo.getColor(android.R.attr.colorAccent))
            } else {
                statusValue.setText(R.string.VariableLayerDefault)
                statusValue.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary))
            }
        } else {
            statusValue.setText(R.string.NoObjectSelected)
            statusValue.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary))
        }
    }

    private fun createTextButton(context: Context, text: String, accent: Boolean): TextView =
        TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            background = GradientDrawable().apply {
                cornerRadius = ViewUtils.dp(12).toFloat()
                setColor(
                    ThemesRepo.getColor(
                        if (accent) android.R.attr.colorAccent else android.R.attr.colorControlHighlight,
                    ),
                )
            }
            setTextColor(
                ThemesRepo.getColor(
                    if (accent) R.attr.textColorOnAccent else android.R.attr.textColorPrimary,
                ),
            )
        }
}
