package com.threekingdoms.emperor.engine

import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.Minister
import com.threekingdoms.emperor.data.MinisterStatus

/**
 * 臣子系统：擢升、贬黜、赏赐、赐死。
 */
object MinisterSystem {

    /** 擢升：忠诚 +5，权威 -2（恃宠） */
    fun promote(state: GameState, minister: Minister) {
        minister.relation = (minister.relation + 5).coerceAtMost(100)
        minister.loyalty = (minister.loyalty + 5).coerceAtMost(100)
        state.emperor.authority = (state.emperor.authority - 2).coerceAtLeast(0)
    }

    /** 赏赐：-1000 金，忠诚 +10 */
    fun reward(state: GameState, minister: Minister) {
        state.gold -= 1000
        minister.loyalty = (minister.loyalty + 10).coerceAtMost(100)
        minister.relation = (minister.relation + 3).coerceAtMost(100)
    }

    /** 贬黜：忠诚 -15，权威 +2 */
    fun demote(state: GameState, minister: Minister) {
        minister.loyalty = (minister.loyalty - 15).coerceAtLeast(0)
        minister.relation = (minister.relation - 5).coerceAtLeast(0)
        state.emperor.authority = (state.emperor.authority + 2).coerceAtMost(100)
    }

    /** 赐死：标记为 DEAD，权威 +5，民心 -10 */
    fun execute(state: GameState, minister: Minister) {
        minister.status = MinisterStatus.DEAD
        state.emperor.authority = (state.emperor.authority + 5).coerceAtMost(100)
        state.peopleMorale = (state.peopleMorale - 10).coerceAtLeast(0)
        state.ministers.remove(minister)
    }
}