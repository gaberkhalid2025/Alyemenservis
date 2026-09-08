package com.example.ui

import com.example.data.*
import com.example.ui.*
import com.example.data.models.*

fun MainViewModel.approvePendingRequest(req: PendingProviderEntity) {
    this.adminViewModel.approveRequest(req)
}

fun MainViewModel.rejectPendingRequest(req: PendingProviderEntity, reason: String = "") {
    this.adminViewModel.rejectRequest(req, reason)
}

fun MainViewModel.saveStoreEntity(store: StoreEntity) {
    this.adminViewModel.saveStore(store)
}

fun MainViewModel.deleteStoreEntity(storeId: String) {
    this.adminViewModel.deleteStore(storeId)
}

fun MainViewModel.restoreStoreEntity(storeId: String) {
    this.adminViewModel.restoreStore(storeId)
}

fun MainViewModel.savePropertyEntity(property: PropertyEntity) {
    this.adminViewModel.saveProperty(property)
}

fun MainViewModel.deletePropertyEntity(propertyId: String) {
    this.adminViewModel.deleteProperty(propertyId)
}

fun MainViewModel.restorePropertyEntity(propertyId: String) {
    this.adminViewModel.restoreProperty(propertyId)
}

fun MainViewModel.saveJobEntity(job: JobEntity) {
    this.adminViewModel.saveJob(job)
}

fun MainViewModel.deleteJobEntity(jobId: String) {
    this.adminViewModel.deleteJob(jobId)
}

fun MainViewModel.restoreJobEntity(jobId: String) {
    this.adminViewModel.restoreJob(jobId)
}
