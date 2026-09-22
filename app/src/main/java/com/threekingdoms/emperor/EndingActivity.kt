package com.threekingdoms.emperor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.threekingdoms.emperor.data.GameStateRepository

/**
 * 结局画面。
 */
class EndingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ending)

        val ending = GameStateRepository.pendingEnding
        findViewById<TextView>(R.id.tvTitle).text = ending.title
        findViewById<TextView>(R.id.tvDesc).text = ending.description
        findViewById<TextView>(R.id.tvStats).text = buildString {
            appendLine("剧本：${GameStateRepository.scenarioId}")
            GameStateRepository.state?.let { s ->
                appendLine("在位：${s.emperor.year}年${s.emperor.month}月${s.emperor.day}日")
                appendLine("总回合：${s.turnCount}")
                appendLine("国库：${s.gold}")
                appendLine("民心：${s.peopleMorale}")
            }
        }
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            GameStateRepository.reset()
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}