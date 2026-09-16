package com.example.utils

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class NotificationDateFormatterTest {
    
    @Test
    fun `recent time returns "منذ لحظات"`() {
        val recent = System.currentTimeMillis() - 10_000 // 10 ثواني
        val result = NotificationDateFormatter.format(recent)
        assertEquals("منذ لحظات", result)
    }
    
    @Test
    fun `minutes ago returns correct text`() {
        val fiveMinAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)
        val result = NotificationDateFormatter.format(fiveMinAgo)
        assertTrue(result.contains("5") && result.contains("دقيقة"))
    }
    
    @Test
    fun `hours ago contains time`() {
        val threeHoursAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3)
        val result = NotificationDateFormatter.format(threeHoursAgo)
        assertTrue(result.contains("3") && result.contains("ساعة"))
    }
    
    @Test
    fun `yesterday returns "أمس"`() {
        val yesterday = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        val result = NotificationDateFormatter.format(yesterday)
        assertTrue(result.contains("أمس"))
    }
    
    @Test
    fun `days ago returns correct text`() {
        val threeDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)
        val result = NotificationDateFormatter.format(threeDaysAgo)
        assertTrue(result.contains("3") && result.contains("أيام"))
    }
    
    @Test
    fun `very old date returns full date`() {
        val veryOld = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        val result = NotificationDateFormatter.format(veryOld)
        assertTrue(result.isNotBlank() && !result.startsWith("منذ") && !result.startsWith("أمس"))
    }
    
    @Test
    fun `zero timestamp returns "الآن"`() {
        assertEquals("الآن", NotificationDateFormatter.format(0L))
    }
}
