package com.example.di

import android.content.Context
import com.example.data.repositories.ApiKeyRepositoryImpl
import com.example.data.repositories.BookingRepository
import com.example.data.repositories.CategoryRepository
import com.example.data.repositories.ChatRepository
import com.example.data.repositories.DashboardExtensionsRepositoryImpl
import com.example.data.repositories.DashboardRepositoryImpl
import com.example.data.repositories.DataManagementRepositoryImpl
import com.example.data.repositories.FavoritesRepositoryImpl
import com.example.data.repositories.GalleryRepositoryImpl
import com.example.data.repositories.IApiKeyRepository
import com.example.data.repositories.IBookingRepository
import com.example.data.repositories.IChatRepository
import com.example.data.repositories.IDashboardExtensionsRepository
import com.example.data.repositories.IDashboardRepository
import com.example.data.repositories.IDataManagementRepository
import com.example.data.repositories.IFavoritesRepository
import com.example.data.repositories.IGalleryRepository
import com.example.data.repositories.IProductsRepository
import com.example.data.repositories.IRatingsRepository
import com.example.data.repositories.IRegistrationRepository
import com.example.data.repositories.IStatusRepository
import com.example.data.repositories.InstantRequestRepository
import com.example.data.repositories.MapRepository
import com.example.data.repositories.NotificationRepository
import com.example.data.repositories.ProductsRepositoryImpl
import com.example.data.repositories.RatingsRepositoryImpl
import com.example.data.repositories.RealtimeSyncRepository
import com.example.data.repositories.RegistrationRepositoryImpl
import com.example.data.repositories.StatusRepositoryImpl
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
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore
    ): BookingRepository {
        return BookingRepository(context, firestore)
    }

    @Provides
    @Singleton
    fun provideIBookingRepository(bookingRepository: BookingRepository): IBookingRepository {
        return bookingRepository
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
    fun provideRegistrationRepositoryImpl(
        @ApplicationContext context: Context
    ): RegistrationRepositoryImpl {
        return RegistrationRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideRegistrationRepository(
        registrationRepositoryImpl: RegistrationRepositoryImpl
    ): IRegistrationRepository {
        return registrationRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideStatusRepositoryImpl(
        @ApplicationContext context: Context
    ): StatusRepositoryImpl {
        return StatusRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideStatusRepository(
        statusRepositoryImpl: StatusRepositoryImpl
    ): IStatusRepository {
        return statusRepositoryImpl
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
    fun provideDashboardExtensionsRepository(): IDashboardExtensionsRepository {
        return DashboardExtensionsRepositoryImpl()
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

    @Provides
    @Singleton
    fun provideDataManagementRepository(
        firestore: FirebaseFirestore
    ): IDataManagementRepository {
        return DataManagementRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideApiKeyRepository(
        firestore: FirebaseFirestore
    ): IApiKeyRepository {
        return ApiKeyRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(
        firestore: FirebaseFirestore
    ): CategoryRepository {
        return CategoryRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(): NotificationRepository {
        return NotificationRepository()
    }

    @Provides
    @Singleton
    fun provideMapRepository(
        @ApplicationContext context: Context
    ): MapRepository {
        return MapRepository(context)
    }

    @Provides
    @Singleton
    fun provideRealtimeSyncRepository(
        @ApplicationContext context: Context
    ): RealtimeSyncRepository {
        return RealtimeSyncRepository(context)
    }
}
