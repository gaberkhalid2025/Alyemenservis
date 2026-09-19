package com.example.di

import android.content.Context
import com.example.data.repositories.BookingRepository
import com.example.data.repositories.ChatRepository
import com.example.data.repositories.IChatRepository
import com.example.data.repositories.InstantRequestRepository
import com.example.data.repositories.IDashboardRepository
import com.example.data.repositories.DashboardRepositoryImpl
import com.example.data.repositories.IProductsRepository
import com.example.data.repositories.ProductsRepositoryImpl
import com.example.data.repositories.IRatingsRepository
import com.example.data.repositories.RatingsRepositoryImpl
import com.example.data.repositories.IFavoritesRepository
import com.example.data.repositories.FavoritesRepositoryImpl
import com.example.data.repositories.IGalleryRepository
import com.example.data.repositories.GalleryRepositoryImpl
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

    @Provides
    @Singleton
    fun provideDashboardRepository(
        @ApplicationContext context: Context
    ): IDashboardRepository {
        return DashboardRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideProductsRepository(
        @ApplicationContext context: Context
    ): IProductsRepository {
        return ProductsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideRatingsRepository(
        @ApplicationContext context: Context
    ): IRatingsRepository {
        return RatingsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideFavoritesRepository(
        @ApplicationContext context: Context
    ): IFavoritesRepository {
        return FavoritesRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideGalleryRepository(
        @ApplicationContext context: Context
    ): IGalleryRepository {
        return GalleryRepositoryImpl(context)
    }
}
