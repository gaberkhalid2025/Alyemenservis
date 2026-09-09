package com.example.data.repositories

import com.example.data.*
import com.example.data.models.*
import com.example.ui.viewmodels.BookingDistributionMode
import com.example.ui.viewmodels.BookingFormFields
import kotlinx.coroutines.flow.MutableStateFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.CopyOnWriteArrayList

@Singleton
class GlobalStateCoordinator @Inject constructor() {
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // Define all MutableStateFlows here...
}
