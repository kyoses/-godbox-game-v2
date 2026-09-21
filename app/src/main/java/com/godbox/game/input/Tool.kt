package com.godbox.game.input

import com.godbox.game.engine.EntityType
import com.godbox.game.engine.TerrainType

enum class ToolCategory { TERRAIN, ENTITY, DISASTER, GOD_HAND, PAUSE, CLEAR, NONE }

/**
 * 当前工具 + 子选项。Activity 通过 ToolBarView 修改这里。
 */
class ToolState {
    var category: ToolCategory = ToolCategory.TERRAIN
    var selectedTerrain: TerrainType = TerrainType.GRASS
    var selectedEntity: EntityType = EntityType.HUMAN
    var selectedDisaster: DisasterKind = DisasterKind.METEOR
}

enum class DisasterKind { METEOR, FLOOD, VOLCANO, HOLY, LIGHTNING }