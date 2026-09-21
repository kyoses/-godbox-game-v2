package com.threekingdoms.emperor.engine

import com.threekingdoms.emperor.data.GameState
import com.threekingdoms.emperor.data.Memorial
import com.threekingdoms.emperor.data.MemorialStatus
import com.threekingdoms.emperor.data.MemorialType

/**
 * 奏折系统：生成、批红、留中、驳回。
 */
object MemorialSystem {

    private val TEMPLATES = listOf(
        Memorial(
            id = "tpl1", type = MemorialType.TAX,
            title = "请增税以充国库", content = "今国库空虚，军饷欠发三月。",
            suggestedAction = "增商税三成", ministerId = "xunyu",
            goldEffect = 3000, moraleEffect = -5
        ),
        Memorial(
            id = "tpl2", type = MemorialType.MILITARY,
            title = "请增兵以镇边患", content = "边关告急，贼兵犯境。",
            suggestedAction = "增兵五千", ministerId = "zhangliao",
            troopEffect = 5000, goldEffect = -1000
        ),
        Memorial(
            id = "tpl3", type = MemorialType.PERSONNEL,
            title = "请擢升有功之士", content = "将士立功，恳请擢升。",
            suggestedAction = "擢升三人", ministerId = "zhugeliang",
            loyaltyEffect = 5
        ),
        Memorial(
            id = "tpl4", type = MemorialType.JUSTICE,
            title = "请严惩贪墨之徒", content = "官员贪墨赈灾钱粮。",
            suggestedAction = "抄没家产", ministerId = "liru",
            goldEffect = 1000, moraleEffect = 5
        ),
        Memorial(
            id = "tpl5", type = MemorialType.PUBLIC_WORKS,
            title = "请修缮都城水渠", content = "水渠年久失修。",
            suggestedAction = "拨银两千修缮", ministerId = "luxun",
            goldEffect = -2000, moraleEffect = 3
        ),
        Memorial(
            id = "tpl6", type = MemorialType.DIPLOMACY,
            title = "请遣使结好邻邦", content = "邻国遣使示好。",
            suggestedAction = "遣使回访", ministerId = "liru",
            goldEffect = -500, moraleEffect = 2
        ),
        Memorial(
            id = "tpl7", type = MemorialType.MILITARY,
            title = "请训练水军以图江东", content = "江东水军精锐，宜早图之。",
            suggestedAction = "募水军万人", ministerId = "zhouyu",
            troopEffect = 10000, goldEffect = -3000
        ),
        Memorial(
            id = "tpl8", type = MemorialType.TAX,
            title = "请减免灾区赋税", content = "今岁大旱，请减免灾区赋税。",
            suggestedAction = "减免三州赋税", ministerId = "xunyu",
            goldEffect = -1500, moraleEffect = 10
        )
    )

    /** 重新生成奏折（每日开始时调用） */
    fun regenerate(state: GameState, count: Int = 3) {
        state.memorials.clear()
        val available = TEMPLATES.toMutableList().apply { shuffle() }
        for (i in 0 until count) {
            val t = available[i % available.size]
            state.memorials.add(t.copy(id = "mem_${System.currentTimeMillis()}_$i"))
        }
    }

    fun approve(state: GameState, memorial: Memorial) {
        if (memorial.status != MemorialStatus.PENDING) return
        memorial.status = MemorialStatus.APPROVED
        state.gold += memorial.goldEffect
        state.peopleMorale = (state.peopleMorale + memorial.moraleEffect).coerceIn(0, 100)
        state.troopTotal += memorial.troopEffect
        state.ministerById(memorial.ministerId)?.let {
            it.loyalty = (it.loyalty + memorial.loyaltyEffect).coerceIn(0, 100)
        }
        // 批红后从列表移除（或标记为已完成）
        state.memorials.remove(memorial)
    }

    fun hold(state: GameState, memorial: Memorial) {
        if (memorial.status != MemorialStatus.PENDING) return
        memorial.status = MemorialStatus.HELD
        state.memorials.remove(memorial)
    }

    fun reject(state: GameState, memorial: Memorial) {
        if (memorial.status != MemorialStatus.PENDING) return
        memorial.status = MemorialStatus.REJECTED
        state.ministerById(memorial.ministerId)?.let {
            it.loyalty = (it.loyalty - 5).coerceAtLeast(0)
        }
        state.memorials.remove(memorial)
    }

    private fun <T> MutableList<T>.shuffle() {
        val r = java.util.Random()
        for (i in indices.reversed()) {
            val j = r.nextInt(i + 1)
            val tmp = this[i]; this[i] = this[j]; this[j] = tmp
        }
    }
}