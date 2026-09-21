package com.threekingdoms.emperor.data

data class Consort(
    val id: String,
    val name: String,
    val beauty: Int,
    val talent: Int,
    val family: String,
    var favor: Int = 30
)