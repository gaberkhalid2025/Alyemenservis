package com.example.domain.usecases

import com.example.data.PendingProviderEntity
import com.example.ui.viewmodels.AdminViewModel
import javax.inject.Inject

class ApproveJoinRequestUseCase @Inject constructor() {

    fun execute(adminViewModel: AdminViewModel, request: PendingProviderEntity) {
        adminViewModel.approveRequest(request)
    }
}
