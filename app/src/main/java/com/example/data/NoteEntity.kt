package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val isPinned: Boolean = false,
    val dominantPattern: String = "Simple Narrative",
    val complexityScore: Float = 0.0f,
    val blockCount: Int = 1,
    val shiftCount: Int = 0,
    val hasDrift: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
