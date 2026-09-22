package com.threekingdoms.emperor

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.GameStateRepository
import com.threekingdoms.emperor.data.Minister
import com.threekingdoms.emperor.data.Prefecture

/**
 * 我方郡内政管理。
 */
class PrefectureManageActivity : AppCompatActivity() {

    private lateinit var state: GameState
    private lateinit var prefecture: Prefecture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = GameStateRepository.state ?: run { finish(); return }
        val prefId = intent.getStringExtra(EXTRA_PREFECTURE_ID)
        prefecture = state.prefectureById(prefId ?: "")
            ?: run { finish(); return }

        setContentView(R.layout.activity_prefecture_mgmt)

        findViewById<TextView>(R.id.tvName).text = prefecture.name
        findViewById<TextView>(R.id.tvRegion).text = prefecture.region
        findViewById<TextView>(R.id.tvTroops).text = "兵力：${prefecture.troops}"
        findViewById<TextView>(R.id.tvMorale).text = "民心：${prefecture.morale}"
        findViewById<TextView>(R.id.tvTax).text = "税收：${prefecture.tax}"
        findViewById<TextView>(R.id.tvGovernor).text = "太守：${
            prefecture.governor?.let { id -> state.ministerById(id)?.name ?: "无" } ?: "无"
        }"

        findViewById<Button>(R.id.btnTaxUp).setOnClickListener {
            state.gold += 500
            prefecture.morale = (prefecture.morale - 5).coerceAtLeast(0)
            Toast.makeText(this, "${prefecture.name} 加税 +500", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<Button>(R.id.btnRelief).setOnClickListener {
            if (state.gold >= 500) {
                state.gold -= 500
                prefecture.morale = (prefecture.morale + 10).coerceAtMost(100)
                prefecture.tax += 100
                Toast.makeText(this, "${prefecture.name} 赈灾成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "国库不足", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
        findViewById<Button>(R.id.btnRecruit).setOnClickListener {
            if (state.gold >= 1000) {
                state.gold -= 1000
                prefecture.troops += 1000
                prefecture.morale = (prefecture.morale - 2).coerceAtLeast(0)
                Toast.makeText(this, "${prefecture.name} 募兵 +1000", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "国库不足", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
        findViewById<Button>(R.id.btnChangeGov).setOnClickListener {
            showChangeGovernorDialog()
        }
    }

    private fun showChangeGovernorDialog() {
        val container = findViewById<LinearLayout>(R.id.ministerList)
        container.removeAllViews()
        container.visibility = View.VISIBLE

        for (m in state.ministers) {
            if (m.status != com.threekingdoms.emperor.data.MinisterStatus.IN_OFFICE) continue
            val tv = TextView(this).apply {
                text = "${m.name} · ${m.title}"
                textSize = 16f
                setPadding(16, 12, 16, 12)
                setTextColor(0xFFF5E6C8.toInt())
                setBackgroundResource(R.drawable.btn_round_gold)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
                layoutParams = lp
                setOnClickListener {
                    prefecture.governor = m.id
                    Toast.makeText(this@PrefectureManageActivity,
                        "${prefecture.name} 太守改为 ${m.name}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            container.addView(tv)
        }
        if (container.childCount == 0) {
            val tv = TextView(this).apply { text = "无可用臣子" }
            container.addView(tv)
        }
    }

    companion object {
        const val EXTRA_PREFECTURE_ID = "prefecture_id"
    }
}