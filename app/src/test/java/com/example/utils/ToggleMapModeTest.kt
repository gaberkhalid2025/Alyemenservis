package com.example.utils

import com.example.ui.screens.map.MapMode
import com.example.ui.screens.map.MapScreenState
import org.junit.Assert.assertEquals
import org.junit.Test

class ToggleMapModeTest {

    @Test
    fun testToggleBetweenModes() {
        val state = MapScreenState(initialMapMode = MapMode.SATELLITE)
        assertEquals(MapMode.SATELLITE, state.currentMapMode)

        // 1. التبديل إلى HOT
        state.toggleMapMode(MapMode.HOT)
        assertEquals(MapMode.HOT, state.currentMapMode)
        assertEquals(MapMode.SATELLITE, state.previousMapMode)

        // 2. الضغط على HOT مجدداً يعيدنا إلى الوضع السابق (SATELLITE)
        state.toggleMapMode(MapMode.HOT)
        assertEquals(MapMode.SATELLITE, state.currentMapMode)

        // 3. التبديل إلى TOPO ثم الرجوع
        state.toggleMapMode(MapMode.TOPO)
        assertEquals(MapMode.TOPO, state.currentMapMode)
        assertEquals(MapMode.SATELLITE, state.previousMapMode)

        state.toggleMapMode(MapMode.TOPO)
        assertEquals(MapMode.SATELLITE, state.currentMapMode)
    }

    @Test
    fun testZoomTicksIncrement() {
        val state = MapScreenState()
        assertEquals(0, state.zoomInTick)
        assertEquals(0, state.zoomOutTick)

        state.zoomInTick++
        assertEquals(1, state.zoomInTick)

        state.zoomOutTick++
        assertEquals(1, state.zoomOutTick)
    }

    @Test
    fun testVelaAndClarityAndLiveOverlays() {
        val state = MapScreenState(initialMapMode = MapMode.SATELLITE)

        state.toggleMapMode(MapMode.VELA)
        assertEquals(MapMode.VELA, state.currentMapMode)
        assertEquals(MapMode.SATELLITE, state.previousMapMode)

        state.toggleMapMode(MapMode.SATELLITE_CLARITY)
        assertEquals(MapMode.SATELLITE_CLARITY, state.currentMapMode)
        assertEquals(MapMode.VELA, state.previousMapMode)

        state.gibsEnabled = true
        state.gibsProduct = "modis"
        state.eoxEnabled = true
        assertEquals(true, state.gibsEnabled)
        assertEquals("modis", state.gibsProduct)
        assertEquals(true, state.eoxEnabled)
    }
}
