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

    // 缓存绘制参数供 onTouchEvent 使用
    private var originX: Float = 0f
    private var originY: Float = 0f
    private var minGridX: Int = 0
    private var minGridY: Int = 0

    fun setPrefectures(list: List<Prefecture>) {
        prefectures = list
        invalidate()
    }

    fun setPlayerForce(force: String) {
        playerForce = force
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeOrigin()
    }

    private fun computeOrigin() {
        if (prefectures.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        // 留出底部图例 + 顶部标题的空间
        val reservedBottom = 80f
        val reservedTop = 60f
        val usableW = w * 0.9f
        val usableH = h - reservedBottom - reservedTop

        val minX = prefectures.minOf { it.gridX }
        val maxX = prefectures.maxOf { it.gridX }
        val minY = prefectures.minOf { it.gridY }
        val maxY = prefectures.maxOf { it.gridY }

        val rangeX = (maxX - minX)
        val rangeY = (maxY - minY)
        val rangeSum = rangeX + rangeY

        // tileWidth 由可用宽度 / X范围决定
        val tileW = usableW / (rangeX + 1)
        // tileHeight 由可用高度 / Y范围决定
        val tileH = usableH / (rangeSum + 2)
        currentTileWidth = tileW.coerceIn(40f, 100f)
        currentTileHeight = tileH.coerceIn(20f, 50f)

        minGridX = minX
        minGridY = minY

        // 让地图中心对齐屏幕中心
        // 地图"宽度" = (rangeX + 1) * tileW
        // 地图"高度" = (rangeSum + 1) * tileH
        originX = (w - (rangeX + 1) * currentTileWidth) / 2f + currentTileWidth / 2f
        originY = reservedTop + (usableH - (rangeSum + 1) * currentTileHeight) / 2f + currentTileHeight / 2f
    }

    private var currentTileWidth: Float = 56f
    private var currentTileHeight: Float = 28f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        paint.color = Color.rgb(20, 30, 45)
        canvas.drawRect(0f, 0f, w, h, paint)

        if (prefectures.isEmpty()) return

        if (originX == 0f && originY == 0f) computeOrigin()

        // 标题
        paint.color = Color.rgb(200, 160, 60)
        paint.textSize = 20f
        canvas.drawText("天下九州 · 点击郡县开战", 16f, 30f, paint)

        // 按 y+x 排序绘制
        val sorted = prefectures.sortedBy { it.gridX + it.gridY }
        for (p in sorted) {
            val (sx, sy) = Projection.iso(p.gridX - minGridX, p.gridY - minGridY,
                currentTileWidth, currentTileHeight, originX, originY)
            drawPrefectureTile(canvas, p, sx, sy)
            drawLabel(canvas, p.name, sx, sy)
        }

        // 图例
        drawLegend(canvas, w, h)
    }

    private fun drawPrefectureTile(canvas: Canvas, p: Prefecture, cx: Float, cy: Float) {
        val w = currentTileWidth / 2f
        val h = currentTileHeight / 2f
        val color = when {
            p.ownerForce == playerForce -> Color.rgb(220, 100, 80)
            p.ownerForce == "汉" -> Color.rgb(120, 150, 100)
            else -> Color.rgb(160, 120, 80)
        }

        path.reset()
        path.moveTo(cx - w, cy)
        path.lineTo(cx, cy - h)
        path.lineTo(cx + w, cy)
        path.lineTo(cx, cy + h)
        path.close()
        paint.color = color
        canvas.drawPath(path, paint)

        paint.color = Color.rgb(60, 50, 40)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(180, 30, 30)
        canvas.drawRect(cx - 5, cy - 5, cx + 5, cy + 5, paint)
    }

    private fun drawLabel(canvas: Canvas, name: String, cx: Float, cy: Float) {
        paint.color = Color.WHITE
        paint.textSize = 11f
        val textWidth = paint.measureText(name)
        canvas.drawText(name, cx - textWidth / 2, cy + 22, paint)
    }

    private fun drawLegend(canvas: Canvas, w: Float, h: Float) {
        val legendY = h - 40f
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
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val hit = findPrefecture(event.x, event.y)
            if (hit != null) {
                listener?.onPrefectureClick(hit)
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findPrefecture(sx: Float, sy: Float): Prefecture? {
        var best: Prefecture? = null
        var bestDist = Float.MAX_VALUE
        for (p in prefectures) {
            val (cx, cy) = Projection.iso(p.gridX - minGridX, p.gridY - minGridY,
                currentTileWidth, currentTileHeight, originX, originY)
            val dx = abs(sx - cx)
            val dy = abs(sy - cy)
            // 菱形"内部"判定：x 距离 + 2y 距离 < tileWidth
            val dist = dx + 2 * dy
            if (dist < bestDist) {
                bestDist = dist
                best = p
            }
        }
        // tileWidth 命中半径
        return if (best != null && bestDist < currentTileWidth * 1.2f) best else null
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}