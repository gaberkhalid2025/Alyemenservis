package com.example.utils

object BookingChat {
    /** معرّف قناة ثابت مشتق من الحجز (لا حقل إضافي). إن كان نظامك يستعمل معرّفاً آخر فاستبدل هذه الدالة فقط. */
    fun channelIdFor(bookingId: String): String =
        "bk_" + bookingId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(60)

    val quickReplies = listOf("أنا في الطريق", "وصلت", "أحتاج توضيح العنوان", "سأتأخر قليلاً")

    fun etaMessage(etaMinutes: Int, address: LandmarkAddress?): String = buildString {
        append("وقت وصولي المتوقع ≈ ${etaMinutes.coerceAtLeast(1)} دقيقة (تقريبي)")
        address?.let { append("\nالعنوان: ${it.describe()}") }
    }
}

/** تنفّذه بدمج دالتي فتح/إرسال الدردشة الموجودتين لديك. */
interface BookingChatBridge {
    fun open(channelId: String)
    fun send(channelId: String, text: String)       // كل رسالة = كتابة واحدة
}
