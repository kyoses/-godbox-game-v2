package com.threekingdoms.emperor.render

import kotlin.math.cos
import kotlin.math.sin

/**
 * 等距投影工具（地图用）。
 */
object Projection {

    fun iso(
        x: Int, y: Int,
        tileWidth: Float, tileHeight: Float,
        originX: Float, originY: Float
    ): Pair<Float, Float> {
        val sx = (x - y) * (tileWidth / 2f) + originX
        val sy = (x + y) * (tileHeight / 2f) + originY
        return sx to sy
    }

    fun worldToScreen(
        wx: Float, wy: Float,
        tileWidth: Float, tileHeight: Float,
        originX: Float, originY: Float
    ): Pair<Float, Float> {
        val sx = (wx - wy) * (tileWidth / 2f) + originX
        val sy = (wx + wy) * (tileHeight / 2f) + originY
        return sx to sy
    }

    fun screenToIso(
        sx: Float, sy: Float,
        tileWidth: Float, tileHeight: Float,
        originX: Float, originY: Float
    ): Pair<Float, Float> {
        val dx = sx - originX
        val dy = sy - originY
        val wx = (dx / (tileWidth / 2f) + dy / (tileHeight / 2f)) / 2f
        val wy = (dy / (tileHeight / 2f) - dx / (tileWidth / 2f)) / 2f
        return wx to wy
    }
}