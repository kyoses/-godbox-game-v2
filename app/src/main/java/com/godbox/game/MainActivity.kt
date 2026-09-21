package com.godbox.game

import android.content.res.Configuration
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.godbox.game.engine.GameView
import com.godbox.game.ui.ToolBarView

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var toolbar: ToolBarView
    private lateinit var hud: TextView

    private val hudRunnable = object : Runnable {
        override fun run() {
            hud.text = buildString {
                append("人口 ${gameView.world.population()}  ")
                append("生物 ${gameView.world.entities.size}  ")
                append("部落 ${gameView.world.civilizations.size}  ")
                append("工具 ${toolName(gameView.toolState.category)}")
            }
            hud.postDelayed(this, 250)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        gameView = findViewById(R.id.gameView)
        toolbar = findViewById(R.id.toolbar)
        hud = findViewById(R.id.hud)

        toolbar.toolState = gameView.toolState
        toolbar.onPauseToggle = {
            gameView.world.paused = !gameView.world.paused
        }
        toolbar.onClear = {
            // 清空世界后立即重新初始化
            gameView.world.entities.clear()
            gameView.world.civilizations.clear()
            gameView.world.disaster.events.clear()
            gameView.world.disaster.particles.clear()
            gameView.world.weather.raindrops.clear()
            gameView.world.init()
        }
        toolbar.rebuildSub(gameView.toolState.category)
    }

    override fun onResume() {
        super.onResume()
        hud.post(hudRunnable)
    }

    override fun onPause() {
        super.onPause()
        hud.removeCallbacks(hudRunnable)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 不重建 Activity，GameView 会通过 surfaceChanged 收到新尺寸
    }

    private fun toolName(c: com.godbox.game.input.ToolCategory): String = when (c) {
        com.godbox.game.input.ToolCategory.TERRAIN -> "地形"
        com.godbox.game.input.ToolCategory.ENTITY -> "生物"
        com.godbox.game.input.ToolCategory.DISASTER -> "灾害"
        com.godbox.game.input.ToolCategory.GOD_HAND -> "上帝之手"
        com.godbox.game.input.ToolCategory.PAUSE -> "暂停"
        com.godbox.game.input.ToolCategory.CLEAR -> "清除"
        com.godbox.game.input.ToolCategory.NONE -> "-"
    }
}