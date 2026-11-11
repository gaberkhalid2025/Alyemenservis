package com.example.utils

import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.MapZone

object ReliabilityZones {
    private const val CELL = 0.01          // ≈ 1.1 كم

    fun build(providers: List<ProviderEntity>, stores: List<StoreEntity>): List<MapZone> {
        fun cell(lat: Double, lng: Double) = Math.floor(lat / CELL).toLong() to Math.floor(lng / CELL).toLong()
        val provCells = providers.filter { it.latitude != 0.0 && it.longitude != 0.0 }.groupBy { cell(it.latitude, it.longitude) }
        val demandCells = stores.filter { it.latitude != 0.0 && it.longitude != 0.0 }.map { cell(it.latitude, it.longitude) }.toSet()
        val out = mutableListOf<MapZone>()
        for ((c, list) in provCells) {
            val color = if (list.any { it.isAvailable }) "#10B981" else "#F59E0B"        // أخضر متاح / أصفر مشغول
            out.add(MapZone((c.first + 0.5) * CELL, (c.second + 0.5) * CELL, 600, color))
        }
        for (c in demandCells) {
            if (c !in provCells) {
                out.add(MapZone((c.first + 0.5) * CELL, (c.second + 0.5) * CELL, 600, "#EF4444"))   // نشاط تجاري بلا فنيين
            }
        }
        return out
    }
}
