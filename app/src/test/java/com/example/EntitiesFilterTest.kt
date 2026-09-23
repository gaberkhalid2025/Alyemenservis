package com.example

import com.example.data.StoreEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class EntitiesFilterTest {

    @Test
    fun testStoreFiltering() {
        val storeList = listOf(
            StoreEntity(id = "1", name = "سوبر ماركت الهدى", categoryId = "سوبرماركت", cityId = "Sanaa", rating = 4.8f, localNeighborhood = "حدة"),
            StoreEntity(id = "2", name = "برجر هوم", categoryId = "مطاعم", cityId = "Aden", rating = 3.5f, localNeighborhood = "كريتر"),
            StoreEntity(id = "3", name = "صيدلية اليمن", categoryId = "طبية", cityId = "Sanaa", rating = 4.2f, localNeighborhood = "التحرير")
        )

        // 1. Filter by search query
        val searchResult = storeList.filter { it.name.contains("الهدى") }
        assertEquals(1, searchResult.size)
        assertEquals("1", searchResult[0].id)

        // 2. Filter by city
        val cityResult = storeList.filter { it.cityId == "Sanaa" }
        assertEquals(2, cityResult.size)

        // 3. Filter by rating
        val ratingResult = storeList.filter { it.rating >= 4.0f }
        assertEquals(2, ratingResult.size)
    }
}
