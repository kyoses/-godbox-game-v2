package com.threekingdoms.emperor.engine

import com.threekingdoms.emperor.data.Consort
import com.threekingdoms.emperor.data.GameState

/**
 * 后宫系统：翻牌、赐宴、冷落。
 */
object HaremSystem {

    /** 翻牌：宠爱 +10，体力 +5 */
    fun summon(state: GameState, consort: Consort) {
        consort.favor = (consort.favor + 10).coerceAtMost(100)
        state.emperor.stamina = (state.emperor.stamina + 5).coerceAtMost(100)
    }

    /** 赐宴：所有妃嫔宠爱 +3，-500 金 */
    fun banquet(state: GameState) {
        if (state.gold < 500) return
        state.gold -= 500
        state.consorts.forEach { it.favor = (it.favor + 3).coerceAtMost(100) }
    }

    /** 冷落：宠爱 -15 */
    fun neglect(state: GameState, consort: Consort) {
        consort.favor = (consort.favor - 15).coerceAtLeast(0)
    }
}