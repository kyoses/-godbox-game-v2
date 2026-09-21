package com.threekingdoms.emperor.data

enum class MinisterStatus { IN_OFFICE, IN_WILD, DEAD, RETIRED }

data class Minister(
    val id: String,
    val name: String,
    val title: String,
    val force: String,
    val portraitKey: String,
    var power: Int,
    var intellect: Int,
    var leadership: Int,
    var benevolence: Int,
    var loyalty: Int,
    var relation: Int,
    var status: MinisterStatus = MinisterStatus.IN_OFFICE
) {
    /** 战斗力评分 */
    val combatPower: Int get() = (power * 2 + leadership + intellect / 2)
}