package com.godbox.game.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.godbox.game.engine.EffectEvent
import com.godbox.game.engine.World

class ParticleRenderer(private val atlas: SpriteAtlas) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()

    fun render(canvas: Canvas, world: World, atlas: SpriteAtlas,
               originX: Float, originY: Float,
               visMinX: Int, visMaxX: Int,
               visMinY: Int, visMaxY: Int) {
        val tw = atlas.tileWidth
        val th = atlas.tileHeight

        // 雨滴
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        paint.color = Color.argb(200, 180, 200, 255)
        for (d in world.weather.raindrops) {
            val wx = d.x; val wy = d.y
            val sx = originX + (wx - wy) * (tw / 2f)
            val sy = originY + (wx + wy) * (th / 2f)
            canvas.drawLine(sx, sy, sx - 3f, sy + 8f, paint)
        }
        paint.style = Paint.Style.FILL

        // 灾害粒子
        for (p in world.disaster.particles) {
            val alpha = (p.life / p.maxLife * 255f).toInt().coerceIn(0, 255)
            paint.color = (p.color and 0x00FFFFFF) or (alpha shl 24)
            val sx = originX + (p.x - p.y) * (tw / 2f)
            val sy = originY + (p.x + p.y) * (th / 2f)
            canvas.drawCircle(sx, sy, p.size, paint)
        }

        // 灾害事件
        for (ev in world.disaster.events) {
            val sx = originX + (ev.x - ev.y) * (tw / 2f)
            val sy = originY + (ev.x + ev.y) * (th / 2f)
            when (ev.type) {
                EffectEvent.Type.VOLCANO -> {
                    val alpha = (ev.life / ev.maxLife * 180f).toInt().coerceIn(0, 255)
                    paint.color = Color.argb(alpha, 255, 80, 30)
                    canvas.drawCircle(sx, sy, ev.radius * th * 1.5f, paint)
                    paint.color = Color.argb((alpha * 0.5f).toInt(), 80, 80, 80)
                    canvas.drawCircle(sx - 8, sy - 12, ev.radius * th * 0.8f, paint)
                }
                EffectEvent.Type.HOLY -> {
                    val alpha = (ev.life / ev.maxLife * 220f).toInt().coerceIn(0, 255)
                    paint.color = Color.argb(alpha, 255, 240, 120)
                    val rays = 8
                    val radius = ev.radius * th * (1.8f - ev.life / ev.maxLife)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2.5f
                    for (i in 0 until rays) {
                        val angle = (i.toFloat() / rays) * 6.28f
                        val len = radius * (0.8f + (i % 3) * 0.15f)
                        canvas.drawLine(
                            sx, sy,
                            sx + kotlin.math.cos(angle) * len,
                            sy + kotlin.math.sin(angle) * len, paint
                        )
                    }
                    paint.style = Paint.Style.FILL
                }
                EffectEvent.Type.LIGHTNING -> {
                    paint.color = Color.argb(240, 240, 240, 255)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3.5f
                    path.reset()
                    path.moveTo(sx, sy - th * 6f)
                    path.lineTo(sx + 2f, sy - th * 4f)
                    path.lineTo(sx - 2f, sy - th * 2.5f)
                    path.lineTo(sx + 3f, sy - th)
                    path.lineTo(sx, sy)
                    canvas.drawPath(path, paint)
                    paint.strokeWidth = 1.8f
                    paint.color = Color.argb(200, 180, 180, 220)
                    canvas.drawLine(sx + 2f, sy - th * 4f,
                        sx + 8f, sy - th * 5f, paint)
                    canvas.drawLine(sx - 2f, sy - th * 2.5f,
                        sx - 8f, sy - th * 1.8f, paint)
                    paint.style = Paint.Style.FILL
                }
                EffectEvent.Type.METEOR_FALL -> {
                    paint.color = Color.argb(220, 255, 200, 80)
                    canvas.drawCircle(sx, sy, tw * 0.25f, paint)
                    val tColors = intArrayOf(
                        Color.argb(180, 255, 150, 50),
                        Color.argb(120, 255, 100, 30),
                        Color.argb(60, 200, 80, 30)
                    )
                    for (i in 0..2) {
                        paint.color = tColors[i]
                        canvas.drawCircle(sx, sy - th * (0.5f + i * 0.8f),
                            tw * (0.18f - i * 0.05f), paint)
                    }
                }
            }
        }
    }
}