package com.threekingdoms.emperor.data

enum class GamePhase {
    SCENARIO_SELECT, MAIN,
    COUNCIL, AUDIENCE, MAP, BATTLE, HAREM
}

/**
 * 全局游戏状态。Repository 单例，跨 Activity 共享。
 */
object GameStateRepository {
    var state: GameState? = null
    var scenarioId: String = ""
    var phase: GamePhase = GamePhase.SCENARIO_SELECT

    fun reset() {
        state = null
        scenarioId = ""
        phase = GamePhase.MAIN
    }
}

data class GameState(
    var emperor: Emperor,
    val ministers: MutableList<Minister>,
    val memorials: MutableList<Memorial>,
    val prefectures: MutableList<Prefecture>,
    val consorts: MutableList<Consort>,
    var gold: Int = 10000,
    var peopleMorale: Int = 70,
    var troopTotal: Int = 50000,
    var turnCount: Int = 0
) {
    fun ministerById(id: String): Minister? = ministers.find { it.id == id }
    fun prefectureById(id: String): Prefecture? = prefectures.find { it.id == id }
}