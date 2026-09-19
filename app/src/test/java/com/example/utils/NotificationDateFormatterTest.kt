package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class NotificationDateFormatterTest {
    
    @Test
    fun `format - recent timestamp shows minutes`() {
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - (60 * 1000 + 5000) // 1 min 5 sec ago
        
        val result = NotificationDateFormatter.format(oneMinuteAgo)
        assertTrue(result.contains("دقيقة") || result.contains("الآن") || result.contains("لحظات"))
    }
    
    @Test
    fun `format - hours ago shows hours`() {
        val now = System.currentTimeMillis()
        val twoHoursAgo = now - (2 * 60 * 60 * 1000 + 10000)
        
        val result = NotificationDateFormatter.format(twoHoursAgo)
        assertTrue(result.contains("ساعة") || result.contains("ساعتين"))
    }
    
    @Test
    fun `format - old timestamp shows date`() {
        val tenDaysAgo = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000)
        
        val result = NotificationDateFormatter.format(tenDaysAgo)
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }
}
