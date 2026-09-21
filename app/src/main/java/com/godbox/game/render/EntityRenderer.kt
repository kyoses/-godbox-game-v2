package com.godbox.game.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.godbox.game.engine.Entity
import com.godbox.game.engine.EntityType
import com.godbox.game.engine.World
import com.godbox.game.input.Camera
import kotlin.math.cos
import kotlin.math.sin

/**
 * 生物渲染器：Magicavoxel 风格方块生物。
 *
 * 每个生物位于 (worldX, worldY) 处，在 z 方向叠加 box。
 */
class EntityRenderer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val path = Path()

    fun render(canvas: Canvas, world: World, camera: Camera,
               surfaceW: Int, surfaceH: Int,
               visibleMinX: Int, visibleMaxX: Int,
               visibleMinY: Int, visibleMaxY: Int) {

        val tw = camera.tileWidth()
        val th = camera.tileHeight()
        val originX = surfaceW / 2f + camera.offsetX
        val originY = surfaceH / 2f + camera.offsetY
        val worldCenterX = world.width / 2f
        val worldCenterY = world.height / 2f

        // 远处简化：偏远格子（> 25 格远）只画圆点
        val viewCenterX = (visibleMinX + visibleMaxX) / 2
        val viewCenterY = (visibleMinY + visibleMaxY) / 2
        val farThreshold = 22

        // 按 (y + x) 排序（与地形同步）
        val entities = world.entities.sortedBy { it.cellX() + it.cellY() }

        for (e in entities) {
            val cx = e.cellX(); val cy = e.cellY()
            if (cx < visibleMinX - 1 || cx > visibleMaxX + 1) continue
            if (cy < visibleMinY - 1 || cy > visibleMaxY + 1) continue

            val dx = (viewCenterX - cx)
            val dy = (viewCenterY - cy)
            val isFar = kotlin.math.abs(dx) + kotlin.math.abs(dy) > farThreshold

            // 计算屏幕坐标
            val localX = (e.x - e.y) * (tw / 2f)
            val localY = (e.x + e.y) * (th / 2f)
            val sx = originX + localX
            val sy = originY + localY

            // 阴影
            paint.color = Color.argb(80, 0, 0, 0)
            canvas.drawOval(
                sx - tw * 0.25f, sy + th * 0.05f,
                sx + tw * 0.25f, sy + th * 0.25f, paint
            )

            if (isFar) {
                paint.color = e.type.color
                canvas.drawCircle(sx, sy - th * 0.2f, tw * 0.15f, paint)
                continue
            }

            // 走路动画：基于累计时间 + 实体 ID 错开
            val time = System.currentTimeMillis() / 280f
            val offset = sin(time + e.id * 0.7).toFloat() * 1.5f
            val bobY = sin(time * 2 + e.id).toFloat() * 0.5f

            when (e.type) {
                EntityType.HUMAN -> drawHuman(canvas, e, sx, sy, tw, th, offset, bobY)
                EntityType.SHEEP -> drawSheep(canvas, e, sx, sy, tw, th, offset, bobY)
                EntityType.WOLF -> drawWolf(canvas, e, sx, sy, tw, th, offset, bobY)
                EntityType.FISH -> drawFish(canvas, e, sx, sy, tw, th)
            }
        }
    }

    private fun drawHuman(canvas: Canvas, e: Entity, sx: Float, sy: Float,
                          tw: Float, th: Float, walkOffset: Float, bobY: Float) {
        val s = tw * 0.55f  // 单格大小
        val u = s / 6f       // 单位像素

        // 腿：2 个 box，根据 walkOffset 摆动
        VoxelModels.drawIsoBlock(canvas, sx - u * 1.5f + walkOffset * 0.5f,
            sy - bobY, u * 1.5f,
            Color.rgb(150, 100, 60),
            Color.rgb(120, 80, 50),
            Color.rgb(90, 60, 40))
        VoxelModels.drawIsoBlock(canvas, sx + u * 1.5f - walkOffset * 0.5f,
            sy - bobY, u * 1.5f,
            Color.rgb(150, 100, 60),
            Color.rgb(120, 80, 50),
            Color.rgb(90, 60, 40))

        // 身体：蓝色
        VoxelModels.drawIsoBlock(canvas, sx, sy - u * 4 - bobY, u * 3f,
            Color.rgb(60, 100, 180),
            Color.rgb(40, 80, 160),
            Color.rgb(30, 60, 120))

        // 头：肤色
        VoxelModels.drawIsoBlock(canvas, sx, sy - u * 7 - bobY, u * 2.5f,
            Color.rgb(255, 220, 180),
            Color.rgb(230, 200, 160),
            Color.rgb(200, 170, 130))

        // 眼睛（黑色小点）
        paint.color = Color.BLACK
        canvas.drawRect(sx - u * 0.8f, sy - u * 7.5f - bobY,
            sx - u * 0.3f, sy - u * 7f - bobY, paint)
        canvas.drawRect(sx + u * 0.3f, sy - u * 7.5f - bobY,
            sx + u * 0.8f, sy - u * 7f - bobY, paint)

        // 战士红顶
        if (e.isFighter) {
            paint.color = Color.rgb(220, 40, 40)
            canvas.drawRect(sx - u * 1f, sy - u * 9f - bobY,
                sx + u * 1f, sy - u * 8f - bobY, paint)
        }

        // 文明颜色徽章
        if (e.civId >= 0) {
            // 不绘制，避免乱
        }
    }

    private fun drawSheep(canvas: Canvas, e: Entity, sx: Float, sy: Float,
                          tw: Float, th: Float, walkOffset: Float, bobY: Float) {
        val s = tw * 0.6f
        val u = s / 6f
        // 身体：白色椭圆
        VoxelModels.drawIsoBlock(canvas, sx, sy - u * 3 - bobY, u * 4f,
            Color.rgb(250, 250, 245),
            Color.rgb(230, 230, 220),
            Color.rgb(200, 200, 190))

        // 头：米色
        VoxelModels.drawIsoBlock(canvas, sx + u * 3.5f + walkOffset * 0.3f,
            sy - u * 4 - bobY, u * 2f,
            Color.rgb(160, 130, 90),
            Color.rgb(130, 100, 70),
            Color.rgb(100, 80, 50))

        // 4 条腿
        for (i in 0..3) {
            val lx = sx + ((if (i % 2 == 0) -1f else 1f) * (u * 1.5f)) +
                (if (i < 2) walkOffset else -walkOffset) * 0.4f
            VoxelModels.drawIsoBlock(canvas, lx, sy - bobY, u * 0.8f,
                Color.rgb(80, 70, 60),
                Color.rgb(60, 50, 40),
                Color.rgb(40, 30, 20))
        }
    }

    private fun drawWolf(canvas: Canvas, e: Entity, sx: Float, sy: Float,
                        tw: Float, th: Float, walkOffset: Float, bobY: Float) {
        val s = tw * 0.6f
        val u = s / 6f
        // 身体
        VoxelModels.drawIsoBlock(canvas, sx, sy - u * 3 - bobY, u * 4f,
            Color.rgb(110, 100, 95),
            Color.rgb(80, 70, 65),
            Color.rgb(60, 50, 45))

        // 头（朝向右前）
        VoxelModels.drawIsoBlock(canvas, sx + u * 3.5f + walkOffset * 0.3f,
            sy - u * 4 - bobY, u * 2f,
            Color.rgb(110, 100, 95),
            Color.rgb(80, 70, 65),
            Color.rgb(60, 50, 45))

        // 耳朵（2 三角）
        path.reset()
        path.moveTo(sx + u * 4f, sy - u * 6f - bobY)
        path.lineTo(sx + u * 4.5f, sy - u * 5f - bobY)
        path.lineTo(sx + u * 3.5f, sy - u * 5f - bobY)
        path.close()
        paint.color = Color.rgb(80, 70, 65)
        canvas.drawPath(path, paint)

        // 眼睛（红）
        paint.color = Color.rgb(220, 80, 60)
        canvas.drawCircle(sx + u * 4.2f, sy - u * 4.5f - bobY, u * 0.4f, paint)

        // 嘴（黑尖）
        path.reset()
        path.moveTo(sx + u * 5f, sy - u * 3.8f - bobY)
        path.lineTo(sx + u * 5.8f, sy - u * 3.5f - bobY)
        path.lineTo(sx + u * 5f, sy - u * 3.2f - bobY)
        path.close()
        paint.color = Color.rgb(30, 20, 20)
        canvas.drawPath(path, paint)

        // 尾巴
        path.reset()
        path.moveTo(sx - u * 4f, sy - u * 3f - bobY)
        path.lineTo(sx - u * 5.5f, sy - u * 4.5f - bobY)
        path.lineTo(sx - u * 5f, sy - u * 3f - bobY)
        path.close()
        paint.color = Color.rgb(90, 80, 75)
        canvas.drawPath(path, paint)

        // 4 条腿
        for (i in 0..3) {
            val lx = sx + ((if (i % 2 == 0) -1f else 1f) * (u * 1.5f)) +
                (if (i < 2) walkOffset else -walkOffset) * 0.4f
            VoxelModels.drawIsoBlock(canvas, lx, sy - bobY, u * 0.8f,
                Color.rgb(70, 60, 55),
                Color.rgb(50, 40, 35),
                Color.rgb(30, 20, 15))
        }
    }

    private fun drawFish(canvas: Canvas, e: Entity, sx: Float, sy: Float,
                         tw: Float, th: Float) {
        val u = tw * 0.1f
        // 身体（椭圆）
        paint.color = Color.rgb(60, 130, 200)
        canvas.drawOval(sx - u * 2f, sy - u * 0.8f,
            sx + u * 2f, sy + u * 0.8f, paint)
        // 尾（三角）
        path.reset()
        path.moveTo(sx - u * 2f, sy)
        path.lineTo(sx - u * 3.5f, sy - u * 1.5f)
        path.lineTo(sx - u * 3.5f, sy + u * 1.5f)
        path.close()
        paint.color = Color.rgb(40, 90, 150)
        canvas.drawPath(path, paint)
        // 眼睛
        paint.color = Color.BLACK
        canvas.drawCircle(sx + u * 1.5f, sy - u * 0.3f, u * 0.3f, paint)
    }
}