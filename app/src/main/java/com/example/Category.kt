package com.example

import androidx.annotation.Keep

@Keep
data class Category(
    val id: Int = 0,
    val name: String = "",
    val icon: String = "",
    val order: Int = 0
) {
    companion object {
        fun fromMap(map: Map<String, Any>): Category {
            return Category(
                id = (map["id"] as? Number)?.toInt() ?: 0,
                name = map["name"] as? String ?: "",
                icon = map["icon"] as? String ?: "",
                order = (map["order"] as? Number)?.toInt() ?: 0
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "icon" to icon,
            "order" to order
        )
    }
}

val defaultCategories = listOf(
    Category(1, "صيانة منزلية", "🔧", 1),
    Category(2, "صحة ورعاية", "🏥", 2),
    Category(3, "تعليم وتدريب", "📚", 3),
    Category(4, "سيارات ونقل", "🚗", 4),
    Category(5, "تقنية وبرمجة", "💻", 5),
    Category(6, "جمال ولياقة", "💇", 6)
)
