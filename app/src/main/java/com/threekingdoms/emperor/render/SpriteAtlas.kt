package com.threekingdoms.emperor.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

/**
 * 程序生成 sprite 缓存。
 * 简化版：每个 key 对应一张预生成 Bitmap。
 */
object SpriteAtlas {

    private val cache = HashMap<String, Bitmap>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val path = Path()

    fun get(key: String, size: Int = 64): Bitmap {
        return cache.getOrPut(key) { createSprite(key, size) }
    }

    private fun createSprite(key: String, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        when {
            key.startsWith("emperor_") -> drawEmperor(c, key.removePrefix("emperor_"), size)
            key.startsWith("minister_") -> drawMinisterIcon(c, size)
            key.startsWith("prefecture_") -> drawPrefecture(c, size)
            key == "battle_bg" -> drawBattleBg(c, size)
            key.startsWith("consort_") -> drawConsort(c, size)
            else -> drawGeneric(c, size, Color.GRAY, "?")
        }
        return bmp
    }

    private fun drawEmperor(c: Canvas, scenarioId: String, size: Int) {
        // 简化：圆脸 + 龙袍
        val cx = size / 2f; val cy = size / 2f
        // 背景
        paint.color = Color.rgb(180, 30, 30)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        // 王冠
        paint.color = Color.rgb(200, 160, 60)
        c.drawRect(cx - 14, 8f, cx + 14, 18f, paint)
        for (i in 0..2) {
            c.drawCircle(cx - 10 + i * 10f, 6f, 3f, paint)
        }
        // 头
        paint.color = Color.rgb(255, 220, 180)
        c.drawCircle(cx, cy - 4, 12f, paint)
        // 眉毛
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        c.drawLine(cx - 6, cy - 6, cx - 2, cy - 5, paint)
        c.drawLine(cx + 2, cy - 5, cx + 6, cy - 6, paint)
        // 眼睛
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        c.drawCircle(cx - 4, cy - 2, 1.5f, paint)
        c.drawCircle(cx + 4, cy - 2, 1.5f, paint)
        // 嘴
        paint.color = Color.rgb(150, 60, 60)
        c.drawRect(cx - 3, cy + 4, cx + 3, cy + 5, paint)
        // 须（曹操/刘备不同风格——简化所有都是黑须）
        if (scenarioId in listOf("caocao", "liubei", "hejin")) {
            paint.color = Color.BLACK
            c.drawRect(cx - 8, cy + 2, cx - 4, cy + 14, paint)
            c.drawRect(cx + 4, cy + 2, cx + 8, cy + 14, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawMinisterIcon(c: Canvas, size: Int) {
        val cx = size / 2f; val cy = size / 2f
        paint.color = Color.rgb(40, 40, 60)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.color = Color.rgb(255, 220, 180)
        c.drawCircle(cx, cy - 4, 10f, paint)
        paint.color = Color.BLACK
        c.drawCircle(cx - 3, cy - 4, 1.2f, paint)
        c.drawCircle(cx + 3, cy - 4, 1.2f, paint)
        // 帽子
        paint.color = Color.rgb(120, 60, 30)
        c.drawRect(cx - 10, 6f, cx + 10, 12f, paint)
        // 衣服
        paint.color = Color.rgb(60, 100, 160)
        path.reset()
        path.moveTo(cx - 14, size.toFloat())
        path.lineTo(cx - 10, cy + 8)
        path.lineTo(cx + 10, cy + 8)
        path.lineTo(cx + 14, size.toFloat())
        path.close()
        c.drawPath(path, paint)
    }

    private fun drawPrefecture(c: Canvas, size: Int) {
        val cx = size / 2f; val cy = size / 2f
        val w = size / 2f; val h = size / 4f
        // 菱形
        path.reset()
        path.moveTo(cx - w, cy)
        path.lineTo(cx, cy - h)
        path.lineTo(cx + w, cy)
        path.lineTo(cx, cy + h)
        path.close()
        paint.color = Color.rgb(180, 140, 90)
        c.drawPath(path, paint)
        paint.color = Color.rgb(120, 90, 50)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        c.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        // 城池 icon
        paint.color = Color.rgb(200, 50, 50)
        c.drawRect(cx - 4, cy - 8, cx + 4, cy + 4, paint)
    }

    private fun drawBattleBg(c: Canvas, size: Int) {
        // 远山
        paint.color = Color.rgb(80, 110, 160)
        c.drawRect(0f, 0f, size.toFloat(), size * 0.4f, paint)
        // 平原
        paint.color = Color.rgb(120, 130, 80)
        c.drawRect(0f, size * 0.4f, size.toFloat(), size.toFloat(), paint)
        // 战旗
        paint.color = Color.rgb(180, 30, 30)
        c.drawRect(size * 0.4f, size * 0.2f, size * 0.45f, size * 0.7f, paint)
        paint.color = Color.rgb(60, 60, 60)
        c.drawRect(size * 0.43f, size * 0.7f, size * 0.46f, size * 0.85f, paint)
    }

    private fun drawConsort(c: Canvas, size: Int) {
        val cx = size / 2f; val cy = size / 2f
        paint.color = Color.rgb(245, 230, 200)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        // 头
        paint.color = Color.rgb(255, 230, 200)
        c.drawCircle(cx, cy - 4, 11f, paint)
        // 头发
        paint.color = Color.BLACK
        c.drawRect(cx - 11, cy - 12, cx + 11, cy - 6, paint)
        // 簪子
        paint.color = Color.rgb(200, 160, 60)
        c.drawCircle(cx, cy - 14, 2f, paint)
        // 眼睛
        paint.color = Color.BLACK
        c.drawCircle(cx - 3, cy - 3, 1.2f, paint)
        c.drawCircle(cx + 3, cy - 3, 1.2f, paint)
        // 嘴
        paint.color = Color.rgb(220, 100, 100)
        c.drawRect(cx - 2, cy + 4, cx + 2, cy + 5, paint)
        // 腮红
        paint.color = Color.argb(100, 240, 150, 150)
        c.drawCircle(cx - 6, cy + 1, 2f, paint)
        c.drawCircle(cx + 6, cy + 1, 2f, paint)
    }

    private fun drawGeneric(c: Canvas, size: Int, color: Int, text: String) {
        paint.color = color
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.color = Color.WHITE
        paint.textSize = (size * 0.5f).coerceAtLeast(12f)
        c.drawText(text, size * 0.3f, size * 0.65f, paint)
    }
}