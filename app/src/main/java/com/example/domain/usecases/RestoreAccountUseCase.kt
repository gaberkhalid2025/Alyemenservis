package com.example.domain.usecases

import android.content.Context
import com.example.ui.helpers.AccountRecoveryHelper
import com.example.ui.MainViewModel.RestoreAccountMatch
import javax.inject.Inject

class RestoreAccountUseCase @Inject constructor() {

    fun searchAccount(
        recoveryHelper: AccountRecoveryHelper,
        phone: String,
        onResult: (RestoreAccountMatch?) -> Unit
    ) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        recoveryHelper.searchAccountForRestore(cleanPhone, onResult)
    }
}
