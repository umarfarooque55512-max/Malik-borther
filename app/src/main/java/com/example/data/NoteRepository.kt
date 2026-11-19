package com.example.data

import com.example.pattern.StoryPatternAnalyzer
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    fun observeNotes(query: String): Flow<List<NoteEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            noteDao.observeAllNotes()
        } else {
            noteDao.searchNotes(trimmed)
        }
    }

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun createBlankNote(title: String = "", content: String = ""): Long {
        val analysis = StoryPatternAnalyzer.analyzeDocument(content)
        val now = System.currentTimeMillis()
        return noteDao.insertNote(
            NoteEntity(
                title = title,
                content = content,
                dominantPattern = analysis.dominantStyle.displayName,
                complexityScore = analysis.overallComplexity,
                blockCount = analysis.blocks.size,
                shiftCount = analysis.storySplitCount,
                hasDrift = analysis.driftingCount > 0,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun saveNote(
        id: Long,
        title: String,
        content: String,
        isPinned: Boolean,
        createdAt: Long
    ): Long {
        val analysis = StoryPatternAnalyzer.analyzeDocument(content)
        val now = System.currentTimeMillis()
        val entity = NoteEntity(
            id = id,
            title = title,
            content = content,
            isPinned = isPinned,
            dominantPattern = analysis.dominantStyle.displayName,
            complexityScore = analysis.overallComplexity,
            blockCount = analysis.blocks.size,
            shiftCount = analysis.storySplitCount,
            hasDrift = analysis.driftingCount > 0,
            createdAt = createdAt,
            updatedAt = now
        )
        return if (id == 0L) {
            noteDao.insertNote(entity)
        } else {
            noteDao.updateNote(entity)
            id
        }
    }

    suspend fun setPinned(id: Long, pinned: Boolean) {
        noteDao.setPinned(id, pinned)
    }

    suspend fun deleteNote(id: Long) {
        noteDao.deleteNoteById(id)
    }
}
