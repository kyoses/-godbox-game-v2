package com.threekingdoms.emperor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.GameStateRepository
import com.threekingdoms.emperor.data.Prefecture
import com.threekingdoms.emperor.ui.MapView

class MapActivity : AppCompatActivity(), MapView.OnPrefectureClickListener {

    private lateinit var state: GameState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = GameStateRepository.state ?: run { finish(); return }
        setContentView(R.layout.activity_map)
        findViewById<MapView>(R.id.mapView).apply {
            setPrefectures(state.prefectures)
            setPlayerForce(state.emperor.force)
            listener = this@MapActivity
        }
    }

    override fun onPrefectureClick(prefecture: Prefecture) {
        // 我方郡：内政管理
        if (prefecture.ownerForce == state.emperor.force) {
            startActivity(Intent(this, PrefectureManageActivity::class.java)
                .putExtra(PrefectureManageActivity.EXTRA_PREFECTURE_ID, prefecture.id))
        } else {
            // 敌方郡：亲征
            startActivity(Intent(this, BattleActivity::class.java)
                .putExtra(BattleActivity.EXTRA_PREFECTURE_ID, prefecture.id))
        }
    }
}