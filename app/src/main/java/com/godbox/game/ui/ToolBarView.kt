package com.godbox.game.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import com.godbox.game.R
import com.godbox.game.engine.EntityType
import com.godbox.game.engine.TerrainType
import com.godbox.game.input.DisasterKind
import com.godbox.game.input.ToolCategory
import com.godbox.game.input.ToolState

/**
 * 底部工具栏：渐变背景 + 圆形主按钮 + chip 子选项。
 */
class ToolBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    var toolState: ToolState? = null
    var onPauseToggle: (() -> Unit)? = null
    var onClear: (() -> Unit)? = null

    private val subRow: LinearLayout
    private val subScroll: HorizontalScrollView
    private val mainRow: LinearLayout

    init {
        orientation = VERTICAL

        // 渐变背景
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.argb(0, 0, 0, 0),
                Color.argb(220, 13, 21, 69)
            )
        )

        // 顶部 1px 分隔线
        val divider = View(context).apply {
            setBackgroundColor(Color.argb(80, 255, 215, 64))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 1)
        }
        addView(divider)

        // 子选项（放上面）
        subRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }
        subScroll = HorizontalScrollView(context).apply {
            addView(subRow)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            isHorizontalScrollBarEnabled = false
        }
        addView(subScroll)

        // 主按钮行
        mainRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        mainRow.addView(makeButton(R.drawable.ic_terrain) { selectCategory(ToolCategory.TERRAIN) })
        mainRow.addView(makeButton(R.drawable.ic_entity) { selectCategory(ToolCategory.ENTITY) })
        mainRow.addView(makeButton(R.drawable.ic_disaster) { selectCategory(ToolCategory.DISASTER) })
        mainRow.addView(makeButton(R.drawable.ic_godhand) {
            toolState?.category = ToolCategory.GOD_HAND
        })
        mainRow.addView(makeButton(R.drawable.ic_pause) {
            onPauseToggle?.invoke()
        })
        mainRow.addView(makeButton(R.drawable.ic_clear) {
            onClear?.invoke()
        })

        addView(mainRow)
    }

    private fun makeButton(drawableId: Int, onClick: () -> Unit): ImageButton {
        val btn = ImageButton(context).apply {
            setImageResource(drawableId)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(120, 255, 255, 255))
            }
            layoutParams = LinearLayout.LayoutParams(96, 96).apply {
                marginStart = 6; marginEnd = 6
            }
            setOnClickListener { onClick() }
            setPadding(12, 12, 12, 12)
        }
        return btn
    }

    private fun selectCategory(c: ToolCategory) {
        val ts = toolState ?: return
        ts.category = c
        rebuildSub(c)
    }

    fun rebuildSub(c: ToolCategory) {
        subRow.removeAllViews()
        val ts = toolState ?: return
        when (c) {
            ToolCategory.TERRAIN -> {
                val opts = listOf(
                    TerrainType.GRASS to "草",
                    TerrainType.DIRT to "土",
                    TerrainType.SAND to "沙",
                    TerrainType.WATER to "水",
                    TerrainType.MOUNTAIN to "山",
                    TerrainType.TREE to "树"
                )
                for ((t, label) in opts) {
                    val v = makeChip(label, ts.selectedTerrain == t) {
                        ts.selectedTerrain = t
                        rebuildSub(c)
                    }
                    subRow.addView(v)
                }
            }
            ToolCategory.ENTITY -> {
                val opts = listOf(
                    EntityType.HUMAN to "人",
                    EntityType.SHEEP to "羊",
                    EntityType.WOLF to "狼",
                    EntityType.FISH to "鱼"
                )
                for ((t, label) in opts) {
                    val v = makeChip(label, ts.selectedEntity == t) {
                        ts.selectedEntity = t
                        rebuildSub(c)
                    }
                    subRow.addView(v)
                }
            }
            ToolCategory.DISASTER -> {
                val opts = listOf(
                    DisasterKind.METEOR to "陨石",
                    DisasterKind.FLOOD to "洪水",
                    DisasterKind.VOLCANO to "火山",
                    DisasterKind.HOLY to "圣光",
                    DisasterKind.LIGHTNING to "闪电"
                )
                for ((t, label) in opts) {
                    val v = makeChip(label, ts.selectedDisaster == t) {
                        ts.selectedDisaster = t
                        rebuildSub(c)
                    }
                    subRow.addView(v)
                }
            }
            else -> { /* no sub */ }
        }
    }

    private fun makeChip(label: String, selected: Boolean, onClick: () -> Unit): View {
        val tv = android.widget.TextView(context).apply {
            text = label
            setTextColor(if (selected) Color.BLACK else Color.WHITE)
            setPadding(20, 10, 20, 10)
            background = GradientDrawable().apply {
                cornerRadius = 60f
                setColor(if (selected) Color.rgb(255, 215, 64) else Color.argb(160, 60, 60, 90))
            }
            setOnClickListener { onClick() }
            textSize = 14f
            isClickable = true
        }
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.setMargins(6, 4, 6, 4)
        tv.layoutParams = lp
        return tv
    }
}