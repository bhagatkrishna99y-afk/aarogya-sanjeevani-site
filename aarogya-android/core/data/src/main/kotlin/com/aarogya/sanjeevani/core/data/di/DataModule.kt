package com.aarogya.sanjeevani.core.data.di

import com.aarogya.sanjeevani.core.data.auth.AuthGateway
import com.aarogya.sanjeevani.core.data.auth.AuthRepository
import com.aarogya.sanjeevani.core.data.auth.AuthRepositoryImpl
import com.aarogya.sanjeevani.core.data.auth.RealAuthGateway
import com.aarogya.sanjeevani.core.data.doctors.DoctorsRepository
import com.aarogya.sanjeevani.core.data.doctors.DoctorsRepositoryImpl
import com.aarogya.sanjeevani.core.data.notifications.InAppNotificationSource
import com.aarogya.sanjeevani.core.data.notifications.NotificationSource
import com.aarogya.sanjeevani.core.data.notifications.NotificationsRepository
import com.aarogya.sanjeevani.core.data.notifications.NotificationsRepositoryImpl
import com.aarogya.sanjeevani.core.data.profile.ProfileRepository
import com.aarogya.sanjeevani.core.data.profile.ProfileRepositoryImpl
import com.aarogya.sanjeevani.core.data.requests.PollingRequestUpdatesSource
import com.aarogya.sanjeevani.core.data.requests.RequestUpdatesSource
import com.aarogya.sanjeevani.core.data.requests.RequestsRepository
import com.aarogya.sanjeevani.core.data.requests.RequestsRepositoryImpl
import com.aarogya.sanjeevani.core.data.services.CachingServicesRepository
import com.aarogya.sanjeevani.core.data.services.RemoteServicesRepository
import com.aarogya.sanjeevani.core.data.services.ServicesRepository
import com.aarogya.sanjeevani.core.data.services.ServicesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAuthGateway(impl: RealAuthGateway): AuthGateway

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindNotificationsRepository(impl: NotificationsRepositoryImpl): NotificationsRepository

    @Binds
    @Singleton
    abstract fun bindNotificationSource(impl: InAppNotificationSource): NotificationSource

    @Binds
    @Singleton
    abstract fun bindRequestsRepository(impl: RequestsRepositoryImpl): RequestsRepository

    @Binds
    @Singleton
    abstract fun bindRequestUpdatesSource(impl: PollingRequestUpdatesSource): RequestUpdatesSource

    @Binds
    @RemoteServicesRepository
    @Singleton
    abstract fun bindRemoteServicesRepository(impl: ServicesRepositoryImpl): ServicesRepository

    @Binds
    @Singleton
    abstract fun bindServicesRepository(impl: CachingServicesRepository): ServicesRepository

    @Binds
    @Singleton
    abstract fun bindDoctorsRepository(impl: DoctorsRepositoryImpl): DoctorsRepository
}
