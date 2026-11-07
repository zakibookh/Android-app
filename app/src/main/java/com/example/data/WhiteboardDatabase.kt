package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.WhiteboardDao
import com.example.data.entity.WhiteboardEntity

@Database(entities = [WhiteboardEntity::class], version = 1, exportSchema = false)
abstract class WhiteboardDatabase : RoomDatabase() {
    abstract fun whiteboardDao(): WhiteboardDao

    companion object {
        @Volatile
        private var INSTANCE: WhiteboardDatabase? = null

        fun getDatabase(context: Context): WhiteboardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhiteboardDatabase::class.java,
                    "whiteboard_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
