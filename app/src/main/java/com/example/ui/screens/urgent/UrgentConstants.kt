package com.example.ui.screens.urgent

object UrgentConstants {
    val departments = listOf("خدمات وفنيين", "مراكز ومتاجر", "مطاعم وكافيهات")

    fun getSubCategories(selectedDepartment: String): List<String> {
        return when (selectedDepartment) {
            "خدمات وفنيين" -> listOf("سباكة طارئة", "كهرباء وطوارئ ماس", "تكييف وتبريد", "بنشر وسحب سيارات", "أقفال وأبواب", "أجهزة منزلية")
            "مراكز ومتاجر" -> listOf("قطع غيار مستعجلة", "بطاريات وزيوت", "أدوية ومستلزمات طبية", "إلكترونيات سريعة")
            else -> listOf("وجبات سريعة طارئة", "مشروبات ومياه", "مأكولات سريعة")
        }
    }

    val cities = listOf("صنعاء", "عدن", "تعز", "الحديدة", "إب", "حضرموت", "مأرب", "ذمار")

    val urgencyTimeOptions = listOf("الوصول خلال 15 دقيقة", "الوصول خلال 20 دقيقة", "الوصول خلال 30 دقيقة")
    val durationOptions = listOf("نصف ساعة", "ساعة واحدة", "ساعتان")
}
