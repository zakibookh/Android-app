package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whiteboards")
data class WhiteboardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String = "General",
    val backgroundType: String = "GRID",
    val themeName: String = "DARK_SLATE",
    val strokesJson: String = "[]",
    val shapesJson: String = "[]",
    val textBlocksJson: String = "[]",
    val graphsJson: String = "[]",
    val pageNumber: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
