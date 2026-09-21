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
import com.threekingdoms.emperor.data.Memorial
import com.threekingdoms.emperor.engine.MemorialSystem

class MemorialDetailActivity : AppCompatActivity() {

    private lateinit var state: GameState
    private lateinit var memorial: Memorial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = GameStateRepository.state ?: run { finish(); return }

        val memorialId = intent.getStringExtra(EXTRA_MEMORIAL_ID)
        memorial = state.memorials.find { it.id == memorialId }
            ?: run { finish(); return }

        setContentView(R.layout.activity_memorial_detail)
        findViewById<TextView>(R.id.tvMemorialTitle).text = memorial.title
        findViewById<TextView>(R.id.tvMemorialMeta).text =
            "类型：${memorial.type.label} · 上奏：${memorial.ministerId}"
        findViewById<TextView>(R.id.tvMemorialContent).text = memorial.content
        findViewById<TextView>(R.id.tvSuggestion).text = "建议：${memorial.suggestedAction}"

        findViewById<Button>(R.id.btnApprove).setOnClickListener {
            MemorialSystem.approve(state, memorial)
            Toast.makeText(this, "已批红", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<Button>(R.id.btnHold).setOnClickListener {
            MemorialSystem.hold(state, memorial)
            Toast.makeText(this, "已留中", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<Button>(R.id.btnReject).setOnClickListener {
            MemorialSystem.reject(state, memorial)
            Toast.makeText(this, "已驳回", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        const val EXTRA_MEMORIAL_ID = "memorial_id"
        fun intent(ctx: Context, memorialId: String): Intent =
            Intent(ctx, MemorialDetailActivity::class.java)
                .putExtra(EXTRA_MEMORIAL_ID, memorialId)
    }
}