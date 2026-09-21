package com.threekingdoms.emperor.data

data class Scenario(
    val id: String,
    val name: String,
    val dynasty: String,
    val era: String,
    val difficulty: String,
    val capital: String,
    val force: String,
    val description: String,
    val initialEmperor: Emperor,
    val initialMinisters: List<Minister>,
    val initialConsorts: List<Consort>,
    val initialMemorials: List<Memorial>,
    val initialPrefectures: List<Prefecture>
)