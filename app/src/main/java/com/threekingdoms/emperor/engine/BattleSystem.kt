package com.threekingdoms.emperor.engine

import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.Prefecture
import kotlin.math.max
import kotlin.random.Random

/**
 * 战斗系统：文字战报。
 */
object BattleSystem {

    enum class Formation(val name: String, val attackBonus: Float, val defenseBonus: Float) {
        BATTLE_ARRAY("八阵图", 1.0f, 1.3f),
        MANDARIN_DUCK("鸳鸯阵", 1.1f, 1.1f),
        ARROW("锋矢阵", 1.4f, 0.9f),
        CRESCENT("偃月阵", 1.2f, 1.0f),
        FISH_SCALE("鱼鳞阵", 0.9f, 1.2f)
    }

    data class BattleResult(
        val won: Boolean,
        val log: List<String>,
        val attackerLoss: Int,
        val defenderLoss: Int,
        val loot: Int
    )

    fun battle(
        state: GameState,
        target: Prefecture,
        formation: Formation = Formation.BATTLE_ARRAY
    ): BattleResult {
        val log = mutableListOf<String>()
        log.add("【出征】${state.emperor.name} 亲率大军攻伐 ${target.name}！")
        log.add("【阵法】${formation.name}")

        val bestGeneral = state.ministers
            .filter { it.status == com.threekingdoms.emperor.data.MinisterStatus.IN_OFFICE }
            .maxByOrNull { it.combatPower } ?: state.ministers.first()
        log.add("【前锋】${bestGeneral.name}（战力 ${bestGeneral.combatPower}）")

        val attackerPower = bestGeneral.combatPower * formation.attackBonus
        val defenderPower = (target.troops / 100f) * (target.morale / 50f)
            * formation.defenseBonus
        log.add("【交锋】我军攻势 ${attackerPower.toInt()} vs 敌守势 ${defenderPower.toInt()}")

        var atkHp = 100
        var defHp = (target.troops / 1000).coerceAtLeast(1)
        val attackerInitialHp = atkHp
        val defenderInitialHp = defHp

        var round = 1
        while (atkHp > 0 && defHp > 0 && round <= 10) {
            val atkDamage = max(5, (attackerPower * Random.nextFloat() * 0.3f).toInt())
            val defDamage = max(2, (defenderPower * Random.nextFloat() * 0.2f).toInt())
            defHp -= atkDamage
            atkHp -= defDamage
            log.add("【第${round}合】我军伤敌 $atkDamage，敌军伤我 $defDamage")
            round++
        }

        val won = defHp <= 0
        val attackerLoss = attackerInitialHp - atkHp
        val defenderLoss = defenderInitialHp - defHp

        if (won) {
            log.add("【大胜】${target.name} 已被攻克！")
            val loot = target.tax + 500
            state.gold += loot
            target.ownerForce = state.emperor.force
            target.morale = 60
            target.troops = 2000
            target.governor = bestGeneral.id
            log.add("【缴获】金银 $loot")
        } else {
            log.add("【惜败】${target.name} 久攻不下，撤军回营。")
            log.add("【损失】我军折损 $attackerLoss 成")
        }

        return BattleResult(won, log, attackerLoss, defenderLoss,
            if (won) target.tax + 500 else 0)
    }
}