package com.threekingdoms.emperor

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.GameStateRepository
import com.threekingdoms.emperor.engine.EventSystem
import com.threekingdoms.emperor.engine.TimeSystem

/**
 * 事件弹窗：每时辰随机触发的事件。
 */
class EventDialogActivity : AppCompatActivity() {

    private lateinit var state: GameState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = GameStateRepository.state ?: run { finish(); return }
        val event = TimeSystem.pendingEvent ?: run { finish(); return }

        setContentView(R.layout.activity_event_dialog)
        findViewById<TextView>(R.id.tvTitle).text = event.title
        findViewById<TextView>(R.id.tvDesc).text = event.description

        val choicesContainer = findViewById<LinearLayout>(R.id.choices)
        choicesContainer.removeAllViews()

        for ((i, choice) in event.choices.withIndex()) {
            val btn = Button(this).apply {
                text = choice.label
                textSize = 16f
                setTextColor(0xFFF5E6C8.toInt())
                setBackgroundResource(R.drawable.btn_round_gold)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 12 }
                layoutParams = lp
                setOnClickListener {
                    EventSystem.applyChoice(state, event, i)
                    finish()
                }
            }
            choicesContainer.addView(btn)
        }
    }
}