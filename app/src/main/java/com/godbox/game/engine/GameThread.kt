package com.godbox.game.engine

/**
 * 固定 30fps 主循环。
 */
class GameThread(
    private val surfaceView: GameView
) : Thread("Godbox-GameLoop") {

    @Volatile var running: Boolean = false
    private val targetFrameMs: Long = 33L

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f) // 上限 50ms 防止跳帧后灾难
            last = now
            try {
                surfaceView.tick(dt)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            val elapsed = (System.nanoTime() - now) / 1_000_000
            val ms = targetFrameMs - elapsed
            if (ms > 0) {
                try {
                    sleep(ms)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }
}