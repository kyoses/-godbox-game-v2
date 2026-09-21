package com.godbox.game.render

import android.graphics.Canvas
import android.graphics.Paint
import com.godbox.game.engine.Entity
import com.godbox.game.engine.EntityType
import com.godbox.game.engine.World

class EntityRenderer(private val atlas: SpriteAtlas) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun render(canvas: Canvas, world: World, atlas: SpriteAtlas,
               originX: Float, originY: Float,
               visMinX: Int, visMaxX: Int,
               visMinY: Int, visMaxY: Int) {

        val tw = atlas.tileWidth
        val th = atlas.tileHeight

        // 按 y+x 排序（与地形同步）
        val entities = world.entities.sortedBy { it.cellX() + it.cellY() }

        val viewCenterX = (visMinX + visMaxX) / 2
        val viewCenterY = (visMinY + visMaxY) / 2

        for (e in entities) {
            val cx = e.cellX(); val cy = e.cellY()
            if (cx < visMinX - 1 || cx > visMaxX + 1) continue
            if (cy < visMinY - 1 || cy > visMaxY + 1) continue

            val localX = (e.x - e.y) * (tw / 2f)
            val localY = (e.x + e.y) * (th / 2f)
            val sx = originX + localX
            val sy = originY + localY

            // 走路动画
            val walkPhase = (System.currentTimeMillis() / 300 + e.id * 7) % 4
            val bobY = if (walkPhase < 2) 0 else -1

            // 阴影
            paint.color = 0x60000000.toInt()
            canvas.drawOval(
                sx - 10f, sy - 2f,
                sx + 10f, sy + 4f, paint
            )

            // 朝向（基于 facing 角）
            val facingLeft = e.facing > 1.57f && e.facing < 4.71f

            val sprite = when (e.type) {
                EntityType.HUMAN -> if (facingLeft) atlas.humanSprites.bodyLeft else atlas.humanSprites.body
                EntityType.SHEEP -> if (facingLeft) atlas.sheepSprites.bodyLeft else atlas.sheepSprites.body
                EntityType.WOLF -> if (facingLeft) atlas.wolfSprites.bodyLeft else atlas.wolfSprites.body
                EntityType.FISH -> if (facingLeft) atlas.fishSprites.bodyLeft else atlas.fishSprites.body
            }

            val halfW = sprite.width / 2f
            val drawY = sy - sprite.height + bobY
            canvas.drawBitmap(sprite, sx - halfW, drawY, paint)

            // 战士徽章
            if (e.type == EntityType.HUMAN && e.isFighter) {
                paint.color = 0xFFFF3030.toInt()
                canvas.drawCircle(sx + 4, drawY + 4, 3f, paint)
            }

            // 生命值条
            if (e.hp < e.type.maxHp) {
                val hpRatio = e.hp / e.type.maxHp
                paint.color = 0xFF333333.toInt()
                canvas.drawRect(sx - 8, drawY - 4, sx + 8, drawY - 2, paint)
                paint.color = if (hpRatio > 0.5f) 0xFF4CAF50.toInt()
                    else if (hpRatio > 0.25f) 0xFFFFA726.toInt()
                    else 0xFFE53935.toInt()
                canvas.drawRect(sx - 8, drawY - 4, sx - 8 + 16 * hpRatio, drawY - 2, paint)
            }
        }
    }
}