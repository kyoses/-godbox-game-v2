package com.godbox.game.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 生物实体。x, y 用"格子坐标"（浮点，允许在格间插值）。
 */
class Entity(
    val id: Long,
    var type: EntityType,
    var x: Float,
    var y: Float,
    val spawnX: Int,
    val spawnY: Int
) {
    var hp: Float = type.maxHp.toFloat()
    var hunger: Float = 20f          // 0=饱，100=饿死
    var age: Float = 0f
    var civId: Int = -1
    var isFighter: Boolean = false

    // 行为状态
    var targetX: Int = 0
    var targetY: Int = 0
    var hasTarget: Boolean = false
    var attackCooldown: Float = 0f

    // 朝向（弧度），用于渲染时画朝向线
    var facing: Float = Random.nextFloat() * 6.28f

    fun facingDx(): Float = cos(facing) * 4f
    fun facingDy(): Float = sin(facing) * 4f

    fun damage(amount: Float): Boolean {
        hp -= amount
        return hp <= 0
    }

    fun isDead(): Boolean = hp <= 0 || age >= type.maxAge || hunger >= 100f

    fun cellX(): Int = x.toInt().coerceAtLeast(0)
    fun cellY(): Int = y.toInt().coerceAtLeast(0)
}