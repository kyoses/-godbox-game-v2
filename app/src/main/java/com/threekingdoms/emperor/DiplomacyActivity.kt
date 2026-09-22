package com.threekingdoms.emperor

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.threekingdoms.emperor.data.Force
import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.GameStateRepository
import com.threekingdoms.emperor.engine.DiplomacySystem

/**
 * 外交系统 UI。
 */
class DiplomacyActivity : AppCompatActivity() {

    private lateinit var state: GameState
    private lateinit var forcesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = GameStateRepository.state ?: run { finish(); return }
        setContentView(R.layout.activity_diplomacy)

        forcesContainer = findViewById(R.id.forcesContainer)

        for (force in state.forces) {
            if (force.id == state.emperor.force) continue
            addForceCard(force)
        }
    }

    private fun addForceCard(force: Force) {
        val view = layoutInflater.inflate(R.layout.item_force_diplomacy, forcesContainer, false)

        view.findViewById<TextView>(R.id.tvForceName).text = force.name
        val rel = force.relationshipTo(state.emperor.force)
        view.findViewById<TextView>(R.id.tvRelationship).text = "关系：$rel"
        view.findViewById<TextView>(R.id.tvTreasury).text = "国库：${force.treasury}"

        view.findViewById<Button>(R.id.btnGift).setOnClickListener {
            toast(DiplomacySystem.apply(state, force.id, DiplomacySystem.Action.SEND_GIFT))
            recreate()
        }
        view.findViewById<Button>(R.id.btnAlly).setOnClickListener {
            toast(DiplomacySystem.apply(state, force.id, DiplomacySystem.Action.PROPOSE_ALLIANCE))
            recreate()
        }
        view.findViewById<Button>(R.id.btnWar).setOnClickListener {
            toast(DiplomacySystem.apply(state, force.id, DiplomacySystem.Action.DECLARE_WAR))
            recreate()
        }

        forcesContainer.addView(view)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}