package com.example.data.repositories.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.example.data.CouponEntity
import com.example.data.repositories.contracts.ICouponRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
import kotlinx.coroutines.tasks.await

class CouponRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ICouponRepository {

    override suspend fun validateCoupon(code: String): AppResult<CouponEntity> {
        return try {
            val querySnapshot = firestore.collection("coupons")
                .whereEqualTo("code", code)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(AppError.NotFoundError("الكوبون غير صحيح"))
            }

            val document = querySnapshot.documents.first()
            val coupon = document.toObject(CouponEntity::class.java)
                ?: return Result.failure(AppError.ValidationError("خطأ في قراءة بيانات الكوبون"))

            val couponWithId = coupon.copy(id = document.id)

            if (couponWithId.status != "ACTIVE") {
                return Result.failure(AppError.ValidationError("هذا الكوبون غير فعال حالياً"))
            }

            if (couponWithId.expiryTimestamp > 0 && couponWithId.expiryTimestamp < System.currentTimeMillis()) {
                return Result.failure(AppError.ValidationError("انتهت صلاحية هذا الكوبون"))
            }

            if (couponWithId.usedCount >= couponWithId.maxUsageCount) {
                return Result.failure(AppError.ValidationError("نفدت عدد مرات استخدام هذا الكوبون"))
            }

            Result.success(couponWithId)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "حدث خطأ أثناء التحقق من الكوبون"))
        }
    }

    override suspend fun applyCoupon(code: String, amount: Double): AppResult<Double> {
        return try {
            val validationResult = validateCoupon(code)
            validationResult.fold(
                onSuccess = { coupon ->
                    val discount = if (coupon.discountPercentage > 0) {
                        amount * (coupon.discountPercentage.toDouble() / 100.0)
                    } else if (coupon.pointsValue > 0) {
                        coupon.pointsValue.toDouble()
                    } else {
                        0.0
                    }
                    val finalAmount = (amount - discount).coerceAtLeast(0.0)
                    
                    // Increment usage count of coupon
                    firestore.collection("coupons").document(coupon.id)
                        .update("usedCount", coupon.usedCount + 1)
                        .await()

                    Result.success(finalAmount)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تطبيق الكوبون"))
        }
    }
}
