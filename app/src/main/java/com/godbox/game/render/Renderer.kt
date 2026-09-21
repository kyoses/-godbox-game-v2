package com.godbox.game.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.godbox.game.engine.World
import com.godbox.game.input.Camera

/**
 * 主管线：清屏 → 应用相机 → 地形 → 实体 → 粒子 → 选中格 → HUD。
 */
class Renderer {

    private val terrain = TerrainRenderer()
    private val entities = EntityRenderer()
    private val particles = ParticleRenderer()
    private val hud = HUDRenderer()

    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val civBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    fun render(canvas: Canvas, world: World, camera: Camera,
               surfaceWidth: Int, surfaceHeight: Int,
               selectedCellX: Int, selectedCellY: Int,
               tool: String, godHandActive: Boolean) {
        // 清屏（深空蓝）
        canvas.drawColor(Color.rgb(20, 30, 50))

        // 计算可见区域（按世界中心渲染 + 偏移 + 屏幕大小反推）
        val tw = camera.tileWidth()
        val th = camera.tileHeight()
        val originX = surfaceWidth / 2f + camera.offsetX
        val originY = surfaceHeight / 2f + camera.offsetY

        // 屏幕的 4 角对应的世界 (x, y)：用 z=0 反推
        // localX = (wx - wy) * tw/2, localY = (wx + wy) * th/2
        // 屏幕左下角 (0, surfaceHeight) 对应哪个 (wx, wy)？
        // 0 = originX + (wx - wy)*tw/2 → wx - wy = -2*originX/tw
        // surfaceHeight = originY + (wx + wy)*th/2 → wx + wy = (surfaceHeight - originY)*2/th
        val corners = arrayOf(
            floatArrayOf(0f, 0f),
            floatArrayOf(surfaceWidth.toFloat(), 0f),
            floatArrayOf(0f, surfaceHeight.toFloat()),
            floatArrayOf(surfaceWidth.toFloat(), surfaceHeight.toFloat())
        )
        var minX = Int.MAX_VALUE; var maxX = Int.MIN_VALUE
        var minY = Int.MAX_VALUE; var maxY = Int.MIN_VALUE
        for (c in corners) {
            val sx = c[0]; val sy = c[1]
            val dx = sx - originX
            val dy = sy - originY
            // wx - wy = 2*dx/tw, wx + wy = 2*dy/th
            val wxF = (dx / (tw / 2f) + dy / (th / 2f)) / 2f + world.width / 2f
            val wyF = (dy / (th / 2f) - dx / (tw / 2f)) / 2f + world.height / 2f
            val ix = wxF.toInt()
            val iy = wyF.toInt()
            if (ix < minX) minX = ix
            if (ix > maxX) maxX = ix
            if (iy < minY) minY = iy
            if (iy > maxY) maxY = iy
        }
        // 留余量
        val visibleMinX = (minX - 2).coerceIn(0, world.width - 1)
        val visibleMaxX = (maxX + 2).coerceIn(0, world.width - 1)
        val visibleMinY = (minY - 2).coerceIn(0, world.height - 1)
        val visibleMaxY = (maxY + 2).coerceIn(0, world.height - 1)

        // 地形（按 y+x 排序，自带深度）
        terrain.render(canvas, world, camera, surfaceWidth, surfaceHeight,
            visibleMinX, visibleMaxX, visibleMinY, visibleMaxY)

        // 实体
        entities.render(canvas, world, camera, surfaceWidth, surfaceHeight,
            visibleMinX, visibleMaxX, visibleMinY, visibleMaxY)

        // 粒子（不参与排序 — 在屏幕坐标最上层）
        particles.render(canvas, world, camera, surfaceWidth, surfaceHeight,
            visibleMinX, visibleMaxX, visibleMinY, visibleMaxY)

        // 文明边框
        for (civ in world.civilizations) {
            if (civ.centerX in visibleMinX..visibleMaxX
                && civ.centerY in visibleMinY..visibleMaxY) {
                val sx = originX + (civ.centerX - civ.centerY) * (tw / 2f)
                val sy = originY + (civ.centerX + civ.centerY) * (th / 2f)
                civBorderPaint.color = civ.color
                val r = tw * 2.5f
                canvas.drawRect(sx - r, sy - th * 0.6f, sx + r, sy + th * 2.5f, civBorderPaint)
            }
        }

        // 选中格高亮
        if (selectedCellX in 0 until world.width && selectedCellY in 0 until world.height) {
            val sx = originX + (selectedCellX - selectedCellY) * (tw / 2f)
            val sy = originY + (selectedCellX + selectedCellY) * (th / 2f)
            val flash = ((System.currentTimeMillis() / 5) % 100).toInt()
            selectionPaint.color = Color.argb(120 + flash, 255, 255, 100)
            // 画菱形边框
            val r = tw * 0.5f
            val h = th * 0.5f
            canvas.drawLine(sx - r, sy, sx, sy - h, selectionPaint)
            canvas.drawLine(sx, sy - h, sx + r, sy, selectionPaint)
            canvas.drawLine(sx + r, sy, sx, sy + h, selectionPaint)
            canvas.drawLine(sx, sy + h, sx - r, sy, selectionPaint)
        }

        // HUD 不经过相机转换
        hud.render(canvas, world, tool, godHandActive)
    }
}