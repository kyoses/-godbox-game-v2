package com.godbox.game.input

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.godbox.game.engine.EntityType
import com.godbox.game.engine.TerrainType
import com.godbox.game.engine.World
import kotlin.math.abs
import kotlin.math.hypot

/**
 * 直接处理 MotionEvent，不依赖 GestureDetector（避免与 ScaleGestureDetector 冲突）。
 *
 * - 单指 down + 移动距离 > touchSlop：平移相机
 * - 单指 up 时移动距离 < touchSlop：单击 → 应用当前工具
 * - 单指 down 持续 > longPressTimeout：长按 → 进入上帝之手
 * - 双指 pinch：缩放相机
 * - 双指 down 后抬起到只剩一指：不触发单击
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

    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout: Long = ViewConfiguration.getLongPressTimeout().toLong()
    private val tapTimeout: Long = ViewConfiguration.getTapTimeout().toLong()

    private var lastDownX = 0f
    private var lastDownY = 0f
    private var lastPointerX = 0f
    private var lastPointerY = 0f
    private var downTime = 0L
    private var movedBeyondSlop = false
    private var pointerCount = 0
    private var initialPinchDistance = 0f
    private var longPressFired = false

    private val handler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        if (!movedBeyondSlop && pointerCount == 1) {
            // 长按：进入上帝之手
            val (cx, cy) = camera.screenToCell(lastPointerX, lastPointerY,
                surfaceW(), surfaceH(), world.width, world.height)
            godHandMode = true
            camera.enterGodHand(cx, cy, surfaceW(), surfaceH(),
                world.width, world.height)
            longPressFired = true
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointerCount = 1
                lastDownX = event.x
                lastDownY = event.y
                lastPointerX = event.x
                lastPointerY = event.y
                downTime = System.currentTimeMillis()
                movedBeyondSlop = false
                longPressFired = false
                handler.postDelayed(longPressRunnable, longPressTimeout)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                pointerCount = event.pointerCount
                if (event.pointerCount == 2) {
                    // 取消长按
                    handler.removeCallbacks(longPressRunnable)
                    initialPinchDistance = pinchDistance(event)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    // 双指缩放
                    handler.removeCallbacks(longPressRunnable)
                    val currentDist = pinchDistance(event)
                    if (initialPinchDistance > 0f && currentDist > 0f) {
                        val factor = currentDist / initialPinchDistance
                        if (abs(factor - 1f) > 0.01f) {
                            camera.scale *= factor
                            initialPinchDistance = currentDist
                        }
                    }
                    movedBeyondSlop = true
                } else {
                    // 单指拖动
                    val dx = event.x - lastPointerX
                    val dy = event.y - lastPointerY
                    if (!movedBeyondSlop) {
                        val totalDx = event.x - lastDownX
                        val totalDy = event.y - lastDownY
                        if (abs(totalDx) > touchSlop || abs(totalDy) > touchSlop) {
                            movedBeyondSlop = true
                            handler.removeCallbacks(longPressRunnable)
                        }
                    }
                    if (movedBeyondSlop) {
                        camera.panByPixels(-dx, -dy)
                    }
                    lastPointerX = event.x
                    lastPointerY = event.y
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                pointerCount = event.pointerCount - 1
                if (pointerCount == 1) {
                    // 抬起到只剩单指：找到剩下的那根手指的位置作为新的 lastPointer
                    val remainingIndex = if (event.actionIndex == 0) 1 else 0
                    lastPointerX = event.getX(remainingIndex)
                    lastPointerY = event.getY(remainingIndex)
                    lastDownX = lastPointerX
                    lastDownY = lastPointerY
                    downTime = System.currentTimeMillis()
                    movedBeyondSlop = true  // 标记为已移动，避免单击触发
                    initialPinchDistance = 0f
                }
            }

            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                if (!movedBeyondSlop && !longPressFired) {
                    val (cx, cy) = camera.screenToCell(lastPointerX, lastPointerY,
                        surfaceW(), surfaceH(), world.width, world.height)
                    selectedCellX = cx
                    selectedCellY = cy
                    onSelectedCellChanged(cx, cy)
                    applyTool(cx, cy)
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
            }
        }
        return true
    }

    private fun pinchDistance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return hypot(dx.toDouble(), dy.toDouble()).toFloat()
    }

    fun resetGodHand() {
        godHandMode = false
        camera.exitGodHand()
    }

    private fun applyTool(cx: Int, cy: Int) {
        when (toolState.category) {
            ToolCategory.TERRAIN -> {
                val cell = world.cellAt(cx, cy)
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