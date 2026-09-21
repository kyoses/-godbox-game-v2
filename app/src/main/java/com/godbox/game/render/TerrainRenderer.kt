package com.godbox.game.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.godbox.game.engine.Cell
import com.godbox.game.engine.TerrainType
import com.godbox.game.engine.World
import com.godbox.game.input.Camera
import kotlin.math.abs
import kotlin.math.sin

/**
 * 地形渲染器：每个地形格子画一个 3D 方块。
 *
 * 绘制顺序：按 (y + x) 升序，远处先画、近处后画，自然遮挡。
 */
class TerrainRenderer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val path = Path()
    private val time = System.currentTimeMillis()

    fun render(canvas: Canvas, world: World, camera: Camera,
               surfaceW: Int, surfaceH: Int,
               visibleMinX: Int, visibleMaxX: Int,
               visibleMinY: Int, visibleMaxY: Int) {

        val tw = camera.tileWidth()
        val th = camera.tileHeight()
        val dh = camera.projection.depthHeight * camera.scale
        val originX = surfaceW / 2f + camera.offsetX
        val originY = surfaceH / 2f + camera.offsetY

        // 计算世界中心渲染在屏幕的位置
        val centerWX = world.width / 2f
        val centerWY = world.height / 2f
        val centerSX = originX
        val centerSY = originY

        // 用 (y + x) 排序：先画远处的（左下/右下 → 内部 → 左上/右上）
        // 实际上正确的等距渲染顺序是：先画 x+y 小的，再画 x+y 大的（顶视角下"从远到近"）
        // 但在等距中"远离屏幕"对应的是 x-y 大的（右上）或 x+y 大的（左下/右下），简化用 x+y 升序
        for (sum in 0..(world.width + world.height - 2)) {
            val startY = maxOf(0, sum - (world.width - 1))
            val endY = minOf(world.height - 1, sum)
            for (y in startY..endY) {
                val x = sum - y
                if (x < 0 || x >= world.width) continue
                if (x < visibleMinX - 1 || x > visibleMaxX + 1) continue
                if (y < visibleMinY - 1 || y > visibleMaxY + 1) continue
                drawCell(canvas, world.cellAt(x, y), x, y, centerSX, centerSY, tw, th, dh)
            }
        }
    }

    private fun drawCell(canvas: Canvas, cell: Cell, x: Int, y: Int,
                        originX: Float, originY: Float,
                        tw: Float, th: Float, dh: Float) {
        // 计算格子在屏幕上的中心（z=0 平面）
        val localX = (x - y) * (tw / 2f)
        val localY = (x + y) * (th / 2f)
        val cx = originX + localX
        val cy = originY + localY

        // 阴影（地面投影）
        paint.color = Color.argb(70, 0, 0, 0)
        canvas.drawOval(
            cx - tw * 0.45f, cy + th * 0.05f,
            cx + tw * 0.45f, cy + th * 0.4f, paint
        )

        val height = cell.terrain.height + if (cell.terrain == TerrainType.MOUNTAIN)
            (cell.heightJitter * 4f).toInt().coerceAtLeast(0) else 0

        when (cell.terrain) {
            TerrainType.WATER -> drawWaterTile(canvas, cx, cy, tw, th)
            else -> {
                VoxelModels.drawIsoBlock(
                    canvas, cx, cy, tw * 0.55f,
                    cell.terrain.topColor, cell.terrain.frontColor, cell.terrain.rightColor
                )
                // 山顶雪冠
                if (cell.terrain == TerrainType.MOUNTAIN && height >= 7) {
                    VoxelModels.drawIsoBlock(
                        canvas, cx - tw * 0.05f, cy - th * 0.05f, tw * 0.4f,
                        Color.rgb(240, 240, 250), Color.rgb(220, 220, 230), Color.rgb(180, 180, 195)
                    )
                }
                // 地形附加物
                when (cell.terrain) {
                    TerrainType.TREE -> drawTree(canvas, cx, cy, tw, th)
                    TerrainType.VILLAGE -> drawHouse(canvas, cx, cy, tw, th)
                    TerrainType.MOUNTAIN -> { /* 已经画过 */ }
                    else -> { /* nothing */ }
                }
            }
        }
    }

    private fun drawWaterTile(canvas: Canvas, cx: Float, cy: Float, tw: Float, th: Float) {
        // 平面菱形
        val t = (System.currentTimeMillis() / 600) % 1000 / 1000f
        val color = VoxelModels.brighten(Color.rgb(40, 90, 180), 0.05f + 0.05f * sin(t * 6.28f))
        VoxelModels.drawIsoTile(canvas, cx, cy, tw * 0.55f, color)

        // 1-2 条波纹（弧线）
        val waveCount = 1 + ((cx.toInt() + cy.toInt()) % 2)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        for (i in 0 until waveCount) {
            val offset = (sin((t + i * 0.3f) * 6.28f) * 4f).toFloat()
            paint.color = Color.argb(160, 200, 220, 255)
            path.reset()
            path.moveTo(cx - tw * 0.3f + offset, cy + th * 0.05f)
            path.quadTo(cx + offset, cy - th * 0.1f, cx + tw * 0.3f + offset, cy + th * 0.05f)
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawTree(canvas: Canvas, cx: Float, cy: Float, tw: Float, th: Float) {
        // 树干：3D 棕色立柱
        VoxelModels.drawIsoBlock(
            canvas, cx - tw * 0.1f, cy - th * 0.05f, tw * 0.2f,
            Color.rgb(120, 80, 40),
            Color.rgb(90, 60, 30),
            Color.rgb(60, 40, 20)
        )

        // 风动偏移（树叶微动）
        val wind = sin(System.currentTimeMillis() / 800.0).toFloat() * 1.2f

        // 下层树冠：3x3 绿色方块（暗）
        for (dx in -1..1) for (dy in -1..1) {
            if (abs(dx) == 1 && abs(dy) == 1) continue // 跳过角
            VoxelModels.drawIsoBlock(
                canvas, cx + dx * tw * 0.22f + wind, cy - th * 0.3f + dy * th * 0.18f,
                tw * 0.22f,
                Color.rgb(50, 130, 50),
                Color.rgb(40, 100, 40),
                Color.rgb(30, 80, 30)
            )
        }

        // 上层树冠（更亮）：2 个中心方块
        for (dx in 0..1) for (dy in 0..1) {
            VoxelModels.drawIsoBlock(
                canvas, cx - tw * 0.1f + dx * tw * 0.2f + wind, cy - th * 0.85f + dy * th * 0.16f,
                tw * 0.2f,
                Color.rgb(80, 170, 70),
                Color.rgb(60, 140, 60),
                Color.rgb(40, 110, 50)
            )
        }
    }

    private fun drawHouse(canvas: Canvas, cx: Float, cy: Float, tw: Float, th: Float) {
        // 墙体（在格子上凸出一个 box）
        VoxelModels.drawIsoBlock(
            canvas, cx, cy - th * 0.05f, tw * 0.4f,
            Color.rgb(230, 220, 150),
            Color.rgb(200, 180, 120),
            Color.rgb(160, 140, 90)
        )

        // 屋顶金字塔：4 个三角
        val roofTopY = cy - th * 0.85f
        // 前面三角
        path.reset()
        path.moveTo(cx - tw * 0.5f, cy - th * 0.4f)
        path.lineTo(cx + tw * 0.5f, cy - th * 0.4f)
        path.lineTo(cx, roofTopY)
        path.close()
        paint.color = Color.rgb(170, 60, 50)
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)

        // 右侧面三角
        path.reset()
        path.moveTo(cx + tw * 0.5f, cy - th * 0.4f)
        path.lineTo(cx + tw * 0.5f, cy - th * 0.4f + 0f)
        path.lineTo(cx + tw * 0.5f, cy - th * 0.4f)
        // 简化：右侧面由 box 自己提供，屋顶只画一个前面三角
        paint.style = Paint.Style.FILL
    }
}