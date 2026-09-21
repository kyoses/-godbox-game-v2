package com.godbox.game.engine

import android.graphics.Color
import kotlin.random.Random

/**
 * 部落 / 文明。
 */
class Civilization(
    val id: Int,
    var centerX: Int,
    var centerY: Int,
    val members: MutableList<Long> = mutableListOf(),
    val enemies: MutableSet<Int> = mutableSetOf()
) {
    val color: Int = Color.rgb(
        Random.nextInt(80, 230),
        Random.nextInt(80, 230),
        Random.nextInt(80, 230)
    )

    fun memberCount(): Int = members.size
}