package com.godbox.game.engine

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.godbox.game.input.Camera
import com.godbox.game.input.InputHandler
import com.godbox.game.input.ToolCategory
import com.godbox.game.input.ToolState
import com.godbox.game.render.Renderer

/**
 * SurfaceView 游戏画布。持有 World / Camera / Renderer / InputHandler。
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    val world = World(80, 80)
    val camera = Camera()
    val toolState = ToolState()
    val renderer = Renderer()

    private var thread: GameThread? = null
    private var initialized = false
    private var inputHandler: InputHandler? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        isClickable = true
        // 默认相机偏移：把世界中心推到屏幕中
        post {
            camera.offsetX = 0f
            camera.offsetY = 0f
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!initialized) {
            world.init()
            initialized = true
        }
        // 屏幕中心定位：把世界中心对齐到屏幕中心
        camera.centerOn(world.width / 2, world.height / 2, width, height,
            world.width, world.height)

        inputHandler = InputHandler(
            context = context,
            world = world,
            camera = camera,
            toolState = toolState,
            surfaceW = { width },
            surfaceH = { height },
            onSelectedCellChanged = { _, _ -> /* no-op */ },
            onPauseToggle = { world.paused = !world.paused },
            onClear = { clearWorld() }
        )

        thread = GameThread(this).also {
            it.running = true
            it.start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        // 重新居中到世界中心
        camera.centerOn(world.width / 2, world.height / 2, w, h,
            world.width, world.height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        thread?.let {
            it.running = false
            try { it.join(500) } catch (_: InterruptedException) {}
        }
        thread = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handler = inputHandler ?: return false
        return handler.onTouchEvent(event) || super.onTouchEvent(event)
    }

    fun tick(dt: Float) {
        world.update(dt)
        val canvas = holder.lockCanvas() ?: return
        try {
            val toolLabel = when (toolState.category) {
                ToolCategory.TERRAIN -> "地形:${toolState.selectedTerrain.name}"
                ToolCategory.ENTITY -> "生物:${toolState.selectedEntity.name}"
                ToolCategory.DISASTER -> "灾害:${toolState.selectedDisaster.name}"
                ToolCategory.GOD_HAND -> "上帝之手"
                ToolCategory.PAUSE -> "暂停"
                ToolCategory.CLEAR -> "清除"
                ToolCategory.NONE -> "-"
            }
            val handler = inputHandler
            renderer.render(canvas, world, camera, width, height,
                handler?.selectedCellX ?: -1,
                handler?.selectedCellY ?: -1,
                toolLabel, handler?.godHandMode ?: false)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun clearWorld() {
        world.entities.clear()
        world.civilizations.clear()
        world.disaster.events.clear()
        world.disaster.particles.clear()
        world.weather.raindrops.clear()
        // 重置地形为草地
        for (c in world.cells) {
            c.terrain = com.godbox.game.engine.TerrainType.GRASS
            c.civId = -1
        }
    }
}