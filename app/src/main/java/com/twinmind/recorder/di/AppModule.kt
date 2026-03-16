package com.twinmind.recorder.di

import android.content.Context
import com.twinmind.recorder.datastore.SessionPreferences
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
    fun provideSessionPreferences(@ApplicationContext context: Context): SessionPreferences =
        SessionPreferences(context)
}
