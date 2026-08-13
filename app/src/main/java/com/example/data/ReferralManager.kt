package com.example.data

object ReferralManager {
    private var referralCount: Int = 0
    private var rewardsEarned: Int = 0

    fun generateReferralLink(userId: String): String {
        return "https://yemendir.com/register?ref=$userId"
    }

    fun trackReferralActivation(userId: String) {
        referralCount += 1
        rewardsEarned += 50 // Earn 50 points per successful referral activation
    }

    fun getReferralStats(): Pair<Int, Int> {
        return Pair(referralCount, rewardsEarned)
    }

    fun resetStats() {
        referralCount = 0
        rewardsEarned = 0
    }
}
