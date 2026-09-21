package com.godbox.game.input

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.godbox.game.engine.EntityType
import com.godbox.game.engine.TerrainType
import com.godbox.game.engine.World

/**
 * 把触摸事件转换为：相机平移 / 缩放 / 上帝之手 / 选中 / 触发工具。
 *
 * 单击：根据 tool 触发
 * 双击：退出上帝之手
 * 长按：进入上帝之手
 * 单指拖动：相机平移
 * 双指缩放：相机缩放
 */
class InputHandler(
    context: Context,
    private val world: World,
    private val camera: Camera,
    private val toolState: ToolState,
    private val surfaceW: () -> Int,
    private val surfaceH: () -> Int,
    private val onSelectedCellChanged: (Int, Int) -> Unit,
    private val onPauseToggle: () -> Unit,
    private val onClear: () -> Unit
) {
    var selectedCellX: Int = -1
    var selectedCellY: Int = -1

    var godHandMode: Boolean = false
        private set

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            camera.zoom(detector.scaleFactor)
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (scaleDetector.isInProgress) return true
            camera.panByPixels(-dx, -dy)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val (cx, cy) = camera.screenToCell(e.x, e.y, surfaceW(), surfaceH(), world.width, world.height)
            selectedCellX = cx
            selectedCellY = cy
            onSelectedCellChanged(cx, cy)
            applyTool(cx, cy)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val (cx, cy) = camera.screenToCell(e.x, e.y, surfaceW(), surfaceH(), world.width, world.height)
            godHandMode = true
            camera.enterGodHand(cx, cy, surfaceW(), surfaceH(), world.width, world.height)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (godHandMode) {
                godHandMode = false
                camera.exitGodHand()
            }
            return true
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    fun resetGodHand() {
        godHandMode = false
        camera.exitGodHand()
    }

    private fun applyTool(cx: Int, cy: Int) {
        when (toolState.category) {
            ToolCategory.TERRAIN -> {
                val cell = world.cellAt(cx, cy)
                // 不能把水上改成树：水面不能行走
                cell.terrain = toolState.selectedTerrain
            }
            ToolCategory.ENTITY -> {
                if (!world.hasEntityAt(cx, cy)) {
                    val cell = world.cellAt(cx, cy)
                    if (cell.terrain.isPassable() ||
                        (toolState.selectedEntity == EntityType.FISH && cell.terrain == TerrainType.WATER)) {
                        world.spawnEntity(toolState.selectedEntity, cx, cy)
                    }
                }
            }
            ToolCategory.DISASTER -> {
                when (toolState.selectedDisaster) {
                    DisasterKind.METEOR -> world.disaster.meteorShower(5)
                    DisasterKind.FLOOD -> world.disaster.flood()
                    DisasterKind.VOLCANO -> world.disaster.volcano(cx, cy)
                    DisasterKind.HOLY -> world.disaster.holyLight(cx, cy)
                    DisasterKind.LIGHTNING -> world.disaster.lightning(cx, cy)
                }
            }
            ToolCategory.PAUSE -> onPauseToggle()
            ToolCategory.CLEAR -> onClear()
            ToolCategory.GOD_HAND -> {
                godHandMode = true
                camera.enterGodHand(cx, cy, surfaceW(), surfaceH(), world.width, world.height)
            }
            ToolCategory.NONE -> { /* no-op */ }
        }
    }
}