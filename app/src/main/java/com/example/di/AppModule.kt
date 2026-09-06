package com.example.di

import android.content.Context
import com.example.data.repositories.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(104857600L)
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore
    ): IChatRepository {
        return ChatRepository(context, firestore)
    }

    @Provides
    @Singleton
    fun provideBookingRepository(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore
    ): BookingRepository {
        return BookingRepository(context)
    }

    @Provides
    @Singleton
    fun provideInstantRequestRepository(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore
    ): InstantRequestRepository {
        return InstantRequestRepository(context)
    }
}
