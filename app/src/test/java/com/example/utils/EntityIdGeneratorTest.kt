package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class EntityIdGeneratorTest {
    
    @Test
    fun `generateUserId has USR prefix`() {
        val id = EntityIdGenerator.generateUserId()
        assertTrue(id.startsWith("USR_"))
    }
    
    @Test
    fun `generateProviderId has PRV prefix`() {
        assertTrue(EntityIdGenerator.generateProviderId().startsWith("PRV_"))
    }
    
    @Test
    fun `generateStoreId has STR prefix`() {
        assertTrue(EntityIdGenerator.generateStoreId().startsWith("STR_"))
    }
    
    @Test
    fun `generateBookingId has BKG prefix`() {
        assertTrue(EntityIdGenerator.generateBookingId().startsWith("BKG_"))
    }
    
    @Test
    fun `generated IDs are unique`() {
        val ids = (1..100).map { EntityIdGenerator.generateUserId() }
        assertEquals(100, ids.distinct().size)
    }
    
    @Test
    fun `generated IDs are timestamp-ordered`() {
        val id1 = EntityIdGenerator.generateUserId()
        Thread.sleep(2)
        val id2 = EntityIdGenerator.generateUserId()
        val ts1 = id1.split("_")[1].toLong()
        val ts2 = id2.split("_")[1].toLong()
        assertTrue(ts2 >= ts1)
    }
    
    @Test
    fun `isValidPrefixedId returns true for valid ID`() {
        val id = EntityIdGenerator.generateUserId()
        assertTrue(EntityIdGenerator.isValidPrefixedId(id))
    }
    
    @Test
    fun `isValidPrefixedId returns false for invalid`() {
        assertFalse(EntityIdGenerator.isValidPrefixedId("invalid"))
        assertFalse(EntityIdGenerator.isValidPrefixedId(""))
        assertFalse(EntityIdGenerator.isValidPrefixedId("USR_"))
    }
    
    @Test
    fun `getPrefix extracts correct prefix`() {
        val id = EntityIdGenerator.generateUserId()
        assertEquals("USR", EntityIdGenerator.getPrefix(id))
    }
}
