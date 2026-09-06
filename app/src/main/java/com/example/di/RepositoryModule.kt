package com.example.di

import android.content.Context
import com.example.data.repositories.BookingRepository
import com.example.data.repositories.ChatRepository
import com.example.data.repositories.IChatRepository
import com.example.data.repositories.InstantRequestRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideChatRepository(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore
    ): ChatRepository {
        return ChatRepository(context, firestore)
    }

    @Provides
    @Singleton
    fun provideIChatRepository(chatRepository: ChatRepository): IChatRepository {
        return chatRepository
    }

    @Provides
    @Singleton
    fun provideBookingRepository(
        @ApplicationContext context: Context
    ): BookingRepository {
        return BookingRepository(context)
    }

    @Provides
    @Singleton
    fun provideInstantRequestRepository(
        @ApplicationContext context: Context
    ): InstantRequestRepository {
        return InstantRequestRepository(context)
    }
}
