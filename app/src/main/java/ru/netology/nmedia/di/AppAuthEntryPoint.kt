package ru.netology.nmedia.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.auth.AppAuth

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppAuthEntryPoint {
    fun apiService(): ApiService
    fun appAuth(): AppAuth
}