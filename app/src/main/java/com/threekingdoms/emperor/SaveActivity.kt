package com.threekingdoms.emperor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.threekingdoms.emperor.data.GameStateRepository
import com.threekingdoms.emperor.data.SaveManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 启动存档选择：3 个槽位 + 新游戏按钮。
 */
class SaveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save)

        val container = findViewById<LinearLayout>(R.id.saveContainer)
        container.removeAllViews()

        val inflater = LayoutInflater.from(this)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

        for (slot in SaveManager.listSlots(this)) {
            val card = inflater.inflate(R.layout.item_save_slot, container, false)
            val tvSlot = card.findViewById<TextView>(R.id.tvSlot)
            val tvInfo = card.findViewById<TextView>(R.id.tvInfo)
            val btnLoad = card.findViewById<Button>(R.id.btnLoad)
            val btnDelete = card.findViewById<Button>(R.id.btnDelete)

            tvSlot.text = "存档 ${slot.id + 1}"
            if (slot.hasData) {
                tvInfo.text = "${slot.scenarioId} · ${dateFormat.format(Date(slot.time))}"
                btnLoad.isEnabled = true
                btnDelete.isEnabled = true
                btnLoad.setOnClickListener { loadSlot(slot.id) }
                btnDelete.setOnClickListener { deleteSlot(slot.id, container) }
            } else {
                tvInfo.text = "空"
                btnLoad.isEnabled = false
                btnDelete.isEnabled = false
            }
            container.addView(card)
        }

        findViewById<Button>(R.id.btnNewGame).setOnClickListener {
            startActivity(Intent(this, ScenarioSelectActivity::class.java))
        }
    }

    private fun loadSlot(id: Int) {
        val state = SaveManager.load(this, id) ?: return
        GameStateRepository.state = state
        startActivity(Intent(this, GameActivity::class.java))
        finish()
    }

    private fun deleteSlot(id: Int, container: ViewGroup) {
        SaveManager.delete(this, id)
        recreate()
    }
}