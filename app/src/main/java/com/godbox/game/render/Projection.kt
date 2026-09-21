package com.godbox.game.render

import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin

/**
 * 轴测投影工具：将世界 (x, y, z) 坐标投影到屏幕 (sx, sy)。
 *
 * 视角：45° 倾斜（Minecraft / WorldBox 风格）。世界中心渲染在屏幕中央。
 *
 * - tileWidth  / tileHeight = 单个格子的"地面菱形"在屏幕上的宽度/高度（默认 32x16）
 * - depthHeight = 单位 z 高度的屏幕像素（默认 12）
 */
class Projection(
    val tileWidth: Float = 32f,
    val tileHeight: Float = 16f,
    val depthHeight: Float = 12f
) {

    fun worldToScreen(worldX: Float, worldY: Float, worldZ: Float, out: PointF) {
        out.x = (worldX - worldY) * (tileWidth / 2f)
        out.y = (worldX + worldY) * (tileHeight / 2f) - worldZ * depthHeight
    }

    fun worldToScreen(worldX: Int, worldY: Int, worldZ: Int, out: PointF) {
        worldToScreen(worldX.toFloat(), worldY.toFloat(), worldZ.toFloat(), out)
    }

    /** 屏幕 → 世界格子（不含 z）。返回 (cellX, cellY)。 */
    fun screenToCell(sx: Float, sy: Float, originX: Float, originY: Float): Pair<Int, Int> {
        val dx = sx - originX
        val dy = sy - originY
        val wx = (dx / (tileWidth / 2f) + dy / (tileHeight / 2f)) / 2f
        val wy = (dy / (tileHeight / 2f) - dx / (tileWidth / 2f)) / 2f
        return wx.toInt() to wy.toInt()
    }

    companion object {
        /** 共用 out 避免每帧分配 */
        fun newPoint(): PointF = PointF()
    }
}