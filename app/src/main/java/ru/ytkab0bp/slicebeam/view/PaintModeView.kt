package ru.ytkab0bp.slicebeam.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.render.GLRenderer
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.ViewUtils

/** Overlay for painting object colors, support, seams, and fuzzy-skin regions. */
@SuppressLint("ViewConstructor")
class PaintModeView @JvmOverloads constructor(
    context: Context,
    private val glView: GLView,
    private val onExit: Runnable?,
    private val mode: Int = GLRenderer.PAINT_MODE_COLOR,
) : FrameLayout(context) {
    private val slots: List<FilamentSlot> = Prefs.getFilamentSlots()
    private lateinit var swatchRow: LinearLayout
    private var selectedFilament = 2 // 1-based; 1 = base color
    private var isFinishing = false
    private lateinit var brushSize: SeekBar
    private lateinit var brushRadiusLabel: TextView
    private lateinit var fab: ImageView
    private var brushActive = true
    private lateinit var brushBtn: TextView
    private lateinit var bucketBtn: TextView
    private lateinit var heightBtn: TextView
    private lateinit var heightBandLabel: TextView
    private lateinit var heightBandWidthSeek: SeekBar
    private lateinit var bucketAngleLabel: TextView
    private lateinit var bucketAngleSeek: SeekBar
    private lateinit var brushTypeBtn: TextView
    private val stateButtons = mutableListOf<TextView>()
    private val stateButtonValues = mutableListOf<Int>()
    private val stateButtonColors = mutableListOf<Int>()

    init {
        if (!isColorMode || slots.size < 2) selectedFilament = 1
        build(context)
        glView!!.isPaintBrushActive = true
        if (!isColorMode) {
            val filament = selectedFilament
            runOnGl { glView!!.renderer.paintFilament = filament }
        }
    }

    private val isColorMode: Boolean
        get() = mode == GLRenderer.PAINT_MODE_COLOR

    private fun runOnGl(action: () -> Unit) {
        glView!!.queueEvent {
            action()
            glView!!.requestRender()
        }
    }

    private fun build(ctx: Context) {
        fab = ImageView(ctx)
        updateFab()
        fab.also { button ->
            val background = GradientDrawable().apply {
                setColor(ThemesRepo.getColor(android.R.attr.colorAccent))
                shape = GradientDrawable.OVAL
            }
            button.background = background
            button.setPadding(ViewUtils.dp(14), ViewUtils.dp(14), ViewUtils.dp(14), ViewUtils.dp(14))
            button.setOnClickListener {
                brushActive = !brushActive
                glView!!.isPaintBrushActive = brushActive
                updateFab()
            }
        }
        addView(
            fab,
            FrameLayout.LayoutParams(ViewUtils.dp(56), ViewUtils.dp(56)).apply {
                gravity = Gravity.END or Gravity.TOP
                topMargin = ViewUtils.dp(12)
                marginEnd = ViewUtils.dp(12)
            },
        )

        val panel = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewUtils.dp(12), ViewUtils.dp(10), ViewUtils.dp(12), ViewUtils.dp(12))
            setBackgroundResource(R.drawable.bottom_sheet_rounded_background)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                ThemesRepo.getColor(R.attr.dialogBackground),
            )
        }

        if (isColorMode) {
            panel.addView(label(ctx, ctx.getString(R.string.PaintBaseFilament)))
            val baseScroll = HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false }
            val baseRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            slots.forEachIndexed { index, slot ->
                val filament = index + 1
                val color = slot.color or 0xFF000000.toInt()
                val swatch = View(ctx).apply {
                    contentDescription = ctx.getString(
                        R.string.FilamentPaletteSlotDescription,
                        filament,
                        slot.type?.takeIf(String::isNotBlank) ?: ctx.getString(R.string.FilamentType),
                    )
                    background = GradientDrawable().apply {
                        setColor(color)
                        cornerRadius = ViewUtils.dp(10).toFloat()
                        if (ColorUtils.calculateLuminance(color) > 0.85) {
                            setStroke(ViewUtils.dp(1), ThemesRepo.getColor(R.attr.dividerColor))
                        }
                    }
                    setOnClickListener {
                        runOnGl {
                            val objectIndex = glView!!.renderer.paintObject
                            if (objectIndex != -1) {
                                glView!!.renderer.setObjectBaseFilament(objectIndex, filament)
                            }
                        }
                    }
                }
                baseRow.addView(
                    swatch,
                    LinearLayout.LayoutParams(ViewUtils.dp(48), ViewUtils.dp(48)).apply {
                        rightMargin = ViewUtils.dp(8)
                    },
                )
            }
            baseScroll.addView(baseRow)
            panel.addView(
                baseScroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = ViewUtils.dp(10) },
            )

            panel.addView(label(ctx, ctx.getString(R.string.PaintWith)))
            val swatchScroll = HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false }
            swatchRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            swatchScroll.addView(swatchRow)
            rebuildSwatches(ctx)
            panel.addView(
                swatchScroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = ViewUtils.dp(10) },
            )
        } else {
            panel.addView(label(ctx, ctx.getString(R.string.PaintWith)))
            val stateRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            val enforceLabel = ctx.getString(
                if (mode == GLRenderer.PAINT_MODE_FUZZY) R.string.PaintFuzzy else R.string.PaintEnforce,
            )
            stateRow.addView(stateButton(ctx, enforceLabel, 1, 0xFF2ECC71.toInt()), stateLp())
            if (mode != GLRenderer.PAINT_MODE_FUZZY) {
                stateRow.addView(stateButton(ctx, ctx.getString(R.string.PaintBlock), 2, 0xFFE74C3C.toInt()), stateLp())
            }
            stateRow.addView(
                stateButton(
                    ctx,
                    ctx.getString(R.string.PaintErase),
                    0,
                    ThemesRepo.getColor(android.R.attr.colorControlHighlight),
                ),
                stateLp(),
            )
            panel.addView(
                stateRow,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = ViewUtils.dp(10) },
            )
        }

        val tools = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        brushBtn = toolButton(ctx, ctx.getString(R.string.PaintBrush), GLRenderer.PAINT_TOOL_BRUSH)
        bucketBtn = toolButton(ctx, ctx.getString(R.string.PaintBucket), GLRenderer.PAINT_TOOL_BUCKET)
        heightBtn = toolButton(ctx, ctx.getString(R.string.PaintHeight), GLRenderer.PAINT_TOOL_HEIGHT)
        tools.addView(brushBtn, toolLp())
        tools.addView(bucketBtn, toolLp())
        tools.addView(heightBtn, toolLp())
        panel.addView(
            tools,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = ViewUtils.dp(8) },
        )

        brushSize = SeekBar(ctx).apply {
            max = 20
            progress = 4
            contentDescription = ctx.getString(R.string.PaintBrushRadius, progress.toFloat())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val radius = maxOf(1, progress).toDouble()
                    brushRadiusLabel.text = ctx.getString(R.string.PaintBrushRadius, radius.toFloat())
                    seekBar.contentDescription = brushRadiusLabel.text
                    runOnGl { glView!!.renderer.paintBrushRadius = radius }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }
        brushRadiusLabel = label(ctx, ctx.getString(R.string.PaintBrushRadius, brushSize.progress.toFloat()))
        panel.addView(
            brushRadiusLabel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        panel.addView(
            brushSize,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewUtils.dp(48),
            ).apply { bottomMargin = ViewUtils.dp(8) },
        )

        brushTypeBtn = TextView(ctx).apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            setPadding(ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8))
            setOnClickListener {
                runOnGl {
                    val sphere = !glView!!.renderer.isBrushSphere
                    glView!!.renderer.isBrushSphere = sphere
                    post { updateBrushTypeBtnStyle() }
                }
            }
        }
        updateBrushTypeBtnStyle()
        panel.addView(
            brushTypeBtn,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewUtils.dp(48),
            ).apply { bottomMargin = ViewUtils.dp(8) },
        )

        heightBandLabel = label(ctx, ctx.getString(R.string.PaintHeightBand, 10.0))
        panel.addView(
            heightBandLabel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        heightBandWidthSeek = SeekBar(ctx).apply {
            max = 100
            progress = 10
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val width = maxOf(1, progress).toDouble()
                    heightBandLabel.text = ctx.getString(R.string.PaintHeightBand, width)
                    runOnGl { glView!!.renderer.heightBandWidth = width }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }
        panel.addView(
            heightBandWidthSeek,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = ViewUtils.dp(8) },
        )

        bucketAngleLabel = label(ctx, ctx.getString(R.string.PaintAngleLimit, 30))
        panel.addView(
            bucketAngleLabel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        bucketAngleSeek = SeekBar(ctx).apply {
            max = 90
            progress = 30
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val angle = maxOf(1, progress).toDouble()
                    bucketAngleLabel.text = ctx.getString(R.string.PaintAngleLimit, angle.toInt())
                    runOnGl { glView!!.renderer.bucketAngleThreshold = angle }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }
        panel.addView(
            bucketAngleSeek,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = ViewUtils.dp(8) },
        )

        selectTool(GLRenderer.PAINT_TOOL_BRUSH)

        val actions = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val clear = textButton(ctx, ctx.getString(R.string.ActionClear), accent = false).apply {
            setOnClickListener { runOnGl { glView!!.renderer.clearPaint() } }
        }
        val done = textButton(ctx, ctx.getString(R.string.ActionDone), accent = true).apply {
            setOnClickListener { finish() }
        }
        actions.addView(
            clear,
            LinearLayout.LayoutParams(0, ViewUtils.dp(48), 1f).apply { rightMargin = ViewUtils.dp(6) },
        )
        actions.addView(
            done,
            LinearLayout.LayoutParams(0, ViewUtils.dp(48), 1f).apply { leftMargin = ViewUtils.dp(6) },
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
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.BOTTOM },
        )
    }

    /** Commits the active GL paint session once, then notifies the owner on the UI thread. */
    fun finish() {
        if (isFinishing) return
        isFinishing = true
        glView.isPaintBrushActive = false
        glView.queueEvent {
            glView.renderer.endPaint(true)
            glView.requestRender()
            post { onExit?.run() }
        }
    }

    private fun label(ctx: Context, text: String): TextView = TextView(ctx).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary))
        setPadding(ViewUtils.dp(2), 0, 0, ViewUtils.dp(4))
    }

    private fun updateFab() {
        fab.setImageResource(if (brushActive) R.drawable.edit_outline_28 else R.drawable.rectangle_hand_point_up_28)
        fab.setColorFilter(ThemesRepo.getColor(R.attr.textColorOnAccent))
        fab.contentDescription = context.getString(
            if (brushActive) R.string.PaintDisableBrush else R.string.PaintEnableBrush,
        )
        fab.isSelected = brushActive
    }

    private fun toolLp(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, ViewUtils.dp(48), 1f).apply {
            leftMargin = ViewUtils.dp(4)
            rightMargin = ViewUtils.dp(4)
        }

    private fun toolButton(ctx: Context, label: String, tool: Int): TextView = TextView(ctx).apply {
        text = label
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        setOnClickListener {
            selectTool(tool)
            runOnGl { glView!!.renderer.paintTool = tool }
        }
        contentDescription = label
    }

    private fun updateBrushTypeBtnStyle() {
        val sphere = glView!!.renderer?.isBrushSphere ?: false
        brushTypeBtn.setText(if (sphere) R.string.PaintBrushSphere else R.string.PaintBrushCircle)
        brushTypeBtn.background = GradientDrawable().apply {
            cornerRadius = ViewUtils.dp(12).toFloat()
            setColor(ThemesRepo.getColor(android.R.attr.colorControlHighlight))
        }
        brushTypeBtn.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary))
        brushTypeBtn.isSelected = sphere
    }

    private fun selectTool(tool: Int) {
        styleTool(brushBtn, tool == GLRenderer.PAINT_TOOL_BRUSH)
        styleTool(bucketBtn, tool == GLRenderer.PAINT_TOOL_BUCKET)
        styleTool(heightBtn, tool == GLRenderer.PAINT_TOOL_HEIGHT)
        brushBtn.isSelected = tool == GLRenderer.PAINT_TOOL_BRUSH
        bucketBtn.isSelected = tool == GLRenderer.PAINT_TOOL_BUCKET
        heightBtn.isSelected = tool == GLRenderer.PAINT_TOOL_HEIGHT
        brushSize.visibility = if (tool == GLRenderer.PAINT_TOOL_BRUSH) View.VISIBLE else View.GONE
        brushTypeBtn.visibility = if (tool == GLRenderer.PAINT_TOOL_BRUSH) View.VISIBLE else View.GONE
        heightBandLabel.visibility = if (tool == GLRenderer.PAINT_TOOL_HEIGHT) View.VISIBLE else View.GONE
        heightBandWidthSeek.visibility = if (tool == GLRenderer.PAINT_TOOL_HEIGHT) View.VISIBLE else View.GONE
        bucketAngleLabel.visibility = if (tool == GLRenderer.PAINT_TOOL_BUCKET) View.VISIBLE else View.GONE
        bucketAngleSeek.visibility = if (tool == GLRenderer.PAINT_TOOL_BUCKET) View.VISIBLE else View.GONE
    }

    private fun styleTool(view: TextView, selected: Boolean) {
        view.background = GradientDrawable().apply {
            cornerRadius = ViewUtils.dp(12).toFloat()
            setColor(
                ThemesRepo.getColor(
                    if (selected) android.R.attr.colorAccent else android.R.attr.colorControlHighlight,
                ),
            )
        }
        view.setTextColor(
            ThemesRepo.getColor(
                if (selected) R.attr.textColorOnAccent else android.R.attr.textColorPrimary,
            ),
        )
    }

    private fun textButton(ctx: Context, label: String, accent: Boolean): TextView = TextView(ctx).apply {
        text = label
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        background = GradientDrawable().apply {
            cornerRadius = ViewUtils.dp(16).toFloat()
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

    private fun rebuildSwatches(ctx: Context) {
        swatchRow.removeAllViews()
        slots.forEachIndexed { index, slot ->
            val filament = index + 1
            val color = slot.color or 0xFF000000.toInt()
            val swatch = TextView(ctx).apply {
                text = filament.toString()
                contentDescription = ctx.getString(
                    R.string.FilamentPaletteSlotDescription,
                    filament,
                    slot.type?.takeIf(String::isNotBlank) ?: ctx.getString(R.string.FilamentType),
                )
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
                typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
                setTextColor(if (ColorUtils.calculateLuminance(color) > 0.5) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                applySwatchBg(this, color, filament == selectedFilament)
                isSelected = filament == selectedFilament
                setOnClickListener {
                    selectedFilament = filament
                    runOnGl { glView!!.renderer.paintFilament = filament }
                    rebuildSwatches(ctx)
                }
            }
            swatchRow.addView(
                swatch,
                LinearLayout.LayoutParams(ViewUtils.dp(48), ViewUtils.dp(48)).apply {
                    rightMargin = ViewUtils.dp(8)
                },
            )
        }
    }

    private fun applySwatchBg(view: View, color: Int, selected: Boolean) {
        view.background = GradientDrawable().apply {
            setColor(color)
            cornerRadius = ViewUtils.dp(10).toFloat()
            if (selected) {
                setStroke(ViewUtils.dp(3), ThemesRepo.getColor(android.R.attr.colorAccent))
            } else if (ColorUtils.calculateLuminance(color) > 0.85) {
                setStroke(ViewUtils.dp(1), ThemesRepo.getColor(R.attr.dividerColor))
            }
        }
    }

    private fun stateLp(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, ViewUtils.dp(48), 1f).apply {
            leftMargin = ViewUtils.dp(4)
            rightMargin = ViewUtils.dp(4)
        }

    private fun stateButton(ctx: Context, label: String, state: Int, activeColor: Int): TextView =
        TextView(ctx).apply {
            text = label
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            stateButtons += this
            stateButtonValues += state
            stateButtonColors += activeColor
            setOnClickListener {
                selectedFilament = state
                runOnGl { glView!!.renderer.paintFilament = state }
                restyleStateButtons()
            }
            restyleStateButtons()
        }

    private fun restyleStateButtons() {
        stateButtons.indices.forEach { index ->
            val selected = stateButtonValues[index] == selectedFilament
            val activeColor = stateButtonColors[index]
            val button = stateButtons[index]
            button.isSelected = selected
            button.background = GradientDrawable().apply {
                cornerRadius = ViewUtils.dp(12).toFloat()
                setColor(
                    if (selected) activeColor
                    else ThemesRepo.getColor(android.R.attr.colorControlHighlight),
                )
            }
            val lightActive = ColorUtils.calculateLuminance(activeColor) > 0.5
            button.setTextColor(
                if (selected) {
                    if (lightActive) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                } else {
                    ThemesRepo.getColor(android.R.attr.textColorPrimary)
                },
            )
        }
    }
}
