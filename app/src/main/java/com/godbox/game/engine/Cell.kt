package com.godbox.game.engine

/**
 * 单个地图格子。
 */
class Cell(
    var terrain: TerrainType = TerrainType.GRASS,
    var moisture: Float = 0f,           // 0..1，影响是否生长树
    var civId: Int = -1,                // 所属文明 id；-1 = 无
    var heightJitter: Float = 0f        // 绘制时随机抖动 (±0.15)
)