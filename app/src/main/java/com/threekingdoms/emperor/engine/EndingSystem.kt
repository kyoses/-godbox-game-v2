package com.threekingdoms.emperor.engine

import com.threekingdoms.emperor.data.EndingType
import com.threekingdoms.emperor.data.GameState

/**
 * 结局判定系统。
 */
object EndingSystem {

    fun check(state: GameState): EndingType {
        if (state.ending != EndingType.NONE) return state.ending

        // 暴毙
        if (state.emperor.stamina <= 0) return EndingType.BAD_HEALTH

        // 被刺：权威高 + 忠诚低
        if (state.emperor.authority >= 95) {
            val lowLoyalty = state.ministers.count { it.loyalty < 30 }
            if (lowLoyalty >= 2) return EndingType.ASSASSINATION
        }

        // 一统天下
        val ownedRatio = state.ownedCount.toFloat() / state.totalPrefectures
        if (ownedRatio >= 0.9f) return EndingType.UNIFICATION

        return EndingType.NONE
    }
}