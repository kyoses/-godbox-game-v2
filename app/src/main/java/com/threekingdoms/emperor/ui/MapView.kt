package com.threekingdoms.emperor.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.threekingdoms.emperor.data.Prefecture
import com.threekingdoms.emperor.render.Projection
import kotlin.math.abs

/**
 * 三国州郡地图：等距投影，菱形地块，点击命中弹回调。
 */
class MapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface OnPrefectureClickListener {
        fun onPrefectureClick(prefecture: Prefecture)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private var prefectures: List<Prefecture> = emptyList()
    private var playerForce: String = ""
    var listener: OnPrefectureClickListener? = null

    private val tileWidth = 56f
    private val tileHeight = 28f

    fun setPrefectures(list: List<Prefecture>) {
        prefectures = list
        invalidate()
    }

    fun setPlayerForce(force: String) {
        playerForce = force
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // 背景
        paint.color = Color.rgb(20, 30, 45)
        canvas.drawRect(0f, 0f, w, h, paint)

        if (prefectures.isEmpty()) return

        // 计算地图范围
        val minX = prefectures.minOf { it.gridX }
        val maxX = prefectures.maxOf { it.gridX }
        val minY = prefectures.minOf { it.gridY }
        val maxY = prefectures.maxOf { it.gridY }

        val originX = (w - (maxX - minX) * tileWidth) / 2f + w * 0.05f
        val originY = (h - (maxX - minX + maxY - minY) * tileHeight) / 2f + h * 0.1f

        // 按 y+x 排序绘制（远处先画）
        val sorted = prefectures.sortedBy { it.gridX + it.gridY }
        for (p in sorted) {
            val (sx, sy) = Projection.iso(p.gridX - minX, p.gridY - minY,
                tileWidth, tileHeight, originX, originY)
            drawPrefectureTile(canvas, p, sx, sy)
            drawLabel(canvas, p.name, sx, sy)
        }

        // 图例
        drawLegend(canvas, w, h)
    }

    private fun drawPrefectureTile(canvas: Canvas, p: Prefecture, cx: Float, cy: Float) {
        val w = tileWidth / 2f
        val h = tileHeight / 2f
        val color = when {
            p.ownerForce == playerForce -> Color.rgb(220, 100, 80)   // 玩家红色
            p.ownerForce == "汉" -> Color.rgb(120, 150, 100)         // 中立绿
            else -> Color.rgb(160, 120, 80)                            // 其他势力棕
        }

        path.reset()
        path.moveTo(cx - w, cy)
        path.lineTo(cx, cy - h)
        path.lineTo(cx + w, cy)
        path.lineTo(cx, cy + h)
        path.close()
        paint.color = color
        canvas.drawPath(path, paint)

        // 描边
        paint.color = Color.rgb(60, 50, 40)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL

        // 城池 icon（小红块）
        paint.color = Color.rgb(180, 30, 30)
        canvas.drawRect(cx - 4, cy - 4, cx + 4, cy + 4, paint)
    }

    private fun drawLabel(canvas: Canvas, name: String, cx: Float, cy: Float) {
        paint.color = Color.WHITE
        paint.textSize = 11f
        val w = paint.measureText(name)
        canvas.drawText(name, cx - w / 2, cy + 22, paint)
    }

    private fun drawLegend(canvas: Canvas, w: Float, h: Float) {
        val legendY = h - 60
        val items = listOf(
            Color.rgb(220, 100, 80) to "我方",
            Color.rgb(120, 150, 100) to "汉室",
            Color.rgb(160, 120, 80) to "敌对"
        )
        paint.textSize = 14f
        var x = 16f
        for ((color, label) in items) {
            paint.color = color
            canvas.drawRect(x, legendY, x + 20, legendY + 20, paint)
            paint.color = Color.WHITE
            canvas.drawText(label, x + 24, legendY + 16, paint)
            x += 100
        }
        // 提示文字
        paint.textSize = 14f
        canvas.drawText("点击敌方州郡开战", 16f, 30f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val w = width.toFloat()
            val h = height.toFloat()
            val minX = prefectures.minOf { it.gridX }
            val maxX = prefectures.maxOf { it.gridX }
            val originX = (w - (maxX - minX) * tileWidth) / 2f + w * 0.05f
            val originY = (h - (maxX - minX + prefectures.maxOf { it.gridY } - prefectures.minOf { it.gridY }) * tileHeight) / 2f + h * 0.1f

            // 找最近格
            var best: Prefecture? = null
            var bestDist = Float.MAX_VALUE
            for (p in prefectures) {
                val (sx, sy) = Projection.iso(p.gridX - minX, p.gridY - prefectures.minOf { it.gridY },
                    tileWidth, tileHeight, originX, originY)
                val dx = event.x - sx
                val dy = event.y - sy
                val dist = abs(dx) + abs(dy)
                if (dist < bestDist) {
                    bestDist = dist
                    best = p
                }
            }

            if (best != null && bestDist < tileWidth * 0.8f) {
                listener?.onPrefectureClick(best)
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}