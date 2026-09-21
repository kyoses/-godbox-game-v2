package com.threekingdoms.emperor.engine

import com.threekingdoms.emperor.data.Emperor
import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.GameStateRepository

/**
 * 时间推进系统：每个时辰消耗体力，满 9 进入下一天，满 30 进入下个月。
 */
object TimeSystem {

    fun advance(state: GameState): String {
        state.emperor.shichen++
        state.emperor.stamina = (state.emperor.stamina - 3).coerceAtLeast(0)
        state.turnCount++

        if (state.emperor.shichen >= 9) {
            state.emperor.shichen = 0
            state.emperor.day++
            MemorialSystem.regenerate(state, count = 3)
        }
        if (state.emperor.day > 30) {
            state.emperor.day = 1
            state.emperor.month++
        }
        if (state.emperor.month > 12) {
            state.emperor.month = 1
            state.emperor.year++
        }

        // 自动恢复体力（睡眠效果：戌时入睡）
        if (state.emperor.shichen == 7) { // 戌时
            state.emperor.stamina = (state.emperor.stamina + 30).coerceAtMost(100)
        }

        GameStateRepository.state = state
        return formatTime(state.emperor)
    }

    fun formatTime(emp: Emperor): String {
        val dyn = emp.dynasty
        val year = emp.year
        val mon = emp.month
        val day = emp.day
        val shi = Emperor.shichenName(emp.shichen)
        return "$dyn·建安${year}年${mon}月${day}日 $shi 时"
    }
}