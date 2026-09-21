package com.godbox.game.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.godbox.game.engine.WeatherState
import com.godbox.game.engine.World

/**
 * HUD：圆角面板（左上角）显示游戏状态。
 */
class HUDRenderer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        isFakeBoldText = false
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        isFakeBoldText = true
        color = Color.rgb(255, 215, 64)
    }

    fun render(canvas: Canvas, world: World, tool: String, godHandActive: Boolean) {
        val pad = 16f
        val lineHeight = 32f
        val panelX = pad
        val panelY = pad
        val panelW = 260f
        val lines = listOf(
            "⛪ 上帝模拟器",
            "──────────",
            "👥 人口 ${world.population()}",
            "🐑 生物 ${world.entities.size}",
            "🏛 部落 ${world.civilizations.size}",
            "${weatherIcon(world.weather.state)} 天气",
            "🖐 工具 $tool",
            if (godHandActive) "👁 上帝之手" else null,
            if (world.paused) "⏸ 暂停" else null
        ).filterNotNull()

        val panelH = lineHeight * lines.size + pad * 2 + 8f

        // 面板背景
        val bgColor = if (world.paused) Color.argb(220, 180, 30, 30)
            else Color.argb(200, 26, 35, 126)
        VoxelModels.drawRoundRect(canvas,
            panelX, panelY, panelX + panelW, panelY + panelH,
            20f, bgColor)

        // 描边
        paint.color = Color.argb(120, 255, 215, 64)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(panelX, panelY, panelX + panelW, panelY + panelH,
            20f, 20f, paint)
        paint.style = Paint.Style.FILL

        // 文本
        var y = panelY + lineHeight + 4f
        for ((idx, line) in lines.withIndex()) {
            if (idx == 0) {
                canvas.drawText(line, panelX + 16f, y, titlePaint)
            } else {
                paint.color = Color.WHITE
                paint.textSize = 24f
                canvas.drawText(line, panelX + 16f, y, paint)
            }
            y += lineHeight
        }

        // 右侧：操作提示
        val hintX = 20f
        val hintY = canvas.height - 220f
        VoxelModels.drawRoundRect(canvas, hintX, hintY,
            hintX + 280f, hintY + 200f, 12f, Color.argb(200, 0, 0, 0))
        paint.color = Color.argb(120, 255, 215, 64)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(hintX, hintY, hintX + 280f, hintY + 200f,
            12f, 12f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 18f
        val tips = listOf(
            "操作：",
            "• 单击 = 放置 / 触发",
            "• 拖动 = 平移视角",
            "• 双指 = 缩放",
            "• 长按 = 上帝之手",
            "• 双击 = 退出上帝之手"
        )
        var ty = hintY + 28f
        for (tip in tips) {
            canvas.drawText(tip, hintX + 10f, ty, paint)
            ty += 24f
        }
    }

    private fun weatherIcon(state: WeatherState): String = when (state) {
        WeatherState.SUNNY -> "☀ 晴"
        WeatherState.RAINING -> "🌧 雨"
        WeatherState.STORM -> "⛈ 雷"
    }
}