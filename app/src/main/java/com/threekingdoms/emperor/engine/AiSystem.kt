package com.threekingdoms.emperor.engine

import com.threekingdoms.emperor.data.Force
import com.threekingdoms.emperor.data.GameState
import kotlin.math.abs
import kotlin.random.Random

/**
 * AI 对手机制：每 9 时辰自动推进一次。
 *
 * 每个非玩家势力：
 * - 概率扩军
 * - 概率进攻邻郡（优先级：玩家 > 弱小邻郡）
 * - 资源管理（不破产）
 */
object AiSystem {

    private var tickCounter: Int = 0

    fun tick(state: GameState) {
        tickCounter++
        val playerForce = state.emperor.force

        // 每个非玩家势力
        for (force in state.forces) {
            if (force.id == playerForce) continue
            if (!force.isAlive) continue

            // 1) 资源管理：每月加钱
            if (tickCounter % 9 == 0) {
                force.treasury += 2000
            }

            // 2) 扩军
            if (Random.nextFloat() < 0.3f) {
                val myPrefectures = state.prefectures.filter { it.ownerForce == force.id }
                for (p in myPrefectures) {
                    p.troops += Random.nextInt(200, 500)
                    p.morale = (p.morale + Random.nextInt(-2, 3)).coerceIn(30, 95)
                }
            }

            // 3) 主动进攻
            if (Random.nextFloat() < 0.35f) {
                // 选一个目标
                val target = chooseAttackTarget(state, force) ?: continue

                val result = BattleSystem.battleByAi(force.id, target)
                if (result.won) {
                    target.ownerForce = force.id
                    target.troops = (result.defenderLoss * 1000).coerceAtLeast(2000)
                    target.morale = 60
                } else {
                    target.troops = (target.troops + 500).coerceAtMost(15000)
                }
                force.treasury -= if (result.won) 0 else 500
            }

            // 4) 外交：如果跟玩家关系极差则进一步恶化
            if (Random.nextFloat() < 0.1f) {
                force.adjustRelationship(playerForce, -5)
            }
        }
    }

    /**
     * 选择攻击目标：
     * 1. 如果 AI 跟玩家相邻，概率攻玩家
     * 2. 否则攻其他弱势力
     * 3. 没有邻郡就跳过
     */
    private fun chooseAttackTarget(state: GameState, force: Force): com.threekingdoms.emperor.data.Prefecture? {
        val myPrefectures = state.prefectures.filter { it.ownerForce == force.id }
        if (myPrefectures.isEmpty()) return null

        // 找玩家邻郡
        val playerAdj = mutableListOf<com.threekingdoms.emperor.data.Prefecture>()
        for (mine in myPrefectures) {
            for (other in state.prefectures) {
                if (other.ownerForce == state.emperor.force) {
                    if (abs(mine.gridX - other.gridX) <= 1 && abs(mine.gridY - other.gridY) <= 1) {
                        playerAdj.add(other)
                    }
                }
            }
        }
        if (playerAdj.isNotEmpty() && Random.nextFloat() < 0.4f) {
            return playerAdj.random()
        }

        // 找最弱邻郡
        val targets = mutableListOf<com.threekingdoms.emperor.data.Prefecture>()
        for (mine in myPrefectures) {
            for (other in state.prefectures) {
                if (other.ownerForce == force.id) continue
                if (abs(mine.gridX - other.gridX) <= 1 && abs(mine.gridY - other.gridY) <= 1) {
                    targets.add(other)
                }
            }
        }
        if (targets.isEmpty()) return null
        return targets.minByOrNull { it.troops + it.morale * 50 }
    }

    /** AI 攻玩家时弹事件 */
    fun checkAiAttacksPlayer(state: GameState): String? {
        for (force in state.forces) {
            if (force.id == state.emperor.force) continue
            val myPrefs = state.prefectures.filter { it.ownerForce == force.id }
            for (mine in myPrefs) {
                for (player in state.prefectures.filter { it.ownerForce == state.emperor.force }) {
                    if (abs(mine.gridX - player.gridX) <= 1 && abs(mine.gridY - player.gridY) <= 1) {
                        return "${force.name} 兵临城下，${player.name}告急！"
                    }
                }
            }
        }
        return null
    }
}