package com.threekingdoms.emperor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.threekingdoms.emperor.data.Scenario
import com.threekingdoms.emperor.data.ScenarioLoader

class ScenarioSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scenario_select)

        val grid = findViewById<GridLayout>(R.id.gridScenarios)
        val inflater = LayoutInflater.from(this)

        for (sc in ScenarioLoader.all()) {
            val card = inflater.inflate(R.layout.item_scenario, grid, false)
            card.findViewById<TextView>(R.id.tvName).text = sc.name
            card.findViewById<TextView>(R.id.tvDynasty).text = sc.dynasty
            card.findViewById<TextView>(R.id.tvEra).text = sc.era
            card.findViewById<TextView>(R.id.tvDifficulty).text = "难度：${sc.difficulty}"

            val lp = GridLayout.LayoutParams()
            lp.width = 0
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            lp.setMargins(8, 8, 8, 8)
            card.layoutParams = lp

            card.setOnClickListener { onScenarioSelected(sc) }
            grid.addView(card)
        }
    }

    private fun onScenarioSelected(scenario: Scenario) {
        ScenarioLoader.load(scenario.id)
        startActivity(Intent(this, GameActivity::class.java))
    }
}