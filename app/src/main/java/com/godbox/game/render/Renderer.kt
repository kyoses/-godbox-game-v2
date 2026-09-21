package com.godbox.game.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.godbox.game.engine.Cell
import com.godbox.game.engine.Entity
import com.godbox.game.engine.EntityType
import com.godbox.game.engine.TerrainType
import com.godbox.game.engine.World
import com.godbox.game.input.Camera

/**
 * 主管线：清屏 → 地形（按 y+x 排序）→ 装饰物 → 实体 → 粒子 → HUD。
 *
 * 所有绘制都基于 SpriteAtlas 提供的预渲染位图。
 */
class Renderer {

    private val atlas: SpriteAtlas = SpriteAtlas()
    private val entities = EntityRenderer(atlas)
    private val particles = ParticleRenderer(atlas)
    private val hud = HUDRenderer()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun render(canvas: Canvas, world: World, camera: Camera,
               surfaceWidth: Int, surfaceHeight: Int,
               selectedCellX: Int, selectedCellY: Int,
               tool: String, godHandActive: Boolean) {

        canvas.drawColor(Color.rgb(15, 25, 45))

        val tw = atlas.tileWidth
        val th = atlas.tileHeight
        val bh = atlas.blockHeight
        val originX = surfaceWidth / 2f + camera.offsetX
        val originY = surfaceHeight / 2f + camera.offsetY

        // 计算可见范围
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
            val wxF = (dx / (tw / 2f) + dy / (th / 2f)) / 2f + world.width / 2f
            val wyF = (dy / (th / 2f) - dx / (tw / 2f)) / 2f + world.height / 2f
            val ix = wxF.toInt(); val iy = wyF.toInt()
            if (ix < minX) minX = ix
            if (ix > maxX) maxX = ix
            if (iy < minY) minY = iy
            if (iy > maxY) maxY = iy
        }
        val visMinX = (minX - 2).coerceIn(0, world.width - 1)
        val visMaxX = (maxX + 2).coerceIn(0, world.width - 1)
        val visMinY = (minY - 2).coerceIn(0, world.height - 1)
        val visMaxY = (maxY + 2).coerceIn(0, world.height - 1)

        // 地形（按 y+x 排序）
        for (sum in 0..(world.width + world.height - 2)) {
            val startY = maxOf(0, sum - (world.width - 1))
            val endY = minOf(world.height - 1, sum)
            for (y in startY..endY) {
                val x = sum - y
                if (x < 0 || x >= world.width) continue
                if (x < visMinX - 1 || x > visMaxX + 1) continue
                if (y < visMinY - 1 || y > visMaxY + 1) continue
                drawCell(canvas, world.cellAt(x, y), x, y, originX, originY, world)
            }
        }

        // 实体
        entities.render(canvas, world, atlas, originX, originY, visMinX, visMaxX, visMinY, visMaxY)

        // 粒子
        particles.render(canvas, world, atlas, originX, originY, visMinX, visMaxX, visMinY, visMaxY)

        // 选中格
        if (selectedCellX in 0 until world.width && selectedCellY in 0 until world.height) {
            val sx = originX + (selectedCellX - selectedCellY) * (tw / 2f)
            val sy = originY + (selectedCellX + selectedCellY) * (th / 2f)
            val bmp = atlas.selectDiamond
            canvas.drawBitmap(bmp, sx - bmp.width / 2f, sy - bmp.height / 2f, paint)
        }

        // HUD（不在相机内）
        hud.render(canvas, world, tool, godHandActive)
    }

    private fun drawCell(canvas: Canvas, cell: Cell, x: Int, y: Int,
                        originX: Float, originY: Float, world: World) {
        val sprites = atlas.terrainSprites[cell.terrain] ?: return
        val tw = atlas.tileWidth
        val th = atlas.tileHeight
        val bh = atlas.blockHeight
        val effectiveHeight = cell.terrain.height +
            if (cell.terrain == TerrainType.MOUNTAIN)
                (cell.heightJitter * 4f).toInt().coerceAtLeast(0)
            else 0
        val blockPx = bh * effectiveHeight

        val localX = (x - y) * (tw / 2f)
        val localY = (x + y) * (th / 2f)
        val cx = originX + localX
        val cy = originY + localY

        // 顶面（菱形，z = blockPx + th）
        val topY = cy - blockPx - th
        val topX = cx - tw / 2f
        canvas.drawBitmap(sprites.top, topX, topY, paint)

        // 前面（左半 + 底尖）
        val frontX = cx - tw / 2f
        val frontY = cy - blockPx
        canvas.drawBitmap(sprites.front, frontX, frontY, paint)

        // 右侧面（右半）
        val rightX = cx - tw / 2f
        val rightY = cy - blockPx
        canvas.drawBitmap(sprites.right, rightX, rightY, paint)

        // 装饰物
        when (cell.terrain) {
            TerrainType.TREE -> {
                // 树干（在前面正中）
                val trunk = atlas.treeTrunk
                canvas.drawBitmap(
                    trunk,
                    cx - trunk.width / 2f,
                    cy - blockPx - trunk.height + 6f,
                    paint
                )
                // 树冠（在上方，覆盖顶面）
                val top = atlas.treeTop
                canvas.drawBitmap(
                    top,
                    cx - top.width / 2f,
                    cy - blockPx - th - top.height * 0.7f,
                    paint
                )
            }
            TerrainType.VILLAGE -> {
                // 房子
                val body = atlas.houseBody
                val roofL = atlas.roofLeft
                val roofR = atlas.roofRight
                // 屋顶在前
                canvas.drawBitmap(roofL,
                    cx - tw / 2f - 2,
                    cy - blockPx - roofL.height + 4f, paint)
                canvas.drawBitmap(roofR,
                    cx - 2, cy - blockPx - roofR.height + 4f, paint)
                // 墙体在屋顶下
                canvas.drawBitmap(body,
                    cx - body.width / 2f,
                    cy - blockPx - body.height + roofL.height, paint)
            }
            TerrainType.MOUNTAIN -> {
                // 雪冠（仅最高的山）
                if (effectiveHeight >= 5) {
                    val bmp = atlas.terrainSprites[TerrainType.MOUNTAIN]!!.top
                    // 简化：在山顶画一个亮色菱形
                    paint.color = Color.argb(220, 240, 240, 250)
                    val cx2 = cx
                    val cy2 = cy - blockPx - th - 4
                    canvas.drawCircle(cx2 - 3, cy2, 4f, paint)
                    paint.color = Color.argb(220, 220, 220, 235)
                    canvas.drawCircle(cx2 + 3, cy2 + 2, 4f, paint)
                }
            }
            else -> {}
        }
    }

    fun recycle() {
        atlas.recycle()
    }
}