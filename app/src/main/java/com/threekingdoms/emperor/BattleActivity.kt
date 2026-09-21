package com.threekingdoms.emperor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.GameStateRepository
import com.threekingdoms.emperor.engine.BattleSystem
import com.threekingdoms.emperor.ui.BattleView

class BattleActivity : AppCompatActivity() {

    private lateinit var state: GameState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = GameStateRepository.state ?: run { finish(); return }
        val prefId = intent.getStringExtra(EXTRA_PREFECTURE_ID)
        val target = state.prefectureById(prefId ?: "")
            ?: run { finish(); return }

        setContentView(R.layout.activity_battle)
        findViewById<TextView>(R.id.tvBattleTitle).text = "攻伐 ${target.name}"
        findViewById<BattleView>(R.id.battleView).setTarget(target)

        val tvLog = findViewById<TextView>(R.id.tvBattleLog)
        val result = BattleSystem.battle(state, target, BattleSystem.Formation.BATTLE_ARRAY)
        tvLog.text = result.log.joinToString("\n")
    }

    companion object {
        const val EXTRA_PREFECTURE_ID = "prefecture_id"
    }
}