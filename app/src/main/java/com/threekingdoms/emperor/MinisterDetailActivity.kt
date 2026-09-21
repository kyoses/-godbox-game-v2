package com.threekingdoms.emperor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.GameStateRepository
import com.threekingdoms.emperor.data.Minister
import com.threekingdoms.emperor.engine.MinisterSystem

class MinisterDetailActivity : AppCompatActivity() {

    private lateinit var state: GameState
    private lateinit var minister: Minister

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = GameStateRepository.state ?: run { finish(); return }

        val ministerId = intent.getStringExtra(EXTRA_MINISTER_ID)
        minister = state.ministerById(ministerId ?: "")
            ?: run { finish(); return }

        setContentView(R.layout.activity_minister_detail)
        findViewById<TextView>(R.id.tvMinisterName).text = minister.name
        findViewById<TextView>(R.id.tvMinisterTitle).text = minister.title
        findViewById<TextView>(R.id.tvMinisterStats).text =
            "武 ${minister.power} · 智 ${minister.intellect} · 统 ${minister.leadership} · 仁 ${minister.benevolence}"
        findViewById<TextView>(R.id.tvLoyalty).text = "忠诚：${minister.loyalty} · 关系：${minister.relation}"

        findViewById<Button>(R.id.btnPromote).setOnClickListener {
            MinisterSystem.promote(state, minister)
            Toast.makeText(this, "${minister.name} 已擢升", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<Button>(R.id.btnReward).setOnClickListener {
            MinisterSystem.reward(state, minister)
            Toast.makeText(this, "${minister.name} 已赏赐", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<Button>(R.id.btnDemote).setOnClickListener {
            MinisterSystem.demote(state, minister)
            Toast.makeText(this, "${minister.name} 已贬黜", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<Button>(R.id.btnExecute).setOnClickListener {
            MinisterSystem.execute(state, minister)
            Toast.makeText(this, "${minister.name} 已赐死", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        const val EXTRA_MINISTER_ID = "minister_id"
        fun intent(ctx: Context, ministerId: String): Intent =
            Intent(ctx, MinisterDetailActivity::class.java)
                .putExtra(EXTRA_MINISTER_ID, ministerId)
    }
}