package com.twinmind.recorder.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.twinmind.recorder.data.local.dao.ChunkDao
import com.twinmind.recorder.data.local.dao.SessionDao
import com.twinmind.recorder.data.local.dao.SummaryDao
import com.twinmind.recorder.data.local.entity.ChunkEntity
import com.twinmind.recorder.data.local.entity.SessionEntity
import com.twinmind.recorder.data.local.entity.SummaryEntity

@Database(
    entities = [SessionEntity::class, ChunkEntity::class, SummaryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun chunkDao(): ChunkDao
    abstract fun summaryDao(): SummaryDao
}
