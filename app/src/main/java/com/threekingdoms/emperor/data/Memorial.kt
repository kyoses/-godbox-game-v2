package com.threekingdoms.emperor.data

enum class MemorialType(val label: String, val iconHint: String) {
    TAX("税收", "💰"),
    MILITARY("军情", "⚔"),
    PERSONNEL("人事", "👥"),
    JUSTICE("刑狱", "⚖"),
    PUBLIC_WORKS("工程", "🏗"),
    DIPLOMACY("外交", "🤝")
}

enum class MemorialStatus { PENDING, APPROVED, HELD, REJECTED }

data class Memorial(
    val id: String,
    val type: MemorialType,
    val title: String,
    val content: String,
    val suggestedAction: String,
    val ministerId: String,
    val goldEffect: Int = 0,
    val moraleEffect: Int = 0,
    val troopEffect: Int = 0,
    val loyaltyEffect: Int = 0,
    var status: MemorialStatus = MemorialStatus.PENDING
)