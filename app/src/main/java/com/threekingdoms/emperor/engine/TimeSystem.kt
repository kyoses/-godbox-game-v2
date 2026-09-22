package com.threekingdoms.emperor.engine

import com.threekingdoms.emperor.data.Emperor
import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.GameStateRepository

/**
 * 时间推进系统：每个时辰消耗体力，满 9 进入下一天。
 */
object TimeSystem {

    var pendingEvent: com.threekingdoms.emperor.data.Event? = null
    var aiAttackWarning: String? = null

    fun advance(state: GameState): AdvanceResult {
        state.emperor.shichen++
        state.emperor.stamina = (state.emperor.stamina - 3).coerceAtLeast(0)
        state.turnCount++

        var isNewDay = false
        if (state.emperor.shichen >= 9) {
            state.emperor.shichen = 0
            state.emperor.day++
            isNewDay = true
            MemorialSystem.regenerate(state, count = 3)
            // 每天触发 AI
            AiSystem.tick(state)
            aiAttackWarning = AiSystem.checkAiAttacksPlayer(state)
        }
        if (state.emperor.day > 30) {
            state.emperor.day = 1
            state.emperor.month++
        }
        if (state.emperor.month > 12) {
            state.emperor.month = 1
            state.emperor.year++
        }

        // 戌时入睡恢复体力
        if (state.emperor.shichen == 7) {
            state.emperor.stamina = (state.emperor.stamina + 30).coerceAtMost(100)
        }

        // 30% 概率触发事件
        if (isNewDay || state.turnCount % 2 == 0) {
            pendingEvent = EventSystem.tryTrigger(state)
        } else {
            pendingEvent = null
        }

        // 结局判定
        val ending = EndingSystem.check(state)
        if (ending != com.threekingdoms.emperor.data.EndingType.NONE) {
            state.ending = ending
        }

        GameStateRepository.state = state
        return AdvanceResult(formatTime(state.emperor), pendingEvent, aiAttackWarning,
            state.ending, isNewDay)
    }

    data class AdvanceResult(
        val timeStr: String,
        val event: com.threekingdoms.emperor.data.Event?,
        val aiWarning: String?,
        val ending: com.threekingdoms.emperor.data.EndingType,
        val isNewDay: Boolean
    )

    fun formatTime(emp: Emperor): String {
        val dyn = emp.dynasty
        val year = emp.year
        val mon = emp.month
        val day = emp.day
        val shi = Emperor.shichenName(emp.shichen)
        return "$dyn·建安${year}年${mon}月${day}日 $shi 时"
    }
}