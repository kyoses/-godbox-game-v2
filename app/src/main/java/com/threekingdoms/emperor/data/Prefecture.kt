package com.threekingdoms.emperor.data

/**
 * 郡县。包含坐标（用于地图显示）和基础数据。
 */
data class Prefecture(
    val id: String,
    val name: String,
    val region: String,
    val gridX: Int,
    val gridY: Int,
    var governor: String? = null,
    var troops: Int = 5000,
    var morale: Int = 70,
    var tax: Int = 1000,
    var ownerForce: String = "汉"
) {
    /** 是否归属玩家（由 GameState 决定） */
    fun isOwnedBy(force: String): Boolean = ownerForce == force
}