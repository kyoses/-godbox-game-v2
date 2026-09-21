package com.threekingdoms.emperor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.threekingdoms.emperor.data.Consort
import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.GameStateRepository
import com.threekingdoms.emperor.engine.HaremSystem

class HaremActivity : AppCompatActivity() {

    private lateinit var state: GameState
    private lateinit var rv: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = GameStateRepository.state ?: run { finish(); return }
        setContentView(R.layout.activity_harem)
        rv = findViewById(R.id.rvConsorts)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ConsortAdapter(state.consorts)
    }

    inner class ConsortAdapter(private val items: List<Consort>) :
        RecyclerView.Adapter<ConsortAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvName)
            val tvFavor: TextView = v.findViewById(R.id.tvFavor)
            val btnSummon: Button
            val btnNeglect: Button

            init {
                btnSummon = Button(v.context).apply { text = "翻牌" }
                btnNeglect = Button(v.context).apply { text = "冷落" }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_consort, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val c = items[position]
            holder.tvName.text = c.name
            holder.tvFavor.text = "❤ ${c.favor} · ${c.family}"
            holder.btnSummon.setOnClickListener {
                HaremSystem.summon(state, c)
                Toast.makeText(this@HaremActivity, "${c.name} 已侍寝", Toast.LENGTH_SHORT).show()
                rv.adapter?.notifyDataSetChanged()
            }
            holder.btnNeglect.setOnClickListener {
                HaremSystem.neglect(state, c)
                Toast.makeText(this@HaremActivity, "${c.name} 被冷落", Toast.LENGTH_SHORT).show()
                rv.adapter?.notifyDataSetChanged()
            }
        }

        override fun getItemCount() = items.size
    }
}