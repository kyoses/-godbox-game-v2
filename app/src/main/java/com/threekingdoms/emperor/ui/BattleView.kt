package com.threekingdoms.emperor.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.threekingdoms.emperor.data.Prefecture
import com.threekingdoms.emperor.render.SpriteAtlas

/**
 * 战斗场景背景图。
 */
class BattleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var target: Prefecture? = null

    fun setTarget(p: Prefecture) {
        target = p
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        val bg = SpriteAtlas.get("battle_bg", 256)
        canvas.drawBitmap(bg, (w - bg.width) / 2, (h - bg.height) / 2, paint)

        // 目标旗
        paint.color = Color.rgb(60, 80, 180)
        canvas.drawRect(w * 0.6f, h * 0.3f, w * 0.62f, h * 0.7f, paint)
        paint.color = Color.rgb(120, 60, 60)
        canvas.drawCircle(w * 0.61f, h * 0.45f, 4f, paint)
    }
}