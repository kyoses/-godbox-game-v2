package com.godbox.game.engine

import kotlin.math.abs
import kotlin.random.Random

/**
 * 世界状态：地图 + 实体 + 文明 + 天气 + 灾害。
 */
class World(val width: Int = 80, val height: Int = 80) {

    val cells: Array<Cell> = Array(width * height) { Cell() }
    val entities: MutableList<Entity> = mutableListOf()
    val civilizations: MutableList<Civilization> = mutableListOf()
    val weather: WeatherSystem = WeatherSystem()
    val disaster: DisasterSystem = DisasterSystem(this)

    var paused: Boolean = false
    private var entityIdCounter: Long = 1
    private var civIdCounter: Int = 0
    private var civCheckTimer: Float = 0f

    fun inBounds(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    fun index(x: Int, y: Int): Int = y * width + x

    fun cellAt(x: Int, y: Int): Cell = cells[index(x, y)]

    fun cellAtSafe(x: Int, y: Int): Cell? = if (inBounds(x, y)) cellAt(x, y) else null

    fun entitiesAt(x: Int, y: Int): List<Entity> = entities.filter {
        it.cellX() == x && it.cellY() == y
    }

    fun hasEntityAt(x: Int, y: Int): Boolean = entities.any {
        it.cellX() == x && it.cellY() == y
    }

    fun init() {
        generateTerrain()
        spawnInitialEntities()
    }

    private fun generateTerrain() {
        // 简单噪声：基于距离中心的偏移
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) * 0.35f

        for (x in 0 until width) {
            for (y in 0 until height) {
                val d = kotlin.math.sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toDouble()).toFloat()
                val n = pseudoNoise(x, y)
                val t = (d / r) + n * 0.4f
                val cell = cellAt(x, y)
                cell.heightJitter = (Random.nextFloat() - 0.5f) * 0.3f
                cell.moisture = ((n + 1f) * 0.5f).coerceIn(0f, 1f)

                cell.terrain = when {
                    t < 0.5f && n > 0.1f -> TerrainType.MOUNTAIN
                    t < 0.7f && n > -0.2f -> TerrainType.GRASS
                    t < 0.85f -> TerrainType.DIRT
                    t < 1.05f -> TerrainType.SAND
                    else -> TerrainType.WATER
                }
            }
        }
        // 在草地上随机种树
        for (x in 0 until width) {
            for (y in 0 until height) {
                val c = cellAt(x, y)
                if (c.terrain == TerrainType.GRASS && Random.nextFloat() < 0.06f) {
                    c.terrain = TerrainType.TREE
                }
            }
        }
    }

    private fun pseudoNoise(x: Int, y: Int): Float {
        val v = ((x * 374761393) xor (y * 668265263)).toLong()
        val r = (v and 0xFFFF).toInt()
        return (r / 65536f) * 2f - 1f
    }

    private fun spawnInitialEntities() {
        repeat(30) { spawnEntityRandom(EntityType.SHEEP) }
        repeat(5) { spawnEntityRandom(EntityType.WOLF) }
        repeat(10) { spawnEntityRandom(EntityType.HUMAN) }
        repeat(8) {
            // 鱼放在水里
            for (attempt in 0 until 20) {
                val x = Random.nextInt(width); val y = Random.nextInt(height)
                if (cellAt(x, y).terrain == TerrainType.WATER) {
                    spawnEntity(EntityType.FISH, x, y)
                    break
                }
            }
        }
    }

    private fun spawnEntityRandom(type: EntityType) {
        for (attempt in 0 until 30) {
            val x = Random.nextInt(width); val y = Random.nextInt(height)
            val cell = cellAt(x, y)
            if (cell.terrain.isPassable() && cell.terrain != TerrainType.WATER && !hasEntityAt(x, y)) {
                spawnEntity(type, x, y)
                return
            }
        }
    }

    fun spawnEntity(type: EntityType, x: Int, y: Int, civId: Int = -1): Entity {
        val e = Entity(
            id = entityIdCounter++,
            type = type,
            x = x.toFloat(),
            y = y.toFloat(),
            spawnX = x,
            spawnY = y
        )
        e.civId = civId
        entities.add(e)
        return e
    }

    fun update(dt: Float) {
        if (paused) return
        // 实体更新
        entities.forEach { EntityAI.update(it, this, dt) }
        // 移除死亡
        val dead = entities.filter { it.isDead() }
        if (dead.isNotEmpty()) entities.removeAll(dead)

        // 天气
        weather.update(dt, width, height)

        // 灾害效果
        disaster.updateEffects(dt)

        // 文明检测
        civCheckTimer += dt
        if (civCheckTimer > 2.0f) {
            civCheckTimer = 0f
            detectCivilizations()
            updateCivilizationExpansion()
        }

        // 部落战争检测
        detectWars()
    }

    private fun detectCivilizations() {
        // 收集所有 HUMAN 索引
        val humans = entities.filter { it.type == EntityType.HUMAN }
        if (humans.size < 5) return
        val n = humans.size
        val uf = UnionFind(n)

        for (i in humans.indices) {
            val a = humans[i]
            for (j in humans.indices) {
                if (i >= j) continue
                val b = humans[j]
                val dx = a.cellX() - b.cellX()
                val dy = a.cellY() - b.cellY()
                if (abs(dx) <= 3 && abs(dy) <= 3) uf.union(i, j)
            }
        }

        val groups = uf.groups()
        for ((_, memberList) in groups) {
            if (memberList.size >= 5) {
                // 找一个未分配文明的组
                val sample = humans[memberList.first()]
                if (sample.civId >= 0) {
                    // 已加入：确保成员都标了相同 civId
                    for (idx in memberList) {
                        val e = humans[idx]
                        if (e.civId < 0) e.civId = sample.civId
                    }
                    continue
                }
                // 新建部落
                val centerX = (memberList.map { humans[it].cellX() }).average().toInt()
                val centerY = (memberList.map { humans[it].cellY() }).average().toInt()
                val newId = civIdCounter++
                val civ = Civilization(newId, centerX, centerY)
                civilizations.add(civ)
                for (idx in memberList) {
                    val e = humans[idx]
                    e.civId = newId
                    civ.members.add(e.id)
                }
                // 标记 30% 为战士
                for (idx in memberList) {
                    if (Random.nextFloat() < 0.3f) humans[idx].isFighter = true
                }
            }
        }

        // 清理空部落
        civilizations.removeAll { it.memberCount() == 0 }
    }

    private fun updateCivilizationExpansion() {
        for (civ in civilizations) {
            if (Random.nextFloat() < 0.05f) {
                // 中心 3x3 随机 2 格设为 VILLAGE
                val spots = mutableListOf<Pair<Int, Int>>()
                for (dx in -1..1) for (dy in -1..1) {
                    val nx = civ.centerX + dx
                    val ny = civ.centerY + dy
                    if (inBounds(nx, ny)) spots.add(nx to ny)
                }
                if (spots.size >= 2) {
                    spots.shuffled().take(2).forEach { (x, y) ->
                        val c = cellAt(x, y)
                        if (c.terrain != TerrainType.WATER && c.terrain != TerrainType.MOUNTAIN) {
                            c.terrain = TerrainType.VILLAGE
                            c.civId = civ.id
                        }
                    }
                }
            }
        }
    }

    private fun detectWars() {
        // 邻接不同部落 → 互相加敌人
        for (civA in civilizations) {
            val enemiesA = civA.members.mapNotNull { id -> entities.find { it.id == id } }
            for (civB in civilizations) {
                if (civB.id <= civA.id) continue
                val enemiesB = civB.members.mapNotNull { id -> entities.find { it.id == id } }
                var adjacent = false
                for (a in enemiesA) {
                    for (b in enemiesB) {
                        val dx = a.cellX() - b.cellX()
                        val dy = a.cellY() - b.cellY()
                        if (abs(dx) <= 1 && abs(dy) <= 1) {
                            adjacent = true; break
                        }
                    }
                    if (adjacent) break
                }
                if (adjacent) {
                    civA.enemies.add(civB.id)
                    civB.enemies.add(civA.id)
                }
            }
        }
    }

    fun population(): Int = entities.count { it.type == EntityType.HUMAN }
}