package com.godbox.game.input

import com.godbox.game.render.Projection

/**
 * 相机：负责屏幕视口位置（offsetX/Y）+ 缩放。
 * 投影（轴测）由 Projection 工具完成，Camera 提供屏幕原点和视口大小。
 */
class Camera {

    /** 屏幕偏移：在 translate() 时让世界原点在屏幕的哪 */
    var offsetX: Float = 0f
    var offsetY: Float = 0f

    /** 全局缩放（影响 tileWidth/Height） */
    var scale: Float = 1f
        set(value) { field = value.coerceIn(0.6f, 3.0f) }

    val projection: Projection = Projection()

    var godHandActive: Boolean = false
    var godHandX: Int = 0
    var godHandY: Int = 0

    /** 当前缩放下的 tile 宽度 / 高度 */
    fun tileWidth(): Float = projection.tileWidth * scale
    fun tileHeight(): Float = projection.tileHeight * scale

    /** 屏幕 → 世界格子坐标 */
    fun screenToCell(sx: Float, sy: Float, surfaceW: Int, surfaceH: Int,
                     worldW: Int, worldH: Int): Pair<Int, Int> {
        val tw = tileWidth()
        val th = tileHeight()
        val originX = surfaceW / 2f + offsetX
        val originY = surfaceH / 2f + offsetY
        val dx = sx - originX
        val dy = sy - originY
        val wx = (dx / (tw / 2f) + dy / (th / 2f)) / 2f + worldW / 2f
        val wy = (dy / (th / 2f) - dx / (tw / 2f)) / 2f + worldH / 2f
        return wx.toInt().coerceIn(0, worldW - 1) to wy.toInt().coerceIn(0, worldH - 1)
    }

    /** 屏幕坐标 → 世界（连续，含小数） */
    fun screenToWorldF(sx: Float, sy: Float, surfaceW: Int, surfaceH: Int,
                       worldW: Int, worldH: Int): Pair<Float, Float> {
        val tw = tileWidth()
        val th = tileHeight()
        val originX = surfaceW / 2f + offsetX
        val originY = surfaceH / 2f + offsetY
        val dx = sx - originX
        val dy = sy - originY
        val wx = (dx / (tw / 2f) + dy / (th / 2f)) / 2f + worldW / 2f
        val wy = (dy / (th / 2f) - dx / (tw / 2f)) / 2f + worldH / 2f
        return wx to wy
    }

    /** 世界 (wx, wy, z=0) → 屏幕坐标。返回 (sx, sy)。 */
    fun worldToScreen(wx: Float, wy: Float, surfaceW: Int, surfaceH: Int,
                      z: Float = 0f): Pair<Float, Float> {
        val tw = tileWidth()
        val th = tileHeight()
        val dh = projection.depthHeight * scale
        val localX = (wx - wy) * (tw / 2f)
        val localY = (wx + wy) * (th / 2f) - z * dh
        val sx = surfaceW / 2f + offsetX + localX
        val sy = surfaceH / 2f + offsetY + localY
        return sx to sy
    }

    /** 屏幕像素拖动量 → 相机偏移变化（按当前缩放反转） */
    fun panByPixels(dx: Float, dy: Float) {
        offsetX += dx
        offsetY += dy
    }

    fun zoom(factor: Float) {
        scale *= factor
    }

    /** 把屏幕中心对齐到指定世界格子 */
    fun centerOn(cellX: Int, cellY: Int, surfaceW: Int, surfaceH: Int,
                 worldW: Int, worldH: Int) {
        val tw = tileWidth()
        val th = tileHeight()
        // 让 (cellX, cellY, z=0) 渲染在屏幕中心
        // localX = (cellX - cellY) * tw/2, localY = (cellX + cellY) * th/2
        // surfaceW/2 + offsetX + localX = surfaceW/2 → offsetX = -localX
        val localX = (cellX - cellY) * (tw / 2f)
        val localY = (cellX + cellY) * (th / 2f)
        offsetX = -localX + (surfaceW / 2f - surfaceW / 2f)
        offsetY = -localY + (surfaceH / 2f - surfaceH / 2f)
        // 简化：直接置 0，再让屏幕原点在 (surfaceW/2 - centerCellX*tw/2, ...)
        // 用一个更直接的公式：屏幕中心对应世界中心 + (offsetX, offsetY)
        // 我们想要 cellX 出现在屏幕中心
        val dx = (cellX - worldW / 2f) * (tw / 2f) - (cellY - worldH / 2f) * (tw / 2f)
        val dy = (cellX - worldW / 2f) * (th / 2f) + (cellY - worldH / 2f) * (th / 2f)
        offsetX = -dx
        offsetY = -dy
    }

    fun enterGodHand(cellX: Int, cellY: Int, surfaceW: Int, surfaceH: Int,
                     worldW: Int, worldH: Int) {
        godHandActive = true
        godHandX = cellX
        godHandY = cellY
        scale = 2.2f
        centerOn(cellX, cellY, surfaceW, surfaceH, worldW, worldH)
    }

    fun exitGodHand() {
        godHandActive = false
        scale = 1f
    }
}