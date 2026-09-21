package com.godbox.game.engine

import kotlin.random.Random

/** 一个粒子（火山灰、火球、闪电节点等） */
class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var maxLife: Float,
    val color: Int,
    val size: Float,
    val gravity: Float = 0f
)

/** 整场灾害的视觉事件 */
class EffectEvent(
    var x: Float,
    var y: Float,
    var life: Float,
    val maxLife: Float,
    val type: Type,
    val radius: Float = 0f
) {
    enum class Type { METEOR_FALL, VOLCANO, HOLY, LIGHTNING }
}

/**
 * 灾害触发。所有方法都是同步事件：写地形、扣血，并添加一个 EffectEvent。
 * 粒子系统（火山灰、火球尾迹等）在 World.update 中按事件产生 Particle。
 */
class DisasterSystem(private val world: World) {

    private var nextEventId: Long = 1
    val events: ArrayDeque<EffectEvent> = ArrayDeque()
    val particles: ArrayDeque<Particle> = ArrayDeque()

    fun meteorShower(count: Int = 5) {
        repeat(count) {
            val cx = Random.nextInt(world.width)
            val cy = Random.nextInt(world.height)
            // 直接落地砸坑：中心 3x3 改 ASH + damage
            meteorStrike(cx, cy)
        }
    }

    fun meteorStrike(cx: Int, cy: Int) {
        spawnMeteorEvent(cx, cy)
        damageArea(cx, cy, 1, 40)
        for (dx in -1..1) for (dy in -1..1) {
            val c = world.cellAtSafe(cx + dx, cy + dy) ?: continue
            c.terrain = TerrainType.ASH
        }
    }

    fun volcano(cx: Int, cy: Int) {
        events.addLast(
            EffectEvent(cx.toFloat(), cy.toFloat(), 4f, 4f, EffectEvent.Type.VOLCANO, radius = 5f)
        )
        damageArea(cx, cy, 5, 50)
        for (dx in -2..2) for (dy in -2..2) {
            val c = world.cellAtSafe(cx + dx, cy + dy) ?: continue
            if (Random.nextFloat() < 0.7f) c.terrain = TerrainType.MOUNTAIN
            spawnFireParticles((cx + dx).toFloat(), (cy + dy).toFloat(), 8)
        }
    }

    fun holyLight(cx: Int, cy: Int) {
        events.addLast(
            EffectEvent(cx.toFloat(), cy.toFloat(), 1.5f, 1.5f, EffectEvent.Type.HOLY, radius = 5f)
        )
        world.entities.forEach { e ->
            val dx = e.cellX() - cx
            val dy = e.cellY() - cy
            if (dx * dx + dy * dy <= 25) {
                e.hp = e.type.maxHp.toFloat()
                e.hunger = 0f
            }
        }
    }

    fun lightning(cx: Int, cy: Int) {
        events.addLast(
            EffectEvent(cx.toFloat(), cy.toFloat(), 0.6f, 0.6f, EffectEvent.Type.LIGHTNING, radius = 1.5f)
        )
        damageArea(cx, cy, 1, 80)
    }

    fun flood() {
        // 水域向上扩张：所有 WATER 格 50% 概率向上扩展
        val newWater = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until world.width) {
            for (y in 1 until world.height) {
                val c = world.cellAtSafe(x, y) ?: continue
                if (c.terrain != TerrainType.WATER) continue
                val above = world.cellAtSafe(x, y - 1) ?: continue
                if (above.terrain == TerrainType.WATER) continue
                if (Random.nextFloat() < 0.5f) newWater.add(x to y - 1)
            }
        }
        for ((x, y) in newWater) world.cellAt(x, y).terrain = TerrainType.WATER
    }

    fun randomStormStrike() {
        val cx = Random.nextInt(world.width)
        val cy = Random.nextInt(world.height)
        lightning(cx, cy)
    }

    private fun spawnMeteorEvent(cx: Int, cy: Int) {
        events.addLast(
            EffectEvent(cx.toFloat(), cy.toFloat(), 0.8f, 0.8f, EffectEvent.Type.METEOR_FALL)
        )
        // 火球粒子从天上落下
        for (i in 0 until 12) {
            particles.addLast(
                Particle(
                    x = cx.toFloat() + Random.nextFloat() - 0.5f,
                    y = Random.nextFloat() * -2f, // 从地图上方外
                    vx = Random.nextFloat() * 4f - 2f,
                    vy = 18f + Random.nextFloat() * 6f,
                    life = 1f,
                    maxLife = 1f,
                    color = 0xFFFFA500.toInt(),
                    size = 2f + Random.nextFloat() * 2f,
                    gravity = 8f
                )
            )
        }
    }

    private fun spawnFireParticles(cx: Float, cy: Float, count: Int) {
        repeat(count) {
            particles.addLast(
                Particle(
                    x = cx + Random.nextFloat() - 0.5f,
                    y = cy + Random.nextFloat() - 0.5f,
                    vx = Random.nextFloat() * 2f - 1f,
                    vy = -2f - Random.nextFloat() * 3f, // 向上飘
                    life = 0.8f + Random.nextFloat() * 0.6f,
                    maxLife = 1.4f,
                    color = 0xFFFF4500.toInt(),
                    size = 2f + Random.nextFloat() * 2f
                )
            )
        }
    }

    private fun damageArea(cx: Int, cy: Int, radius: Int, amount: Int) {
        world.entities.forEach { e ->
            val dx = e.cellX() - cx
            val dy = e.cellY() - cy
            if (dx * dx + dy * dy <= radius * radius) {
                if (e.damage(amount.toFloat())) {
                    // 死亡时如果是 HUMAN，留点粒子
                    if (e.type == EntityType.HUMAN) {
                        particles.addLast(
                            Particle(
                                x = e.x, y = e.y,
                                vx = Random.nextFloat() * 4f - 2f,
                                vy = -3f,
                                life = 1f, maxLife = 1f,
                                color = 0xFFFF8888.toInt(),
                                size = 2f
                            )
                        )
                    }
                }
            }
        }
    }

    fun updateEffects(dt: Float) {
        // 推进事件
        val it = events.iterator()
        while (it.hasNext()) {
            val ev = it.next()
            ev.life -= dt
            if (ev.life <= 0f) it.remove()
            else if (ev.type == EffectEvent.Type.VOLCANO) {
                if (Random.nextFloat() < 0.4f) spawnFireParticles(ev.x, ev.y, 3)
            } else if (ev.type == EffectEvent.Type.HOLY) {
                if (Random.nextFloat() < 0.6f) {
                    particles.addLast(
                        Particle(
                            x = ev.x + Random.nextFloat() * 6f - 3f,
                            y = ev.y + Random.nextFloat() * 6f - 3f,
                            vx = 0f, vy = 0.4f,
                            life = 0.6f, maxLife = 0.6f,
                            color = 0xFFFFFF00.toInt(),
                            size = 2f
                        )
                    )
                }
            }
        }
        // 推进粒子
        val ip = particles.iterator()
        while (ip.hasNext()) {
            val p = ip.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += p.gravity * dt
            p.life -= dt
            if (p.life <= 0f) ip.remove()
        }
    }
}