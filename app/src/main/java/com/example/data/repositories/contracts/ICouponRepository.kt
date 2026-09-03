package com.example.data.repositories.contracts

import com.example.data.CouponEntity
import com.example.data.utils.AppResult

interface ICouponRepository {
    suspend fun applyCoupon(code: String, amount: Double): AppResult<Double>
    suspend fun validateCoupon(code: String): AppResult<CouponEntity>
}
