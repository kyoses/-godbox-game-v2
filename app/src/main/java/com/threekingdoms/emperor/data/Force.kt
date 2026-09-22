package com.threekingdoms.emperor.data

import android.graphics.Color

/**
 * 势力：魏/蜀/吴/群雄/匈奴/汉
 */
data class Force(
    val id: String,
    val name: String,
    val color: Int,
    var treasury: Int = 5000,
    var foodReserve: Int = 1000,
    var isAlive: Boolean = true,
    /** 对其他势力的好感度 -100~+100，key = 对方 force id */
    val relationships: MutableMap<String, Int> = mutableMapOf()
) {
    companion object {
        const val WEI = "魏"
        const val SHU = "蜀"
        const val WU = "吴"
        const val QUNXIONG = "群雄"
        const val XIONGNU = "匈奴"
        const val HAN = "汉"

        val ALL = listOf(WEI, SHU, WU, QUNXIONG, XIONGNU, HAN)

        fun colorOf(id: String): Int = when (id) {
            WEI -> Color.rgb(60, 100, 200)
            SHU -> Color.rgb(60, 160, 60)
            WU -> Color.rgb(220, 130, 60)
            QUNXIONG -> Color.rgb(180, 60, 60)
            XIONGNU -> Color.rgb(150, 100, 60)
            HAN -> Color.rgb(120, 120, 120)
            else -> Color.GRAY
        }
    }

    fun relationshipTo(otherId: String): Int = relationships[otherId] ?: 0

    fun setRelationship(otherId: String, value: Int) {
        relationships[otherId] = value.coerceIn(-100, 100)
    }

    fun adjustRelationship(otherId: String, delta: Int) {
        setRelationship(otherId, (relationships[otherId] ?: 0) + delta)
    }
}