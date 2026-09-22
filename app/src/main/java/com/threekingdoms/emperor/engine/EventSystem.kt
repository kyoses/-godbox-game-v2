package com.threekingdoms.emperor.engine

import com.threekingdoms.emperor.data.Event
import com.threekingdoms.emperor.data.GameState
import kotlin.random.Random

/**
 * 随机事件系统。
 */
object EventSystem {

    fun tryTrigger(state: GameState): Event? {
        if (Random.nextFloat() > 0.30f) return null
        return Event.POOL.random()
    }

    /**
     * 应用事件选择。
     */
    fun applyChoice(state: GameState, event: Event, choiceIndex: Int) {
        if (choiceIndex !in event.choices.indices) return
        val c = event.choices[choiceIndex]
        state.gold += c.goldEffect
        state.peopleMorale = (state.peopleMorale + c.moraleEffect).coerceIn(0, 100)
        state.emperor.stamina = (state.emperor.stamina + c.staminaEffect).coerceIn(0, 100)
        state.emperor.authority = (state.emperor.authority + c.authorityEffect).coerceIn(0, 100)
        // 全员忠诚度小幅变化
        state.ministers.forEach {
            it.loyalty = (it.loyalty + c.loyaltyEffect).coerceIn(0, 100)
        }
        // 调整与所有势力的关系
        if (c.relationshipEffect != 0) {
            state.forces.forEach { force ->
                force.adjustRelationship(state.emperor.force, c.relationshipEffect)
            }
        }
    }
}