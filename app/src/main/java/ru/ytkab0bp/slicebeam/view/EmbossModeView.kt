package ru.ytkab0bp.slicebeam.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.EmbossSurfaceClickedEvent
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.io.File

@SuppressLint("ViewConstructor")
class EmbossModeView(
    context: Context,
    private val glView: GLView,
    private val onExit: Runnable?,
) : FrameLayout(context) {
    private val eventBridge = EmbossModeEventBridge(::onEmbossSurfaceClicked)
    private val textInput: EditText
    private val sizeInput: EditText
    private val depthInput: EditText
    private val btnAdd: TextView
    private val btnSub: TextView
    private var isAddMode = true
    private var fontFilePath: String? = null

    init {
        prepareFontFile(context)

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewUtils.dp(16), ViewUtils.dp(16), ViewUtils.dp(16), ViewUtils.dp(16))
            setBackgroundResource(R.drawable.bottom_sheet_rounded_background)
            backgroundTintList = ColorStateList.valueOf(ThemesRepo.getColor(R.attr.dialogBackground))
        }

        val title = TextView(context).apply {
            setText(R.string.EmbossTitle)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary))
            setPadding(0, 0, 0, ViewUtils.dp(4))
        }
        panel.addView(title)

        val subtitle = TextView(context).apply {
            setText(R.string.EmbossInstruction)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary))
            setPadding(0, 0, 0, ViewUtils.dp(12))
        }
        panel.addView(subtitle)

        textInput = EditText(context).apply {
            setHint(R.string.EmbossTextHint)
            setText(R.string.AppName)
            setSingleLine(true)
        }
        panel.addView(
            createStyledInput(context, context.getString(R.string.EmbossTextLabel), textInput),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        val settingsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, ViewUtils.dp(8), 0, ViewUtils.dp(8))
        }

        sizeInput = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(R.string.EmbossDefaultFontSize)
        }
        val sizeInputView = createStyledInput(context, context.getString(R.string.EmbossFontSize), sizeInput)

        depthInput = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(R.string.EmbossDefaultDepth)
        }
        val depthInputView = createStyledInput(context, context.getString(R.string.EmbossDepth), depthInput)

        settingsRow.addView(
            sizeInputView,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = ViewUtils.dp(6)
            },
        )
        settingsRow.addView(
            depthInputView,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = ViewUtils.dp(6)
            },
        )
        panel.addView(settingsRow)

        val modeLabel = TextView(context).apply {
            setText(R.string.EmbossOperation)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary))
        }
        panel.addView(modeLabel)

        val modeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, ViewUtils.dp(16))
        }

        btnAdd = createTextButton(context, context.getString(R.string.EmbossRaise), accent = true)
        btnSub = createTextButton(context, context.getString(R.string.EmbossEngrave), accent = false)
        btnAdd.setOnClickListener {
            isAddMode = true
            updateModeButtons()
        }
        btnSub.setOnClickListener {
            isAddMode = false
            updateModeButtons()
        }

        modeRow.addView(
            btnAdd,
            LinearLayout.LayoutParams(0, ViewUtils.dp(44), 1f).apply {
                rightMargin = ViewUtils.dp(4)
            },
        )
        modeRow.addView(
            btnSub,
            LinearLayout.LayoutParams(0, ViewUtils.dp(44), 1f).apply {
                leftMargin = ViewUtils.dp(4)
            },
        )
        panel.addView(modeRow)

        val done = createTextButton(context, context.getString(R.string.ActionDone), accent = true)
        done.setOnClickListener { onExit?.run() }
        panel.addView(
            done,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(48)),
        )

        addView(
            panel,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.BOTTOM },
        )
        updateModeButtons()
    }

    private fun createStyledInput(context: Context, label: String, editText: EditText): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, ViewUtils.dp(4), 0, ViewUtils.dp(4))
        }
        val labelView = TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary))
            setPadding(0, 0, 0, ViewUtils.dp(4))
        }
        container.addView(labelView)

        editText.background = GradientDrawable().apply {
            setColor(ThemesRepo.getColor(android.R.attr.colorControlHighlight))
            cornerRadius = ViewUtils.dp(8).toFloat()
        }
        editText.setPadding(ViewUtils.dp(12), ViewUtils.dp(10), ViewUtils.dp(12), ViewUtils.dp(10))
        editText.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary))
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        container.addView(editText)
        return container
    }

    private fun updateModeButtons() {
        setButtonState(btnAdd, isAddMode)
        setButtonState(btnSub, !isAddMode)
    }

    private fun setButtonState(button: TextView, active: Boolean) {
        button.background = GradientDrawable().apply {
            cornerRadius = ViewUtils.dp(10).toFloat()
            setColor(
                ThemesRepo.getColor(
                    if (active) android.R.attr.colorAccent else android.R.attr.colorControlHighlight,
                ),
            )
        }
        button.setTextColor(
            ThemesRepo.getColor(
                if (active) R.attr.textColorOnAccent else android.R.attr.textColorPrimary,
            ),
        )
    }

    private fun createTextButton(context: Context, text: String, accent: Boolean): TextView =
        TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            background = GradientDrawable().apply {
                cornerRadius = ViewUtils.dp(10).toFloat()
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

    private fun prepareFontFile(context: Context) {
        try {
            val fontFile = File(context.cacheDir, "roboto_medium.ttf")
            if (!fontFile.exists()) {
                context.assets.open("font/roboto_medium.ttf").use { input ->
                    fontFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            fontFilePath = fontFile.absolutePath
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SliceBeam.EVENT_BUS.registerListener(eventBridge)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        SliceBeam.EVENT_BUS.unregisterListener(eventBridge)
    }

    fun onEmbossSurfaceClicked(event: EmbossSurfaceClickedEvent) {
        val selectedObject = glView!!.renderer.selectedObject
        if (selectedObject == -1) {
            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.SelectObjectFirst))
            return
        }

        if (fontFilePath == null) prepareFontFile(context)
        val resolvedFontPath = fontFilePath
        if (resolvedFontPath == null) {
            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.EmbossFailed, "Schriftart konnte nicht geladen werden"))
            return
        }

        val text = textInput.text.toString().trim().ifEmpty { "text" }
        val size = sizeInput.text.toString().replace(',', '.').toFloatOrNull() ?: 10f
        val depth = depthInput.text.toString().replace(',', '.').toFloatOrNull() ?: 1f
        val operationType = if (isAddMode) 0 else 1

        glView!!.queueEvent {
            try {
                glView!!.renderer.model!!.embossText(
                    selectedObject,
                    resolvedFontPath,
                    text,
                    size,
                    depth,
                    operationType,
                    event.position,
                    event.normal,
                )
                glView!!.renderer.invalidateGlModel(selectedObject)
                glView!!.requestRender()
                ViewUtils.postOnMainThread {
                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.EmbossSuccess))
                }
            } catch (error: Exception) {
                error.printStackTrace()
                ViewUtils.postOnMainThread {
                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.EmbossFailed, error.message))
                }
            }
        }
    }
}
