package com.godbox.game.engine

import kotlin.random.Random

/** 天气状态 */
enum class WeatherState { SUNNY, RAINING, STORM }

/**
 * 天气系统 + 雨滴粒子数据（被 Renderer 直接消费）。
 */
class WeatherSystem {
    var state: WeatherState = WeatherState.SUNNY
    private var switchTimer: Float = 30f

    // 雨滴在世界坐标中（每帧移动）。生命周期到 0 时回收。
    val raindrops: ArrayDeque<RainDrop> = ArrayDeque()
    private var dropSpawnTimer: Float = 0f

    fun update(dt: Float, worldW: Int, worldH: Int) {
        switchTimer -= dt
        if (switchTimer <= 0f) {
            switchTimer = 25f + Random.nextFloat() * 15f
            state = when (Random.nextInt(100)) {
                in 0..4 -> WeatherState.STORM
                in 5..14 -> WeatherState.RAINING
                else -> WeatherState.SUNNY
            }
            // 切回晴天清掉雨滴
            if (state == WeatherState.SUNNY) raindrops.clear()
        }

        if (state == WeatherState.RAINING || state == WeatherState.STORM) {
            dropSpawnTimer -= dt
            val spawnPerSec = if (state == WeatherState.STORM) 120f else 60f
            val spawnInterval = 1f / spawnPerSec
            while (dropSpawnTimer <= 0f) {
                raindrops.addLast(
                    RainDrop(
                        x = Random.nextFloat() * worldW,
                        y = Random.nextFloat() * worldH * 0.3f,
                        vy = 14f + Random.nextFloat() * 6f,
                        life = 1.5f
                    )
                )
                dropSpawnTimer += spawnInterval
            }
        }

        // 更新雨滴
        val it = raindrops.iterator()
        while (it.hasNext()) {
            val d = it.next()
            d.y += d.vy * dt
            d.life -= dt
            if (d.life <= 0f || d.y > worldH) it.remove()
        }
    }
}

class RainDrop(var x: Float, var y: Float, var vy: Float, var life: Float)