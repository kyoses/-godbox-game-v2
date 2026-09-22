package com.threekingdoms.emperor.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 存档管理：每个槽位一个 key，存 JSON 字符串。
 */
object SaveManager {

    private const val PREFS = "yqk99_save"
    private const val META_KEY = "yqk99_save_meta"
    const val SLOT_COUNT = 3

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(ctx: Context, slotId: Int, state: GameState) {
        try {
            val json = stateToJson(state)
            prefs(ctx).edit()
                .putString("slot_$slotId", json)
                .putString("slot_${slotId}_scenario", GameStateRepository.scenarioId)
                .putLong("slot_${slotId}_time", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun load(ctx: Context, slotId: Int): GameState? {
        return try {
            val json = prefs(ctx).getString("slot_$slotId", null) ?: return null
            jsonToState(json)?.also {
                GameStateRepository.scenarioId =
                    prefs(ctx).getString("slot_${slotId}_scenario", "") ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun delete(ctx: Context, slotId: Int) {
        prefs(ctx).edit()
            .remove("slot_$slotId")
            .remove("slot_${slotId}_scenario")
            .remove("slot_${slotId}_time")
            .apply()
    }

    fun listSlots(ctx: Context): List<SaveSlot> {
        val p = prefs(ctx)
        return (0 until SLOT_COUNT).map { i ->
            val has = p.getString("slot_$i", null) != null
            val time = if (has) p.getLong("slot_${i}_time", 0L) else 0L
            val scenario = if (has) p.getString("slot_${i}_scenario", "") ?: "" else ""
            SaveSlot(i, has, time, scenario)
        }
    }

    fun hasAutoSave(ctx: Context): Boolean {
        return prefs(ctx).getString("auto_save", null) != null
    }

    fun autoSave(ctx: Context, state: GameState) {
        try {
            val json = stateToJson(state)
            prefs(ctx).edit()
                .putString("auto_save", json)
                .putString("auto_save_scenario", GameStateRepository.scenarioId)
                .putLong("auto_save_time", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAutoSave(ctx: Context): GameState? {
        return try {
            val json = prefs(ctx).getString("auto_save", null) ?: return null
            jsonToState(json)?.also {
                GameStateRepository.scenarioId =
                    prefs(ctx).getString("auto_save_scenario", "") ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class SaveSlot(val id: Int, val hasData: Boolean, val time: Long, val scenarioId: String)

    // ===================== 序列化 =====================

    private fun esc(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
    }

    private fun stateToJson(state: GameState): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"gold\":${state.gold},")
        sb.append("\"peopleMorale\":${state.peopleMorale},")
        sb.append("\"troopTotal\":${state.troopTotal},")
        sb.append("\"turnCount\":${state.turnCount},")
        sb.append("\"emperor\":${emperorToJson(state.emperor)},")
        sb.append("\"forces\":[")
        sb.append(state.forces.joinToString(",") { forceToJson(it) })
        sb.append("],")
        sb.append("\"ministers\":[")
        sb.append(state.ministers.joinToString(",") { ministerToJson(it) })
        sb.append("],")
        sb.append("\"memorials\":[")
        sb.append(state.memorials.joinToString(",") { memorialToJson(it) })
        sb.append("],")
        sb.append("\"prefectures\":[")
        sb.append(state.prefectures.joinToString(",") { prefectureToJson(it) })
        sb.append("],")
        sb.append("\"consorts\":[")
        sb.append(state.consorts.joinToString(",") { consortToJson(it) })
        sb.append("]")
        sb.append("}")
        return sb.toString()
    }

    private fun emperorToJson(e: Emperor): String {
        return "{" +
            "\"name\":\"${esc(e.name)}\"," +
            "\"dynasty\":\"${esc(e.dynasty)}\"," +
            "\"force\":\"${esc(e.force)}\"," +
            "\"capital\":\"${esc(e.capitalPrefecture)}\"," +
            "\"year\":${e.year},\"month\":${e.month},\"day\":${e.day},\"shichen\":${e.shichen}," +
            "\"power\":${e.power},\"intellect\":${e.intellect},\"leadership\":${e.leadership}," +
            "\"benevolence\":${e.benevolence},\"authority\":${e.authority},\"charisma\":${e.charisma}," +
            "\"stamina\":${e.stamina}" +
            "}"
    }

    private fun forceToJson(f: Force): String {
        val rels = f.relationships.entries.joinToString(",") { "\"${esc(it.key)}\":${it.value}" }
        return "{" +
            "\"id\":\"${esc(f.id)}\"," +
            "\"name\":\"${esc(f.name)}\"," +
            "\"color\":${f.color}," +
            "\"treasury\":${f.treasury}," +
            "\"foodReserve\":${f.foodReserve}," +
            "\"isAlive\":${f.isAlive}," +
            "\"rels\":{${rels}}" +
            "}"
    }

    private fun ministerToJson(m: Minister): String {
        return "{" +
            "\"id\":\"${esc(m.id)}\"," +
            "\"name\":\"${esc(m.name)}\"," +
            "\"title\":\"${esc(m.title)}\"," +
            "\"force\":\"${esc(m.force)}\"," +
            "\"portraitKey\":\"${esc(m.portraitKey)}\"," +
            "\"power\":${m.power},\"intellect\":${m.intellect},\"leadership\":${m.leadership},\"benevolence\":${m.benevolence}," +
            "\"loyalty\":${m.loyalty},\"relation\":${m.relation}," +
            "\"status\":\"${m.status.name}\"" +
            "}"
    }

    private fun memorialToJson(m: Memorial): String {
        return "{" +
            "\"id\":\"${esc(m.id)}\"," +
            "\"type\":\"${m.type.name}\"," +
            "\"title\":\"${esc(m.title)}\"," +
            "\"content\":\"${esc(m.content)}\"," +
            "\"suggestedAction\":\"${esc(m.suggestedAction)}\"," +
            "\"ministerId\":\"${esc(m.ministerId)}\"," +
            "\"goldEffect\":${m.goldEffect},\"moraleEffect\":${m.moraleEffect}," +
            "\"troopEffect\":${m.troopEffect},\"loyaltyEffect\":${m.loyaltyEffect}," +
            "\"status\":\"${m.status.name}\"" +
            "}"
    }

    private fun prefectureToJson(p: Prefecture): String {
        return "{" +
            "\"id\":\"${esc(p.id)}\"," +
            "\"name\":\"${esc(p.name)}\"," +
            "\"region\":\"${esc(p.region)}\"," +
            "\"gridX\":${p.gridX},\"gridY\":${p.gridY}," +
            "\"governor\":\"${esc(p.governor ?: "")}\"," +
            "\"troops\":${p.troops},\"morale\":${p.morale},\"tax\":${p.tax}," +
            "\"ownerForce\":\"${esc(p.ownerForce)}\"" +
            "}"
    }

    private fun consortToJson(c: Consort): String {
        return "{" +
            "\"id\":\"${esc(c.id)}\"," +
            "\"name\":\"${esc(c.name)}\"," +
            "\"beauty\":${c.beauty},\"talent\":${c.talent}," +
            "\"family\":\"${esc(c.family)}\"," +
            "\"favor\":${c.favor}" +
            "}"
    }

    private fun jsonToState(json: String): GameState? {
        // 简化的 JSON 解析：手动解析顶层字段
        return try {
            val emperorJson = extractObject(json, "emperor") ?: return null
            val forcesJson = extractArray(json, "forces")
            val ministersJson = extractArray(json, "ministers")
            val memorialsJson = extractArray(json, "memorials")
            val prefecturesJson = extractArray(json, "prefectures")
            val consortsJson = extractArray(json, "consorts")

            val emperor = parseEmperor(emperorJson)
            val forces = splitTopLevelObjects(forcesJson).mapNotNull { parseForce(it) }
            val ministers = splitTopLevelObjects(ministersJson).mapNotNull { parseMinister(it) }
            val memorials = splitTopLevelObjects(memorialsJson).mapNotNull { parseMemorial(it) }
            val prefectures = splitTopLevelObjects(prefecturesJson).mapNotNull { parsePrefecture(it) }
            val consorts = splitTopLevelObjects(consortsJson).mapNotNull { parseConsort(it) }

            GameState(
                emperor, ministers, memorials, prefectures, consorts,
                gold = extractInt(json, "gold") ?: 10000,
                peopleMorale = extractInt(json, "peopleMorale") ?: 70,
                troopTotal = extractInt(json, "troopTotal") ?: 50000,
                turnCount = extractInt(json, "turnCount") ?: 0
            ).also {
                // 强制让 forces 与 GameState.forces 同步
                it.forces.clear()
                it.forces.addAll(forces)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractObject(json: String, key: String): String? {
        val marker = "\"$key\":"
        val start = json.indexOf(marker)
        if (start < 0) return null
        val i = start + marker.length
        // 跳过空白
        var idx = i
        while (idx < json.length && json[idx].isWhitespace()) idx++
        if (idx >= json.length || json[idx] != '{') return null
        return extractBalanced(json, idx, '{', '}')
    }

    private fun extractArray(json: String, key: String): String {
        val marker = "\"$key\":"
        val start = json.indexOf(marker)
        if (start < 0) return ""
        val i = start + marker.length
        var idx = i
        while (idx < json.length && json[idx].isWhitespace()) idx++
        if (idx >= json.length || json[idx] != '[') return ""
        return extractBalanced(json, idx, '[', ']')
    }

    private fun extractBalanced(json: String, start: Int, open: Char, close: Char): String {
        var depth = 0
        var idx = start
        while (idx < json.length) {
            val c = json[idx]
            if (c == open) depth++
            else if (c == close) {
                depth--
                if (depth == 0) return json.substring(start, idx + 1)
            }
            idx++
        }
        return json.substring(start)
    }

    private fun splitTopLevelObjects(arrayJson: String): List<String> {
        if (arrayJson.length < 2) return emptyList()
        val inner = arrayJson.substring(1, arrayJson.length - 1)
        val result = mutableListOf<String>()
        var depth = 0
        var start = 0
        for (i in inner.indices) {
            val c = inner[i]
            if (c == '{') {
                if (depth == 0) start = i
                depth++
            } else if (c == '}') {
                depth--
                if (depth == 0) result.add(inner.substring(start, i + 1))
            }
        }
        return result
    }

    private fun extractInt(json: String, key: String): Int? {
        val marker = "\"$key\":"
        val start = json.indexOf(marker)
        if (start < 0) return null
        val i = start + marker.length
        var idx = i
        while (idx < json.length && json[idx].isWhitespace()) idx++
        if (idx >= json.length) return null
        if (json[idx] == '-' || json[idx].isDigit()) {
            var end = idx
            while (end < json.length && (json[end].isDigit() || json[end] == '-')) end++
            return json.substring(idx, end).toIntOrNull()
        }
        return null
    }

    private fun extractString(json: String, key: String): String? {
        val marker = "\"$key\":\""
        val start = json.indexOf(marker)
        if (start < 0) return null
        val i = start + marker.length
        val end = findStringEnd(json, i)
        return json.substring(i, end)
    }

    private fun findStringEnd(json: String, start: Int): Int {
        var i = start
        while (i < json.length) {
            val c = json[i]
            if (c == '\\') { i += 2; continue }
            if (c == '"') return i
            i++
        }
        return json.length
    }

    private fun parseEmperor(json: String): Emperor {
        return Emperor(
            name = extractString(json, "name") ?: "",
            dynasty = extractString(json, "dynasty") ?: "",
            force = extractString(json, "force") ?: "",
            capitalPrefecture = extractString(json, "capital") ?: "",
            year = extractInt(json, "year") ?: 1,
            month = extractInt(json, "month") ?: 1,
            day = extractInt(json, "day") ?: 1,
            shichen = extractInt(json, "shichen") ?: 0,
            power = extractInt(json, "power") ?: 50,
            intellect = extractInt(json, "intellect") ?: 50,
            leadership = extractInt(json, "leadership") ?: 50,
            benevolence = extractInt(json, "benevolence") ?: 50,
            authority = extractInt(json, "authority") ?: 50,
            charisma = extractInt(json, "charisma") ?: 50,
            stamina = extractInt(json, "stamina") ?: 100
        )
    }

    private fun parseForce(json: String): Force? {
        val id = extractString(json, "id") ?: return null
        val name = extractString(json, "name") ?: id
        val color = extractInt(json, "color") ?: 0
        val f = Force(
            id, name, color,
            treasury = extractInt(json, "treasury") ?: 5000,
            foodReserve = extractInt(json, "foodReserve") ?: 1000,
            isAlive = json.contains("\"isAlive\":true")
        )
        val relsJson = extractObject(json, "rels")
        if (relsJson != null) {
            // 简化的关系解析
            val content = relsJson.substring(1, relsJson.length - 1)
            val regex = Regex("\"([^\"]+)\":(-?\\d+)")
            for (m in regex.findAll(content)) {
                f.relationships[m.groupValues[1]] = m.groupValues[2].toInt()
            }
        }
        return f
    }

    private fun parseMinister(json: String): Minister? {
        val id = extractString(json, "id") ?: return null
        return Minister(
            id = id,
            name = extractString(json, "name") ?: id,
            title = extractString(json, "title") ?: "",
            force = extractString(json, "force") ?: "",
            portraitKey = extractString(json, "portraitKey") ?: id,
            power = extractInt(json, "power") ?: 50,
            intellect = extractInt(json, "intellect") ?: 50,
            leadership = extractInt(json, "leadership") ?: 50,
            benevolence = extractInt(json, "benevolence") ?: 50,
            loyalty = extractInt(json, "loyalty") ?: 50,
            relation = extractInt(json, "relation") ?: 50,
            status = MinisterStatus.valueOf(extractString(json, "status") ?: "IN_OFFICE")
        )
    }

    private fun parseMemorial(json: String): Memorial? {
        val id = extractString(json, "id") ?: return null
        return Memorial(
            id = id,
            type = MemorialType.valueOf(extractString(json, "type") ?: "TAX"),
            title = extractString(json, "title") ?: "",
            content = extractString(json, "content") ?: "",
            suggestedAction = extractString(json, "suggestedAction") ?: "",
            ministerId = extractString(json, "ministerId") ?: "",
            goldEffect = extractInt(json, "goldEffect") ?: 0,
            moraleEffect = extractInt(json, "moraleEffect") ?: 0,
            troopEffect = extractInt(json, "troopEffect") ?: 0,
            loyaltyEffect = extractInt(json, "loyaltyEffect") ?: 0,
            status = MemorialStatus.valueOf(extractString(json, "status") ?: "PENDING")
        )
    }

    private fun parsePrefecture(json: String): Prefecture? {
        val id = extractString(json, "id") ?: return null
        return Prefecture(
            id = id,
            name = extractString(json, "name") ?: "",
            region = extractString(json, "region") ?: "",
            gridX = extractInt(json, "gridX") ?: 0,
            gridY = extractInt(json, "gridY") ?: 0,
            governor = extractString(json, "governor")?.takeIf { it.isNotEmpty() },
            troops = extractInt(json, "troops") ?: 5000,
            morale = extractInt(json, "morale") ?: 70,
            tax = extractInt(json, "tax") ?: 1000,
            ownerForce = extractString(json, "ownerForce") ?: Force.HAN
        )
    }

    private fun parseConsort(json: String): Consort? {
        val id = extractString(json, "id") ?: return null
        return Consort(
            id = id,
            name = extractString(json, "name") ?: "",
            beauty = extractInt(json, "beauty") ?: 60,
            talent = extractInt(json, "talent") ?: 50,
            family = extractString(json, "family") ?: "",
            favor = extractInt(json, "favor") ?: 30
        )
    }
}