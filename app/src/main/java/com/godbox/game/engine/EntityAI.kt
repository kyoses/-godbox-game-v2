package com.godbox.game.engine

import kotlin.math.abs
import kotlin.random.Random

/**
 * 实体 AI 决策。每帧每个活体调用一次。
 */
object EntityAI {

    fun update(e: Entity, world: World, dt: Float) {
        if (e.isDead()) return
        e.age += dt
        e.hunger += e.type.hungerRate * dt
        if (e.attackCooldown > 0f) e.attackCooldown -= dt

        // 1) 战斗：邻格有敌人 → 攻击
        val target = findAdjacentEnemy(e, world)
        if (target != null) {
            if (e.attackCooldown <= 0f) {
                if (target.damage(e.type.biteDamage.toFloat())) {
                    // 目标死亡 — 吃
                    if (target.type.foodValue > 0) e.hunger -= target.type.foodValue
                }
                e.attackCooldown = 1.0f
                e.facing = facingToward(e.cellX(), e.cellY(), target.cellX(), target.cellY())
            }
            return
        }

        // 2) 饥饿 > 50：寻找食物并寻路
        if (e.hunger > 50f) {
            val food = findNearestFood(e, world)
            if (food != null) {
                e.hasTarget = true
                e.targetX = food.x
                e.targetY = food.y
            }
        }

        // 3) 繁殖：人口增长
        if (e.type == EntityType.HUMAN && e.hunger < 50f && e.age > 20f && e.age < 60f
            && Random.nextFloat() < 0.002f) {
            // 邻格有空 → 生新个体
            val cx = e.cellX()
            val cy = e.cellY()
            val spots = mutableListOf<Pair<Int, Int>>()
            for (dx in -1..1) for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = cx + dx
                val ny = cy + dy
                if (world.inBounds(nx, ny) && world.cellAt(nx, ny).terrain.isPassable()
                    && !world.hasEntityAt(nx, ny)) spots.add(nx to ny)
            }
            if (spots.isNotEmpty()) {
                val (nx, ny) = spots.random()
                world.spawnEntity(EntityType.HUMAN, nx, ny, civId = e.civId)
            }
        }

        // 4) 移动：朝目标走一格
        if (e.hasTarget) {
            val tx = e.targetX
            val ty = e.targetY
            val dx = (tx - e.x).toInt()
            val dy = (ty - e.y).toInt()
            val step = stepTowards(e, world, dx, dy)
            if (step != null) {
                e.x = step.first
                e.y = step.second
                e.facing = facingToward(step.first.toInt(), step.second.toInt(), tx, ty)
                // 到达目标附近清空
                if (abs(step.first - tx) < 0.5f && abs(step.second - ty) < 0.5f) {
                    e.hasTarget = false
                }
                return
            }
        }

        // 5) 随机游走
        if (Random.nextFloat() < 0.3f) {
            val dirs = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
            val (dx, dy) = dirs.random()
            val nx = e.cellX() + dx
            val ny = e.cellY() + dy
            if (world.inBounds(nx, ny) && world.cellAt(nx, ny).terrain.isPassable()) {
                e.x = nx.toFloat()
                e.y = ny.toFloat()
                e.facing = facingToward(e.cellX(), e.cellY(), nx, ny)
            }
        }
    }

    private fun findAdjacentEnemy(e: Entity, world: World): Entity? {
        val cx = e.cellX(); val cy = e.cellY()
        for (dx in -1..1) for (dy in -1..1) {
            if (dx == 0 && dy == 0) continue
            val nx = cx + dx; val ny = cy + dy
            val others = world.entitiesAt(nx, ny)
            for (other in others) {
                if (other === e) continue
                if (isHostile(e, other)) return other
            }
        }
        return null
    }

    private fun isHostile(a: Entity, b: Entity): Boolean {
        if (a.type == EntityType.WOLF && (b.type == EntityType.SHEEP || b.type == EntityType.HUMAN)) return true
        if (a.type == EntityType.HUMAN && b.type == EntityType.WOLF) return true
        // 不同文明的人类互相敌对
        if (a.type == EntityType.HUMAN && b.type == EntityType.HUMAN
            && a.civId >= 0 && b.civId >= 0 && a.civId != b.civId) return true
        return false
    }

    private fun findNearestFood(e: Entity, world: World): FoodTarget? {
        val cx = e.cellX(); val cy = e.cellY()
        val maxR = 12
        var best: FoodTarget? = null
        var bestD = Int.MAX_VALUE
        for (r in 1..maxR) {
            for (dx in -r..r) {
                for (dy in -r..r) {
                    if (abs(dx) != r && abs(dy) != r) continue
                    val nx = cx + dx; val ny = cy + dy
                    if (!world.inBounds(nx, ny)) continue
                    val cell = world.cellAt(nx, ny)
                    if (isFoodFor(e, cell)) {
                        val d = dx * dx + dy * dy
                        if (d < bestD) { bestD = d; best = FoodTarget(nx, ny) }
                    }
                    // 也找猎物（实体）
                    val ents = world.entitiesAt(nx, ny)
                    for (other in ents) {
                        if (other === e) continue
                        if (isHostile(e, other) && (e.type == EntityType.WOLF || e.type == EntityType.HUMAN)) {
                            val d = dx * dx + dy * dy
                            if (d < bestD) { bestD = d; best = FoodTarget(nx, ny) }
                        }
                    }
                }
            }
            if (best != null) return best
        }
        return best
    }

    private fun isFoodFor(e: Entity, cell: Cell): Boolean = when (e.type) {
        EntityType.HUMAN -> cell.terrain == TerrainType.TREE
        EntityType.SHEEP -> cell.terrain == TerrainType.GRASS || cell.terrain == TerrainType.DIRT
        EntityType.WOLF -> false
        EntityType.FISH -> false
    }

    private fun stepTowards(e: Entity, world: World, dx: Int, dy: Int): Pair<Float, Float>? {
        val cx = e.cellX(); val cy = e.cellY()
        // 8 个方向：dx / dy 各自 -1/0/1
        val sx = if (dx > 0) 1 else if (dx < 0) -1 else 0
        val sy = if (dy > 0) 1 else if (dy < 0) -1 else 0
        // 随机挑选一个主方向 / 副方向顺序
        val order = if (Random.nextBoolean()) listOf(sx to sy, sx to 0, 0 to sy)
        else listOf(sx to sy, 0 to sy, sx to 0)
        for ((ddx, ddy) in order) {
            if (ddx == 0 && ddy == 0) continue
            val nx = cx + ddx; val ny = cy + ddy
            if (!world.inBounds(nx, ny)) continue
            val cell = world.cellAt(nx, ny)
            // 鱼只能在水里移动
            if (e.type == EntityType.FISH && cell.terrain != TerrainType.WATER) continue
            // 陆生生物不能在水中行走（除了人类可以涉水？这里简化为不能）
            if (e.type != EntityType.FISH && cell.terrain == TerrainType.WATER) continue
            if (!cell.terrain.isPassable()) continue
            if (world.hasEntityAt(nx, ny)) continue
            return nx.toFloat() to ny.toFloat()
        }
        return null
    }

    private fun facingToward(ax: Int, ay: Int, bx: Int, by: Int): Float {
        val dx = (bx - ax).toFloat()
        val dy = (by - ay).toFloat()
        return kotlin.math.atan2(dy, dx)
    }
}

data class FoodTarget(val x: Int, val y: Int)