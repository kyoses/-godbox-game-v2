package com.godbox.game.engine

import android.graphics.Color

enum class EntityType(
    val color: Int,
    val maxHp: Int,
    val maxAge: Float,
    val hungerRate: Float, // 每秒饥饿增长
    val biteDamage: Int, // 战斗伤害
    val foodValue: Int  // 被吃时给对方的饥饿恢复
) {
    HUMAN(Color.rgb(255, 220, 180), 100, 250f, 1.5f, 8, 60),
    SHEEP(Color.rgb(245, 245, 230), 30, 200f, 1.0f, 0, 35),
    WOLF(Color.rgb(80, 60, 50), 60, 180f, 2.0f, 15, 0),
    FISH(Color.rgb(60, 120, 200), 10, 80f, 0.5f, 0, 25);

    val isPredator: Boolean get() = this == WOLF
    val isHuman: Boolean get() = this == HUMAN
    val isPrey: Boolean get() = this == SHEEP || this == FISH || this == HUMAN
    val isFood: Boolean get() = this == SHEEP || this == FISH
}