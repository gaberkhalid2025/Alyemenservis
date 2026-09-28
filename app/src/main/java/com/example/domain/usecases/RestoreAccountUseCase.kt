package com.example.domain.usecases

import com.example.ui.helpers.AccountRecoveryHelper
import com.example.ui.MainViewModel.RestoreAccountMatch
import javax.inject.Inject

class RestoreAccountUseCase @Inject constructor() {

    fun searchAccount(
        recoveryHelper: AccountRecoveryHelper,
        phone: String,
        onResult: (RestoreAccountMatch?) -> Unit
    ) {
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone)
        if (cleanPhone.isBlank() || cleanPhone.length < 7) {
            onResult(null)
            return
        }
        recoveryHelper.searchAccountForRestore(cleanPhone, onResult)
    }
}
