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
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
        if (cleanPhone.isBlank() || cleanPhone.length < 7) {
            onResult(null)
            return
        }
        recoveryHelper.searchAccountForRestore(cleanPhone, onResult)
    }

    fun verifyPassword(
        recoveryHelper: AccountRecoveryHelper,
        phone: String,
        accountType: String,
        passwordInput: String,
        onResult: (Boolean) -> Unit
    ) {
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
        val cleanPassword = passwordInput.trim()
        if (cleanPhone.isBlank() || cleanPhone.length < 7 || cleanPassword.isBlank()) {
            onResult(false)
            return
        }
        recoveryHelper.verifyRestorePassword(cleanPhone, accountType.trim().ifBlank { "CLIENT" }, cleanPassword, onResult)
    }
}
