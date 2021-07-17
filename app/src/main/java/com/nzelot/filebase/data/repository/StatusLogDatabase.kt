package com.nzelot.filebase.data.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nzelot.filebase.data.model.StatusLogEntry
import com.nzelot.filebase.data.storage.StatusLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(entities = [StatusLogEntry::class], version = 1)
abstract class StatusLogDatabase : RoomDatabase() {
    abstract fun statusLogDao() : StatusLogDao
}

@Module
@InstallIn(SingletonComponent::class)
class StatusLogStorage {

    @Singleton
    @Provides
    fun providesStatusLogDb(
        @ApplicationContext context: Context
    ) : StatusLogDatabase =
        Room.databaseBuilder(context, StatusLogDatabase::class.java, "status-logs").build()

}