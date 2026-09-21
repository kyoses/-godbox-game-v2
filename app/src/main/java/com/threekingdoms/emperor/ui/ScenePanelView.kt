package com.threekingdoms.emperor.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.threekingdoms.emperor.render.SpriteAtlas

/**
 * 中央场景面板：绘制当前场景的皇帝立绘 + 装饰。
 */
class ScenePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var scenarioId: String = ""

    fun setScenarioId(id: String) {
        scenarioId = id
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // 背景：朱红宫墙
        paint.color = Color.rgb(80, 30, 30)
        canvas.drawRect(0f, 0f, w, h, paint)

        // 顶部房梁
        paint.color = Color.rgb(60, 30, 20)
        canvas.drawRect(0f, 0f, w, 30f, paint)
        paint.color = Color.rgb(180, 140, 60)
        canvas.drawRect(0f, 28f, w, 32f, paint)

        // 底部台阶
        for (i in 0..3) {
            paint.color = Color.rgb(100 - i * 10, 80 - i * 8, 60 - i * 6)
            canvas.drawRect(0f, h - 20f * (i + 1), w, h - 20f * i, paint)
        }

        // 皇帝立绘
        val sprite = SpriteAtlas.get("emperor_$scenarioId", 200)
        val left = (w - sprite.width) / 2f
        val top = (h - sprite.height) / 2f - 20f
        canvas.drawBitmap(sprite, left, top, paint)

        // 王座柱子
        paint.color = Color.rgb(120, 60, 30)
        canvas.drawRect(20f, 40f, 36f, h - 60f, paint)
        canvas.drawRect(w - 36f, 40f, w - 20f, h - 60f, paint)
        paint.color = Color.rgb(200, 160, 60)
        canvas.drawRect(20f, 40f, 36f, 50f, paint)
        canvas.drawRect(w - 36f, 40f, w - 20f, 50f, paint)
    }
}