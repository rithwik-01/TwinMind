package com.twinmind.recorder.di

import android.content.Context
import androidx.room.Room
import com.twinmind.recorder.data.local.dao.ChunkDao
import com.twinmind.recorder.data.local.dao.SessionDao
import com.twinmind.recorder.data.local.dao.SummaryDao
import com.twinmind.recorder.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "twinmind.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSessionDao(db: AppDatabase): SessionDao   = db.sessionDao()
    @Provides fun provideChunkDao(db: AppDatabase): ChunkDao       = db.chunkDao()
    @Provides fun provideSummaryDao(db: AppDatabase): SummaryDao   = db.summaryDao()
}
