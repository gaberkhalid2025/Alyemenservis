package com.example.data

data class DiscountCoupon(
    val code: String,
    val discountPercent: Int,
    val description: String,
    val isUsed: Boolean = false
)

object CouponManager {
    private val coupons = mutableMapOf(
        "WELCOME50" to DiscountCoupon("WELCOME50", 15, "كوبون ترحيبي يمنحك خصم ١٥٪"),
        "YEMEN2026" to DiscountCoupon("YEMEN2026", 20, "كوبون بمناسبة الصيف يمنحك خصم ٢٠٪")
    )

    fun createCoupon(code: String, discount: Int, description: String): Boolean {
        if (coupons.containsKey(code.uppercase())) return false
        coupons[code.uppercase()] = DiscountCoupon(code.uppercase(), discount, description)
        return true
    }

    fun applyCoupon(code: String): DiscountCoupon? {
        val upper = code.uppercase()
        val coupon = coupons[upper]
        if (coupon != null && !coupon.isUsed) {
            return coupon
        }
        return null
    }

    fun getAllCoupons(): List<DiscountCoupon> {
        return coupons.values.toList()
    }
}
