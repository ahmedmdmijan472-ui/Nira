package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NiraDatabase : RoomDatabase() {
    abstract fun niraDao(): NiraDao

    companion object {
        @Volatile
        private var INSTANCE: NiraDatabase? = null

        fun getInstance(context: Context): NiraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NiraDatabase::class.java,
                    "nira_assistant.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
