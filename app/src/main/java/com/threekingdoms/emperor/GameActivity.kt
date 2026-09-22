package com.threekingdoms.emperor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.threekingdoms.emperor.data.EndingType
import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.GameStateRepository
import com.threekingdoms.emperor.data.Memorial
import com.threekingdoms.emperor.data.Minister
import com.threekingdoms.emperor.data.SaveManager
import com.threekingdoms.emperor.engine.TimeSystem
import com.threekingdoms.emperor.ui.ActionMenuView
import com.threekingdoms.emperor.ui.ScenePanelView

class GameActivity : AppCompatActivity(), ActionMenuView.Listener {

    private lateinit var state: GameState
    private lateinit var tvTitle: TextView
    private lateinit var tvGold: TextView
    private lateinit var tvMorale: TextView
    private lateinit var tvStamina: TextView
    private lateinit var rvMinisters: RecyclerView
    private lateinit var rvMemorials: RecyclerView
    private lateinit var sceneView: ScenePanelView
    private lateinit var actionMenu: ActionMenuView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = GameStateRepository.state ?: run {
            finish(); return
        }

        setContentView(R.layout.activity_game)

        tvTitle = findViewById(R.id.tvTitle)
        tvGold = findViewById(R.id.tvGold)
        tvMorale = findViewById(R.id.tvMorale)
        tvStamina = findViewById(R.id.tvStamina)
        rvMinisters = findViewById(R.id.rvMinisters)
        rvMemorials = findViewById(R.id.rvMemorials)
        sceneView = findViewById(R.id.sceneView)
        actionMenu = findViewById(R.id.actionMenu)
        sceneView.setScenarioId(GameStateRepository.scenarioId)
        actionMenu.listener = this

        findViewById<Button>(R.id.btnNextHour).setOnClickListener { nextHourClicked() }
        findViewById<Button>(R.id.btnDiplomacy).setOnClickListener {
            startActivity(Intent(this, DiplomacyActivity::class.java))
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveGame() }

        rvMinisters.layoutManager = LinearLayoutManager(this)
        rvMemorials.layoutManager = LinearLayoutManager(this)
        refreshAll()
        checkEnding()
    }

    override fun onResume() {
        super.onResume()
        state = GameStateRepository.state ?: run { finish(); return }
        refreshAll()
    }

    override fun onPause() {
        super.onPause()
        // 自动存档
        if (GameStateRepository.state != null) {
            SaveManager.autoSave(this, GameStateRepository.state!!)
        }
    }

    private fun nextHourClicked() {
        val result = TimeSystem.advance(state)
        Toast.makeText(this, "推进至 ${result.timeStr}", Toast.LENGTH_SHORT).show()

        if (result.aiWarning != null) {
            Toast.makeText(this, "⚔ ${result.aiWarning}", Toast.LENGTH_LONG).show()
        }

        if (result.event != null) {
            startActivity(Intent(this, EventDialogActivity::class.java))
        }

        refreshAll()
        checkEnding()
    }

    private fun saveGame() {
        AlertDialog.Builder(this)
            .setTitle("保存存档")
            .setItems(arrayOf("存档 1", "存档 2", "存档 3")) { _, which ->
                SaveManager.save(this, which, state)
                Toast.makeText(this, "已保存到存档 ${which + 1}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun checkEnding() {
        if (state.ending != EndingType.NONE) {
            GameStateRepository.pendingEnding = state.ending
            startActivity(Intent(this, EndingActivity::class.java))
            finish()
        }
    }

    private fun refreshAll() {
        tvTitle.text = TimeSystem.formatTime(state.emperor)
        tvGold.text = "💰 ${state.gold}"
        tvMorale.text = "👥 ${state.peopleMorale}"
        tvStamina.text = "⚡ ${state.emperor.stamina}"

        rvMinisters.adapter = MinisterAdapter(state.ministers) { minister ->
            startActivity(MinisterDetailActivity.intent(this, minister.id))
        }
        rvMemorials.adapter = MemorialAdapter(state.memorials) { memorial ->
            startActivity(MemorialDetailActivity.intent(this, memorial.id))
        }
    }

    override fun onCouncil() {
        if (state.memorials.isEmpty()) {
            Toast.makeText(this, "今日无奏折", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(MemorialDetailActivity.intent(this, state.memorials.first().id))
    }

    override fun onAudience() {
        if (state.ministers.isEmpty()) {
            Toast.makeText(this, "无可召见臣子", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(MinisterDetailActivity.intent(this, state.ministers.first().id))
    }

    override fun onMap() {
        startActivity(Intent(this, MapActivity::class.java))
    }

    override fun onCampaign() {
        Toast.makeText(this, "请先打开地图选郡", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MapActivity::class.java))
    }

    override fun onHarem() {
        startActivity(Intent(this, HaremActivity::class.java))
    }

    // ====================== Adapter ======================

    class MinisterAdapter(
        private val items: List<Minister>,
        private val onClick: (Minister) -> Unit
    ) : RecyclerView.Adapter<MinisterAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvName)
            val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_minister, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = items[position]
            holder.tvName.text = m.name
            holder.tvTitle.text = "${m.title} · 忠${m.loyalty}"
            holder.itemView.setOnClickListener { onClick(m) }
        }

        override fun getItemCount() = items.size
    }

    class MemorialAdapter(
        private val items: List<Memorial>,
        private val onClick: (Memorial) -> Unit
    ) : RecyclerView.Adapter<MemorialAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTitle: TextView = v.findViewById(R.id.tvTitle)
            val tvSubtitle: TextView = v.findViewById(R.id.tvSubtitle)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_memorial, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = items[position]
            holder.tvTitle.text = m.title
            val typeStr = m.type.label
            val ministerName = m.ministerId
            holder.tvSubtitle.text = "$typeStr · $ministerName"
            holder.itemView.setOnClickListener { onClick(m) }
        }

        override fun getItemCount() = items.size
    }
}