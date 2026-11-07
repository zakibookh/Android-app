package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.WhiteboardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhiteboardDao {
    @Query("SELECT * FROM whiteboards ORDER BY updatedAt DESC")
    fun getAllWhiteboards(): Flow<List<WhiteboardEntity>>

    @Query("SELECT * FROM whiteboards WHERE id = :id LIMIT 1")
    fun getWhiteboardById(id: Long): Flow<WhiteboardEntity?>

    @Query("SELECT * FROM whiteboards WHERE subject = :subject ORDER BY updatedAt DESC")
    fun getWhiteboardsBySubject(subject: String): Flow<List<WhiteboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhiteboard(whiteboard: WhiteboardEntity): Long

    @Update
    suspend fun updateWhiteboard(whiteboard: WhiteboardEntity)

    @Query("DELETE FROM whiteboards WHERE id = :id")
    suspend fun deleteWhiteboard(id: Long)
}
