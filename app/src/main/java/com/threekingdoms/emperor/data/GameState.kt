package com.threekingdoms.emperor.data

enum class GamePhase {
    SCENARIO_SELECT, MAIN,
    COUNCIL, AUDIENCE, MAP, BATTLE, HAREM, DIPLOMACY, PREFECTURE_MGMT,
    ENDING
}

/**
 * 结局类型
 */
enum class EndingType(val title: String, val description: String) {
    NONE("继续", ""),
    BAD_HEALTH("陛下驾崩", "陛下因积劳成疾，龙驭宾天。"),
    ASSASSINATION("帝崩于叛", "宫中叛变，陛下为乱臣所弑。"),
    UNIFICATION("一统天下", "陛下削平群雄，天下归一。"),
    ABDICATION("禅让内禅", "陛下禅位于贤臣。")
}

object GameStateRepository {
    var state: GameState? = null
    var scenarioId: String = ""
    var phase: GamePhase = GamePhase.SCENARIO_SELECT
    var pendingEnding: EndingType = EndingType.NONE

    fun reset() {
        state = null
        scenarioId = ""
        phase = GamePhase.MAIN
        pendingEnding = EndingType.NONE
    }
}

data class GameState(
    var emperor: Emperor,
    val ministers: MutableList<Minister>,
    val memorials: MutableList<Memorial>,
    val prefectures: MutableList<Prefecture>,
    val consorts: MutableList<Consort>,
    val forces: MutableList<Force> = mutableListOf(),
    var gold: Int = 10000,
    var peopleMorale: Int = 70,
    var troopTotal: Int = 50000,
    var turnCount: Int = 0,
    var ending: EndingType = EndingType.NONE
) {
    fun ministerById(id: String): Minister? = ministers.find { it.id == id }
    fun prefectureById(id: String): Prefecture? = prefectures.find { it.id == id }
    fun consortById(id: String): Consort? = consorts.find { it.id == id }
    fun forceById(id: String): Force? = forces.find { it.id == id }

    fun ownedPrefectureCount(forceId: String): Int =
        prefectures.count { it.ownerForce == forceId }

    /** 我方郡数 */
    val ownedCount: Int get() = ownedPrefectureCount(emperor.force)

    /** 总郡数 */
    val totalPrefectures: Int get() = prefectures.size
}