package com.threekingdoms.emperor.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.threekingdoms.emperor.data.Prefecture
import com.threekingdoms.emperor.render.Projection
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 三国州郡地图 v2：
 * - 伪 3D 立体菱形（顶面 + 高度边框）
 * - 双指缩放（0.5x ~ 4x）
 * - 单指拖动平移
 * - 双击放大 / 单击选中（弹父 Activity 回调）
 * - 点击闪烁高亮
 */
class MapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface OnPrefectureClickListener {
        fun onPrefectureClick(prefecture: Prefecture)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        isFakeBoldText = true
        color = Color.WHITE
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        isFakeBoldText = true
        color = Color.rgb(220, 170, 60)
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }
    private val path = Path()

    private var prefectures: List<Prefecture> = emptyList()
    private var playerForce: String = ""
    var listener: OnPrefectureClickListener? = null

    // 平移和缩放
    private var scale: Float = 1.0f
        set(value) { field = value.coerceIn(0.5f, 4.0f) }
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f

    // 地图原始尺寸（基于 grid）
    private var baseOriginX: Float = 0f
    private var baseOriginY: Float = 0f
    private var baseTileWidth: Float = 56f
    private var baseTileHeight: Float = 28f
    private var baseHeight: Float = 14f  // 3D 高度
    private var minGridX: Int = 0
    private var minGridY: Int = 0
    private var rangeX: Int = 0
    private var rangeY: Int = 0

    // 选中的郡
    private var selectedIndex: Int = -1
    private var flashStartTime: Long = 0L

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale *= detector.scaleFactor
            clampOffsets()
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (scaleDetector.isInProgress) return true
            offsetX -= dx
            offsetY -= dy
            clampOffsets()
            invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val hit = findPrefectureAt(e.x, e.y)
            if (hit != null) {
                selectedIndex = prefectures.indexOf(hit)
                flashStartTime = System.currentTimeMillis()
                invalidate()
                listener?.onPrefectureClick(hit)
                performClick()
                return true
            }
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            // 双击：以点击位置为中心放大
            val oldScale = scale
            scale = (scale * 1.6f).coerceAtMost(4.0f)
            val factor = scale / oldScale
            // 调整 offset 让点击位置不动
            offsetX = e.x - (e.x - offsetX) * factor
            offsetY = e.y - (e.y - offsetY) * factor
            clampOffsets()
            invalidate()
            return true
        }
    })

    fun setPrefectures(list: List<Prefecture>) {
        prefectures = list
        if (list.isNotEmpty()) computeBaseLayout()
        invalidate()
    }

    fun setPlayerForce(force: String) {
        playerForce = force
        invalidate()
    }

    /** 重置视图（缩放/平移回到初始） */
    fun resetView() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeBaseLayout()
        centerView()
    }

    private fun computeBaseLayout() {
        if (prefectures.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        minGridX = prefectures.minOf { it.gridX }
        minGridY = prefectures.minOf { it.gridY }
        val maxX = prefectures.maxOf { it.gridX }
        val maxY = prefectures.maxOf { it.gridY }
        rangeX = maxX - minGridX
        rangeY = maxY - minGridY

        // 留出顶部标题、底部图例 + 缩放按钮空间
        val reservedTop = 80f
        val reservedBottom = 140f
        val usableW = w
        val usableH = h - reservedTop - reservedBottom

        // tileWidth 让整张地图水平撑满
        baseTileWidth = usableW / (rangeX + 1.5f)
        baseTileHeight = baseTileWidth * 0.5f
        baseHeight = baseTileWidth * 0.25f

        // origin 让 (0,0) 落在 (reservedTop + baseTileHeight/2)
        baseOriginX = baseTileWidth / 2f
        baseOriginY = reservedTop + baseTileHeight / 2f
    }

    private fun centerView() {
        // 把地图中心对齐屏幕中心
        val w = width.toFloat()
        val h = height.toFloat()
        val mapCenterX = (rangeX / 2f) * baseTileWidth / 2f + baseOriginX
        val mapCenterY = (rangeX / 2f + rangeY / 2f) * baseTileHeight / 2f + baseOriginY
        offsetX = w / 2f - mapCenterX
        offsetY = h / 2f - mapCenterY
    }

    private fun clampOffsets() {
        val w = width.toFloat()
        val h = height.toFloat()
        val mapW = (rangeX + 1) * baseTileWidth + baseTileWidth
        val mapH = (rangeX + rangeY + 1) * baseTileHeight
        val maxOffX = max(0f, (mapW * scale - w) / 2f) + 200f
        val maxOffY = max(0f, (mapH * scale - h) / 2f) + 200f
        offsetX = offsetX.coerceIn(-maxOffX, maxOffX)
        offsetY = offsetY.coerceIn(-maxOffY, maxOffY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // 背景：墨黑 + 暗纹
        fillPaint.color = Color.rgb(15, 22, 35)
        canvas.drawRect(0f, 0f, w, h, fillPaint)

        if (prefectures.isEmpty()) return

        if (baseOriginX == 0f && baseOriginY == 0f) computeBaseLayout()

        // 标题
        canvas.drawText("天下九州 · 三国战局", 24f, 50f, titlePaint)
        textPaint.textSize = 18f
        canvas.drawText("双指缩放 · 单击开战 · 双击放大", 24f, 78f, textPaint.apply {
            color = Color.rgb(180, 160, 100)
            isFakeBoldText = false
        })

        // 绘制地形背景（地形层，让画面不单调）
        drawTerrainBackground(canvas)

        // 缩放应用
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale, offsetX + w / 2f, offsetY + h / 2f)

        val sorted = prefectures.sortedBy { it.gridX + it.gridY }
        for ((idx, p) in sorted.withIndex()) {
            val (cx, cy) = Projection.iso(
                p.gridX - minGridX,
                p.gridY - minGridY,
                baseTileWidth, baseTileHeight,
                baseOriginX, baseOriginY
            )
            drawIsoPrefecture(canvas, p, cx, cy)
            // 闪烁选中效果
            if (idx == selectedIndex) {
                val elapsed = (System.currentTimeMillis() - flashStartTime) % 1000
                if (elapsed < 600) {
                    val alpha = (255 * (1f - elapsed / 600f)).toInt().coerceIn(0, 255)
                    drawHighlight(canvas, cx, cy, alpha)
                }
            }
        }

        canvas.restore()

        // UI 层：图例 + 缩放按钮（不缩放）
        drawLegend(canvas, w, h)
        drawZoomButtons(canvas, w, h)
    }

    private fun drawTerrainBackground(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        // 渐变背景
        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.rgb(25, 35, 50),
                Color.rgb(15, 22, 35)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, fillPaint)
        fillPaint.shader = null
    }

    private fun drawIsoPrefecture(canvas: Canvas, p: Prefecture, cx: Float, cy: Float) {
        val w = baseTileWidth / 2f
        val h = baseTileHeight / 2f
        val bh = baseHeight

        // 根据归属选择颜色
        val (topColor, leftColor, rightColor) = when {
            p.ownerForce == playerForce -> Triple(
                Color.rgb(255, 130, 100),    // 我方顶面亮
                Color.rgb(220, 80, 60),       // 前面中
                Color.rgb(160, 50, 40)        // 右面暗
            )
            p.ownerForce == "汉" -> Triple(
                Color.rgb(150, 180, 120),
                Color.rgb(110, 140, 90),
                Color.rgb(70, 95, 60)
            )
            else -> Triple(
                Color.rgb(200, 150, 90),
                Color.rgb(150, 110, 60),
                Color.rgb(100, 70, 40)
            )
        }

        // 阴影（在格下方画一个椭圆）
        fillPaint.color = Color.argb(100, 0, 0, 0)
        canvas.drawOval(
            cx - w * 0.7f, cy + h * 0.5f,
            cx + w * 0.7f, cy + h * 0.9f,
            fillPaint
        )

        // 右侧面（暗）
        path.reset()
        path.moveTo(cx, cy - h)
        path.lineTo(cx + w, cy)
        path.lineTo(cx + w, cy + h * 0.6f)
        path.lineTo(cx, cy + h * 0.6f + bh)
        path.close()
        fillPaint.color = rightColor
        canvas.drawPath(path, fillPaint)
        // 右侧面纹理
        drawFaceTexture(canvas, path, rightColor, true)

        // 前面（中）
        path.reset()
        path.moveTo(cx - w, cy)
        path.lineTo(cx, cy + h)
        path.lineTo(cx, cy + h + bh)
        path.lineTo(cx - w, cy + h * 0.6f)
        path.close()
        fillPaint.color = leftColor
        canvas.drawPath(path, fillPaint)
        drawFaceTexture(canvas, path, leftColor, false)

        // 顶面（亮）
        path.reset()
        path.moveTo(cx - w, cy)
        path.lineTo(cx, cy - h)
        path.lineTo(cx + w, cy)
        path.lineTo(cx, cy + h)
        path.close()
        fillPaint.color = topColor
        canvas.drawPath(path, fillPaint)

        // 顶面描边（深色勾边）
        strokePaint.color = Color.rgb(40, 30, 20)
        strokePaint.strokeWidth = 2f
        canvas.drawPath(path, strokePaint)

        // 顶面纹理：根据地形类型画不同图案
        drawTopTexture(canvas, p, cx, cy, w, h)

        // 城池 icon（小朱红方块，立体感）
        drawCastleIcon(canvas, cx, cy - h * 0.3f)

        // 地名（在前面下方）
        textPaint.textSize = 16f
        textPaint.color = Color.WHITE
        val name = p.name
        val nameWidth = textPaint.measureText(name)
        canvas.drawText(name, cx - nameWidth / 2, cy + h + bh - 6f, textPaint)

        // 兵力数字（下方）
        textPaint.textSize = 11f
        textPaint.color = Color.argb(200, 255, 230, 200)
        val troopsText = "${p.troops / 1000}k"
        val troopsWidth = textPaint.measureText(troopsText)
        canvas.drawText(troopsText, cx - troopsWidth / 2, cy + h + bh + 12f, textPaint)
    }

    private fun drawFaceTexture(canvas: Canvas, path: Path, baseColor: Int, isRight: Boolean) {
        // 在前面/右侧面画几道横线（仿石砖）
        strokePaint.strokeWidth = 1f
        strokePaint.color = Color.argb(60, 0, 0, 0)
        val bounds = android.graphics.RectF()
        path.computeBounds(bounds, true)
        for (i in 1..3) {
            val ratio = i / 4f
            if (isRight) {
                canvas.drawLine(
                    bounds.right - (bounds.right - bounds.left) * ratio,
                    bounds.top, bounds.right,
                    bounds.top + (bounds.bottom - bounds.top) * 0.6f,
                    strokePaint
                )
            } else {
                canvas.drawLine(
                    bounds.left, bounds.top + (bounds.bottom - bounds.top) * 0.6f * ratio,
                    bounds.left + (bounds.right - bounds.left) * 0.7f,
                    bounds.top + (bounds.bottom - bounds.top) * 0.6f,
                    strokePaint
                )
            }
        }
    }

    private fun drawTopTexture(canvas: Canvas, p: Prefecture, cx: Float, cy: Float, w: Float, h: Float) {
        // 根据名称画不同图标
        strokePaint.color = Color.argb(180, 0, 0, 0)
        strokePaint.strokeWidth = 2f
        when {
            p.name.contains("关") -> {
                // 关隘：画两道竖线
                canvas.drawLine(cx - 6, cy - 3, cx - 6, cy + 6, strokePaint)
                canvas.drawLine(cx + 6, cy - 3, cx + 6, cy + 6, strokePaint)
            }
            p.id.startsWith("han") || p.id.contains("han") -> {
                // 河流/山：随机点
                for (i in 0..5) {
                    val px = cx + (i - 2) * 5f
                    canvas.drawLine(px, cy - 2, px + 2, cy + 4, strokePaint)
                }
            }
            else -> {
                // 一般城市：画城墙纹
                strokePaint.color = Color.argb(150, 60, 40, 30)
                for (i in 0..4) {
                    val px = cx - 10 + i * 5
                    canvas.drawLine(px, cy + 1, px + 2, cy + 4, strokePaint)
                }
            }
        }
    }

    private fun drawCastleIcon(canvas: Canvas, cx: Float, cy: Float) {
        // 小朱红立体方块（城门）
        fillPaint.color = Color.rgb(180, 30, 30)
        canvas.drawRect(cx - 5, cy - 5, cx + 5, cy + 5, fillPaint)
        // 顶部高光
        fillPaint.color = Color.rgb(220, 80, 60)
        canvas.drawRect(cx - 5, cy - 5, cx + 5, cy - 2, fillPaint)
        // 城门开口
        fillPaint.color = Color.rgb(80, 30, 20)
        canvas.drawRect(cx - 2, cy, cx + 2, cy + 5, fillPaint)
    }

    private fun drawHighlight(canvas: Canvas, cx: Float, cy: Float, alpha: Int) {
        val w = baseTileWidth / 2f
        val h = baseTileHeight / 2f
        val bh = baseHeight
        strokePaint.color = Color.argb(alpha, 255, 215, 64)
        strokePaint.strokeWidth = 6f
        path.reset()
        path.moveTo(cx - w, cy)
        path.lineTo(cx, cy - h)
        path.lineTo(cx + w, cy)
        path.lineTo(cx, cy + h)
        path.close()
        canvas.drawPath(path, strokePaint)
        path.reset()
        path.moveTo(cx - w, cy + h * 0.6f)
        path.lineTo(cx, cy + h + bh)
        path.lineTo(cx + w, cy + h * 0.6f)
        path.close()
        canvas.drawPath(path, strokePaint)
    }

    private fun drawLegend(canvas: Canvas, w: Float, h: Float) {
        val legendY = h - 90f
        val items = listOf(
            Color.rgb(255, 130, 100) to "我方",
            Color.rgb(150, 180, 120) to "汉室",
            Color.rgb(200, 150, 90) to "敌对"
        )
        textPaint.textSize = 18f
        textPaint.isFakeBoldText = false
        textPaint.color = Color.WHITE
        var x = 24f
        for ((color, label) in items) {
            fillPaint.color = color
            canvas.drawRect(x, legendY, x + 26, legendY + 26, fillPaint)
            canvas.drawText(label, x + 32, legendY + 22, textPaint)
            x += 130
        }
        // 操作提示
        textPaint.color = Color.rgb(180, 160, 100)
        canvas.drawText("缩放: ${"%.1f".format(scale)}x", 24f, h - 20f, textPaint)
    }

    private fun drawZoomButtons(canvas: Canvas, w: Float, h: Float) {
        val btnSize = 60f
        val margin = 24f
        val btnX = w - btnSize - margin
        val btnY1 = h - 240f
        val btnY2 = h - 170f

        // + 按钮
        fillPaint.color = Color.argb(200, 30, 30, 40)
        canvas.drawRoundRect(btnX, btnY1, btnX + btnSize, btnY1 + btnSize, 12f, 12f, fillPaint)
        strokePaint.color = Color.rgb(220, 170, 60)
        strokePaint.strokeWidth = 2f
        canvas.drawRoundRect(btnX, btnY1, btnX + btnSize, btnY1 + btnSize, 12f, 12f, strokePaint)
        textPaint.color = Color.rgb(220, 170, 60)
        textPaint.textSize = 36f
        textPaint.isFakeBoldText = true
        canvas.drawText("+", btnX + btnSize / 2 - 10, btnY1 + btnSize / 2 + 12, textPaint)

        // - 按钮
        fillPaint.color = Color.argb(200, 30, 30, 40)
        canvas.drawRoundRect(btnX, btnY2, btnX + btnSize, btnY2 + btnSize, 12f, 12f, fillPaint)
        canvas.drawRoundRect(btnX, btnY2, btnX + btnSize, btnY2 + btnSize, 12f, 12f, strokePaint)
        canvas.drawText("-", btnX + btnSize / 2 - 8, btnY2 + btnSize / 2 + 12, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        // +/- 按钮
        if (event.action == MotionEvent.ACTION_DOWN) {
            val w = width.toFloat()
            val h = height.toFloat()
            val btnSize = 60f
            val margin = 24f
            val btnX = w - btnSize - margin
            val btnY1 = h - 240f
            val btnY2 = h - 170f
            if (event.x in btnX..btnX + btnSize) {
                if (event.y in btnY1..btnY1 + btnSize) {
                    val oldScale = scale
                    scale = (scale * 1.3f).coerceAtMost(4.0f)
                    val factor = scale / oldScale
                    offsetX = event.x - (event.x - offsetX) * factor
                    offsetY = event.y - (event.y - offsetY) * factor
                    clampOffsets()
                    invalidate()
                    return true
                } else if (event.y in btnY2..btnY2 + btnSize) {
                    val oldScale = scale
                    scale = (scale / 1.3f).coerceAtLeast(0.5f)
                    val factor = scale / oldScale
                    offsetX = event.x - (event.x - offsetX) * factor
                    offsetY = event.y - (event.y - offsetY) * factor
                    clampOffsets()
                    invalidate()
                    return true
                }
            }
        }
        return true
    }

    private fun findPrefectureAt(sx: Float, sy: Float): Prefecture? {
        // 把屏幕坐标反变换回世界坐标
        val w = width.toFloat()
        val h = height.toFloat()
        val centerX = w / 2f + offsetX
        val centerY = h / 2f + offsetY
        // 反缩放
        val dx = (sx - centerX) / scale + centerX
        val dy = (sy - centerY) / scale + centerY
        val (wx, wy) = Projection.screenToIso(
            dx, dy,
            baseTileWidth, baseTileHeight,
            baseOriginX, baseOriginY
        )
        val targetGridX = (wx.toInt() + minGridX)
        val targetGridY = (wy.toInt() + minGridY)
        // 在附近 1 格内找最近
        var best: Prefecture? = null
        var bestDist = Float.MAX_VALUE
        for (p in prefectures) {
            val gx = (p.gridX - targetGridX).toFloat()
            val gy = (p.gridY - targetGridY).toFloat()
            val d = abs(gx) + abs(gy)
            if (d < bestDist) {
                bestDist = d
                best = p
            }
        }
        return if (best != null && bestDist < 1.5f) best else null
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}