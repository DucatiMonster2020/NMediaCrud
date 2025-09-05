package ru.netology.nmedia.di

import android.content.Context
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppAuth(
        @ApplicationContext
        context: Context,
        firebaseMessaging: FirebaseMessaging): AppAuth {
        return AppAuth(context, firebaseMessaging)
    }
}