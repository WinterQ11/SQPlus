package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChannelEntity
import com.example.data.model.DownloadRecordEntity
import com.example.data.model.FileEntity

@Database(
    entities = [
        ChannelEntity::class,
        FileEntity::class,
        DownloadRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SqPlusDatabase : RoomDatabase() {

    abstract fun sqPlusDao(): SqPlusDao

    companion object {
        @Volatile
        private var INSTANCE: SqPlusDatabase? = null

        fun getInstance(context: Context): SqPlusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SqPlusDatabase::class.java,
                    "sqplus_platform.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
