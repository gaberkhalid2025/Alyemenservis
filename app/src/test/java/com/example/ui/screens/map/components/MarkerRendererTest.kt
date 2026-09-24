package com.example.ui.screens.map.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class MarkerRendererTest {

    @Test
    fun testGetColorForType() {
        assertEquals(Color(0xFF00E5FF), MarkerRenderer.getColorForType("PROVIDER"))
        assertEquals(Color(0xFF00E5FF), MarkerRenderer.getColorForType("TECHNICIAN"))
        assertEquals(Color(0xFF10B981), MarkerRenderer.getColorForType("STORE"))
        assertEquals(Color(0xFFF59E0B), MarkerRenderer.getColorForType("RESTAURANT"))
        assertEquals(Color(0xFFEC4899), MarkerRenderer.getColorForType("MEDICAL"))
        assertEquals(Color(0xFF8B5CF6), MarkerRenderer.getColorForType("PROPERTY"))
        assertEquals(Color(0xFF6366F1), MarkerRenderer.getColorForType("JOB"))
    }

    @Test
    fun testGetEmojiForType() {
        assertEquals("👷", MarkerRenderer.getEmojiForType("PROVIDER"))
        assertEquals("🏪", MarkerRenderer.getEmojiForType("STORE"))
        assertEquals("🍔", MarkerRenderer.getEmojiForType("RESTAURANT"))
        assertEquals("🏥", MarkerRenderer.getEmojiForType("MEDICAL"))
        assertEquals("🏠", MarkerRenderer.getEmojiForType("PROPERTY"))
        assertEquals("💼", MarkerRenderer.getEmojiForType("JOB"))
        assertEquals("📍", MarkerRenderer.getEmojiForType("UNKNOWN"))
    }

    @Test
    fun testClusterPointsEmpty() {
        val clusters = MarkerRenderer.clusterPoints(emptyList())
        assertTrue(clusters.isEmpty())
    }

    @Test
    fun testClusterPointsSinglePoint() {
        val points = listOf(
            MarkerRenderer.MapItemPoint(
                id = "p1",
                title = "كهربائي",
                type = "PROVIDER",
                x = 100f,
                y = 100f,
                originalItem = Unit
            )
        )
        val clusters = MarkerRenderer.clusterPoints(points, thresholdPx = 50f)
        assertEquals(1, clusters.size)
        assertEquals(1, clusters[0].items.size)
        assertEquals(100f, clusters[0].centerX, 0.001f)
        assertEquals(100f, clusters[0].centerY, 0.001f)
    }

    @Test
    fun testClusterPointsGrouping() {
        // Two points in the same 50px grid cell
        val points = listOf(
            MarkerRenderer.MapItemPoint(
                id = "p1",
                title = "نقطة 1",
                type = "STORE",
                x = 10f,
                y = 10f,
                originalItem = Unit
            ),
            MarkerRenderer.MapItemPoint(
                id = "p2",
                title = "نقطة 2",
                type = "STORE",
                x = 20f,
                y = 20f,
                originalItem = Unit
            ),
            MarkerRenderer.MapItemPoint(
                id = "p3",
                title = "نقطة بعيدة",
                type = "STORE",
                x = 500f,
                y = 500f,
                originalItem = Unit
            )
        )
        val clusters = MarkerRenderer.clusterPoints(points, thresholdPx = 50f)
        assertEquals(2, clusters.size)
        val groupedCluster = clusters.first { it.items.size == 2 }
        assertEquals(15f, groupedCluster.centerX, 0.001f)
        assertEquals(15f, groupedCluster.centerY, 0.001f)
    }
}
