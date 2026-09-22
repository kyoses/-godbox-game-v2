package com.threekingdoms.emperor.engine

import com.threekingdoms.emperor.data.Force
import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.Minister

/**
 * 外交系统。
 */
object DiplomacySystem {

    enum class Action(val cost: Int, val label: String) {
        SEND_GIFT(500, "赠送"),
        PROPOSE_ALLIANCE(1000, "结盟"),
        BREAK_ALLIANCE(0, "断盟"),
        DECLARE_WAR(2000, "宣战"),
        SEND_AMBASSADOR(300, "遣使")
    }

    enum class Result { SUCCESS, FAILURE }

    fun apply(state: GameState, otherForceId: String, action: Action): String {
        val playerForceId = state.emperor.force
        val playerForce = state.forceById(playerForceId) ?: return "未找到玩家势力"
        val other = state.forceById(otherForceId) ?: return "未找到目标势力"

        if (state.gold < action.cost) {
            return "国库不足"
        }
        state.gold -= action.cost

        val msg = when (action) {
            Action.SEND_GIFT -> {
                other.adjustRelationship(playerForceId, 15)
                "${other.name} 接受赠礼，关系 +15"
            }
            Action.PROPOSE_ALLIANCE -> {
                val rel = other.relationshipTo(playerForceId)
                if (rel >= 30) {
                    other.setRelationship(playerForceId, 80)
                    playerForce.setRelationship(otherForceId, 80)
                    "与 ${other.name} 结为同盟！"
                } else {
                    other.adjustRelationship(playerForceId, 10)
                    "${other.name} 暂时婉拒，但好感提升"
                }
            }
            Action.BREAK_ALLIANCE -> {
                other.setRelationship(playerForceId, -50)
                playerForce.setRelationship(otherForceId, -50)
                "与 ${other.name} 断绝盟约"
            }
            Action.DECLARE_WAR -> {
                other.setRelationship(playerForceId, -100)
                playerForce.setRelationship(otherForceId, -100)
                "向 ${other.name} 宣战！"
            }
            Action.SEND_AMBASSADOR -> {
                other.adjustRelationship(playerForceId, 5)
                "遣使访问 ${other.name}，关系 +5"
            }
        }
        return msg
    }
}