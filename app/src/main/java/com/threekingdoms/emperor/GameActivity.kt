package com.threekingdoms.emperor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.GameStateRepository
import com.threekingdoms.emperor.data.Memorial
import com.threekingdoms.emperor.data.Minister
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

        findViewById<Button>(R.id.btnNextHour).setOnClickListener {
            val t = TimeSystem.advance(state)
            Toast.makeText(this, "推进至 $t", Toast.LENGTH_SHORT).show()
            refreshAll()
        }

        rvMinisters.layoutManager = LinearLayoutManager(this)
        rvMemorials.layoutManager = LinearLayoutManager(this)
        refreshAll()
    }

    override fun onResume() {
        super.onResume()
        state = GameStateRepository.state ?: run { finish(); return }
        refreshAll()
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
        // 默认进入第一条奏折
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