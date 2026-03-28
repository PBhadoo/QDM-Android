package com.parveenbhadoo.qdm.di

import com.parveenbhadoo.qdm.data.remote.HttpClientProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(provider: HttpClientProvider): OkHttpClient = provider.client
}
