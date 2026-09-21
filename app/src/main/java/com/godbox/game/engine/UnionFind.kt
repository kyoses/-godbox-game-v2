package com.godbox.game.engine

/**
 * 简易并查集，用于发现邻接的人类群体。
 */
class UnionFind(size: Int) {
    private val parent = IntArray(size) { it }
    private val rank = IntArray(size)

    fun find(x: Int): Int {
        var root = x
        while (parent[root] != root) {
            parent[root] = parent[parent[root]]
            root = parent[root]
        }
        return root
    }

    fun union(a: Int, b: Int) {
        val ra = find(a)
        val rb = find(b)
        if (ra == rb) return
        if (rank[ra] < rank[rb]) {
            parent[ra] = rb
        } else if (rank[ra] > rank[rb]) {
            parent[rb] = ra
        } else {
            parent[rb] = ra
            rank[ra]++
        }
    }

    fun groups(): Map<Int, MutableList<Int>> {
        val map = HashMap<Int, MutableList<Int>>()
        for (i in parent.indices) {
            val r = find(i)
            map.getOrPut(r) { mutableListOf() }.add(i)
        }
        return map
    }
}