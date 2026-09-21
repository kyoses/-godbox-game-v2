package com.godbox.game.engine

import android.graphics.Color
import com.godbox.game.render.VoxelModels

/**
 * 地形类型。每个地形有：
 * - 基础颜色
 * - 3D 高度（Z 方向单位）
 * - 顶面/前面/右侧面预计算颜色（伪造光照）
 * - 是否可通行
 */
enum class TerrainType(
    val baseColor: Int,
    val height: Int,
    val walkable: Boolean
) {
    WATER(Color.rgb(40, 90, 180), 0, false),
    SAND(Color.rgb(225, 200, 130), 1, true),
    DIRT(Color.rgb(140, 100, 60), 2, true),
    GRASS(Color.rgb(86, 152, 64), 2, true),
    TREE(Color.rgb(86, 152, 64), 2, false),
    ASH(Color.rgb(70, 60, 55), 1, true),
    ROAD(Color.rgb(120, 100, 80), 1, true),
    VILLAGE(Color.rgb(180, 140, 90), 2, true),
    MOUNTAIN(Color.rgb(120, 110, 100), 5, false);

    /** 顶面（最亮） */
    val topColor: Int = VoxelModels.brighten(baseColor, 0.25f)
    /** 前面（中间） */
    val frontColor: Int = baseColor
    /** 右侧面（最暗） */
    val rightColor: Int = VoxelModels.darken(baseColor, 0.25f)

    fun isPassable(): Boolean = walkable

    /** 不同地形对应的"垂直噪声倍率"，用于山的随机高度 */
    fun heightJitter(): Int = when (this) {
        MOUNTAIN -> 4   // 山在 height 基础上再加 0~4
        else -> 0
    }
}