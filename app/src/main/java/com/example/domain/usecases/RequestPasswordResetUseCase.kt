package com.example.domain.usecases

import android.content.Context
import com.example.ui.helpers.AccountRecoveryHelper
import javax.inject.Inject

class RequestPasswordResetUseCase @Inject constructor() {

    fun execute(
        recoveryHelper: AccountRecoveryHelper,
        context: Context,
        phone: String,
        name: String,
        accountType: String,
        onPasswordWaitingPhoneSet: (String) -> Unit,
        triggerNotification: (String) -> Unit,
        onResult: (Boolean) -> Unit
    ) {
        recoveryHelper.requestPasswordReset(
            context = context,
            phone = phone,
            name = name,
            accountType = accountType,
            onPasswordWaitingPhoneSet = onPasswordWaitingPhoneSet,
            triggerNotification = triggerNotification,
            onResult = onResult
        )
    }
}
