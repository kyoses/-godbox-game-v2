package com.threekingdoms.emperor.data

/**
 * 皇帝数据。包含六维属性 + 时间。
 */
data class Emperor(
    val name: String,
    val dynasty: String,
    val force: String,
    var capitalPrefecture: String,
    var year: Int,
    var month: Int,
    var day: Int,
    var shichen: Int,
    var power: Int,
    var intellect: Int,
    var leadership: Int,
    var benevolence: Int,
    var authority: Int,
    var charisma: Int,
    var stamina: Int = 100
) {
    companion object {
        val SHICHEN_NAMES = arrayOf("卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

        fun shichenName(idx: Int): String = SHICHEN_NAMES[idx.coerceIn(0, 8)]
    }
}