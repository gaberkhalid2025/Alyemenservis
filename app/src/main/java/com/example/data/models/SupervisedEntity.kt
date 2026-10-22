package com.example.data.models

import androidx.annotation.Keep

@Keep
interface SupervisedEntity {
    val isBlocked: Boolean
    val blockReason: String
}
