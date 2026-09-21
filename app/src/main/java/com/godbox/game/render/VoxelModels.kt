package com.godbox.game.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

/**
 * 公用方块 / 生物模型绘制。所有方法接收 Canvas + 屏幕原点 (ox, oy) + 中心点位置。
 *
 * 坐标系：每个方块以"中心点 (cx, cy)"为参考。"宽"和"高"按屏幕像素。
 */
object VoxelModels {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val path = Path()

    /** 画一个轴测 3D 方块：前面 + 顶面 + 右侧面，中心在 (cx, cy)，halfSize 单边半像素。 */
    fun drawIsoBlock(canvas: Canvas, cx: Float, cy: Float, halfSize: Float,
                     topColor: Int, frontColor: Int, rightColor: Int) {
        // 菱形菱宽 2*halfSize，菱高 2*halfSize
        // 顶面 4 点：(0, -h), (w, 0), (0, h), (-w, 0)
        val w = halfSize
        val h = halfSize * 0.5f

        // 前面（从中心向下画一个矩形 → 等距菱形 底 = (0, h)、左 = (-w, 0)、右 = (w, 0)、底尖 (0, h+blockHeight)）
        // 简化：前面是一个四边形
        val blockH = halfSize  // 立方体高度

        // 前面（朝向屏幕左下）
        path.reset()
        path.moveTo(cx - w, cy + h)
        path.lineTo(cx, cy + h + blockH)
        path.lineTo(cx + w, cy + h)
        path.lineTo(cx, cy)
        path.close()
        fillPaint.color = frontColor
        canvas.drawPath(path, fillPaint)

        // 右侧面（朝右下）
        path.reset()
        path.moveTo(cx, cy + h + blockH)
        path.lineTo(cx + w, cy + h)
        path.lineTo(cx + w, cy - h)
        path.lineTo(cx, cy)
        path.close()
        fillPaint.color = rightColor
        canvas.drawPath(path, fillPaint)

        // 顶面（朝上）
        path.reset()
        path.moveTo(cx - w, cy + h)
        path.lineTo(cx, cy)
        path.lineTo(cx + w, cy - h)
        path.lineTo(cx, cy - 2 * h)
        path.close()
        fillPaint.color = topColor
        canvas.drawPath(path, fillPaint)
    }

    /** 画一个简化的"扁平"菱形格子（仅顶面，无高度），用于 WATER 之类的平面地形 */
    fun drawIsoTile(canvas: Canvas, cx: Float, cy: Float, halfSize: Float, color: Int) {
        val w = halfSize
        val h = halfSize * 0.5f
        path.reset()
        path.moveTo(cx - w, cy + h)
        path.lineTo(cx, cy)
        path.lineTo(cx + w, cy - h)
        path.lineTo(cx, cy - 2 * h)
        path.close()
        fillPaint.color = color
        canvas.drawPath(path, fillPaint)
    }

    /** 画一个矩形填充（屏幕空间，不参与透视）。用于 HUD/UI。 */
    fun drawRect(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        fillPaint.color = color
        canvas.drawRect(left, top, right, bottom, fillPaint)
    }

    /** 画一个圆角矩形填充 */
    fun drawRoundRect(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float,
                       cornerRadius: Float, color: Int) {
        fillPaint.color = color
        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, fillPaint)
    }

    /** 画文字 */
    fun drawText(canvas: Canvas, text: String, x: Float, y: Float, sizeSp: Float, color: Int) {
        fillPaint.color = color
        fillPaint.textSize = sizeSp
        canvas.drawText(text, x, y, fillPaint)
    }

    /** 把 RGB 变亮 factor∈[0,1] */
    fun brighten(color: Int, factor: Float): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    fun darken(color: Int, factor: Float): Int {
        val r = (Color.red(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1f - factor)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    /** 当前 paint（用于外部更复杂的复合绘制）。注意：复用对象，非线程安全。 */
    fun paint(): Paint = fillPaint

    fun pathBuffer(): Path = path

    fun strokePaint(): Paint = strokePaint
}